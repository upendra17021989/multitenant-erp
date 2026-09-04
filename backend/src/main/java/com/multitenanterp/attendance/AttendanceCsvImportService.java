package com.multitenanterp.attendance;

import com.multitenanterp.platform.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class AttendanceCsvImportService {
    static final List<String> REQUIRED_HEADERS = List.of("employeeNumber", "attendanceDate");
    private final JdbcClient db;
    private final AttendanceService attendanceService;

    public AttendanceCsvImportService(JdbcClient db, AttendanceService attendanceService) {
        this.db = db;
        this.attendanceService = attendanceService;
    }

    public AttendanceImportResult importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) throw badRequest("CSV file is required");
        String content;
        try { content = new String(file.getBytes(), StandardCharsets.UTF_8); }
        catch (IOException e) { throw badRequest("CSV file could not be read"); }

        List<List<String>> parsed = parse(content);
        if (parsed.isEmpty()) throw badRequest("CSV file has no header row");
        Map<String,Integer> headers = headers(parsed.getFirst());
        REQUIRED_HEADERS.forEach(name -> {
            if (!headers.containsKey(name)) throw badRequest("Missing required CSV column: " + name);
        });

        List<AttendanceImportRowResult> results = new ArrayList<>();
        for (int index=1; index<parsed.size(); index++) {
            List<String> row=parsed.get(index);
            if (row.stream().allMatch(String::isBlank)) continue;
            int rowNumber=index+1;
            try {
                UUID employeeId=employee(value(row,headers,"employeeNumber"));
                LocalDate date=LocalDate.parse(value(row,headers,"attendanceDate"));
                UUID shiftId=shift(value(row,headers,"shiftCode"),date);
                Instant checkIn=instant(value(row,headers,"checkIn"));
                Instant checkOut=instant(value(row,headers,"checkOut"));
                String status=optional(row,headers,"status");
                if(status==null) status=checkIn==null&&checkOut==null?"ABSENT":"PRESENT";
                AttendanceRecord saved=attendanceService.saveAttendance(null,new SaveAttendanceRequest(
                        employeeId,date,shiftId,status,checkIn,checkOut,null,0,"IMPORT",optional(row,headers,"notes")));
                results.add(new AttendanceImportRowResult(rowNumber,true,saved.id(),null));
            } catch (Exception e) {
                results.add(new AttendanceImportRowResult(rowNumber,false,null,message(e)));
            }
        }
        int accepted=(int)results.stream().filter(AttendanceImportRowResult::accepted).count();
        return new AttendanceImportResult(results.size(),accepted,results.size()-accepted,List.copyOf(results));
    }

    static List<List<String>> parse(String content) {
        List<List<String>> rows=new ArrayList<>();
        List<String> row=new ArrayList<>(); StringBuilder value=new StringBuilder(); boolean quoted=false;
        for(int i=0;i<content.length();i++){
            char c=content.charAt(i);
            if(c=='"'){
                if(quoted&&i+1<content.length()&&content.charAt(i+1)=='"'){value.append('"');i++;}
                else quoted=!quoted;
            } else if(c==','&&!quoted){row.add(value.toString().trim());value.setLength(0);}
            else if((c=='\n'||c=='\r')&&!quoted){
                if(c=='\r'&&i+1<content.length()&&content.charAt(i+1)=='\n')i++;
                row.add(value.toString().trim());value.setLength(0);rows.add(row);row=new ArrayList<>();
            } else value.append(c);
        }
        if(quoted)throw badRequest("CSV contains an unterminated quoted value");
        if(value.length()>0||!row.isEmpty()){row.add(value.toString().trim());rows.add(row);}
        return rows;
    }

    private UUID employee(String number){return db.sql("SELECT id FROM employment WHERE tenant_id=:tenant AND employee_number=:number")
            .param("tenant",tenant()).param("number",number.trim().toUpperCase(Locale.ROOT)).query(UUID.class).optional()
            .orElseThrow(()->badRequest("Employee not found: "+number));}
    private UUID shift(String code,LocalDate date){
        if(code==null||code.isBlank())return null;
        return db.sql("SELECT id FROM work_shift WHERE tenant_id=:tenant AND code=:code AND status='ACTIVE' AND effective_from<=:date AND (effective_to IS NULL OR effective_to>=:date) ORDER BY effective_from DESC LIMIT 1")
                .param("tenant",tenant()).param("code",code.trim().toUpperCase(Locale.ROOT)).param("date",date).query(UUID.class).optional()
                .orElseThrow(()->badRequest("Effective shift not found: "+code));
    }
    private static Map<String,Integer> headers(List<String> values){
        Map<String,Integer> result=new HashMap<>();
        for(int i=0;i<values.size();i++){String name=values.get(i).strip();if(i==0)name=name.replace("\uFEFF","");if(!name.isEmpty())result.put(name,i);}
        return result;
    }
    private static String value(List<String> row,Map<String,Integer> headers,String name){String value=optional(row,headers,name);if(value==null)throw badRequest("Missing value: "+name);return value;}
    private static String optional(List<String> row,Map<String,Integer> headers,String name){Integer i=headers.get(name);if(i==null||i>=row.size()||row.get(i).isBlank())return null;return row.get(i).trim();}
    private static Instant instant(String value){return value==null?null:Instant.parse(value);}
    private static UUID tenant(){return TenantContext.requireTenantId();}
    private static String message(Exception e){if(e instanceof ResponseStatusException r&&r.getReason()!=null)return r.getReason();return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
    private static ResponseStatusException badRequest(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
