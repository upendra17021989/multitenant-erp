package com.multitenanterp.employee;

import com.multitenanterp.platform.tenant.TenantContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class EmployeeExcelImportService {
    private static final Set<String> EMPLOYMENT_TYPES=Set.of("PERMANENT","PROBATION","CONTRACT","CONSULTANT","INTERN");
    private static final List<DateTimeFormatter> DATE_FORMATS=List.of(
            DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("d-MMM-uuuu",Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d/MM/uuuu",Locale.ENGLISH), DateTimeFormatter.ofPattern("d/M/uuuu",Locale.ENGLISH));
    private final JdbcClient db;
    private final EmployeeService employees;
    private final DataFormatter formatter=new DataFormatter(Locale.ENGLISH);

    public EmployeeExcelImportService(JdbcClient db,EmployeeService employees){this.db=db;this.employees=employees;}

    public EmployeeImportResult importExcel(MultipartFile file,String sheetName,String employmentType){
        if(file==null||file.isEmpty())throw bad("Excel file is required");
        String type=clean(employmentType)==null?"PERMANENT":employmentType.trim().toUpperCase(Locale.ROOT);
        if(!EMPLOYMENT_TYPES.contains(type))throw bad("Invalid employment type");
        if(clean(sheetName)==null)throw bad("Worksheet name is required");
        try(Workbook workbook=new XSSFWorkbook(file.getInputStream())){
            Sheet sheet=workbook.getSheet(sheetName.trim());
            if(sheet==null)throw bad("Worksheet not found: "+sheetName);
            Row header=sheet.getRow(sheet.getFirstRowNum());
            if(header==null)throw bad("Worksheet has no header row");
            Map<String,Integer> headers=headers(header);
            require(headers,"employeecode","firstname","joindate");
            List<EmployeeImportRowResult> results=new ArrayList<>();
            Set<String> fileNumbers=new HashSet<>();
            for(int index=header.getRowNum()+1;index<=sheet.getLastRowNum();index++){
                Row row=sheet.getRow(index);
                if(row==null||blank(row))continue;
                int rowNumber=index+1;String employeeNumber=text(row,headers,"employeecode");
                try{
                    if(employeeNumber==null)throw bad("Missing value: Employee Code");
                    employeeNumber=employeeNumber.toUpperCase(Locale.ROOT);
                    if(!fileNumbers.add(employeeNumber))throw bad("Duplicate employee code in workbook: "+employeeNumber);
                    if(exists(employeeNumber))throw bad("Employee code already exists: "+employeeNumber);
                    Name name=name(text(row,headers,"firstname"),text(row,headers,"middlename"),text(row,headers,"lastname"));
                    Employee saved=employees.create(request(row,headers,type,employeeNumber,name));
                    results.add(new EmployeeImportRowResult(rowNumber,true,saved.id(),employeeNumber,null));
                }catch(Exception exception){results.add(new EmployeeImportRowResult(rowNumber,false,null,employeeNumber,message(exception)));}
            }
            int accepted=(int)results.stream().filter(EmployeeImportRowResult::accepted).count();
            return new EmployeeImportResult(results.size(),accepted,results.size()-accepted,List.copyOf(results));
        }catch(IOException exception){throw bad("Excel file could not be read");}
    }

    private SaveEmployeeRequest request(Row row,Map<String,Integer> h,String type,String number,Name name){
        String status=status(text(row,h,"status"));
        LocalDate joining=date(row,h,"joindate",true);
        return new SaveEmployeeRequest(number,status,type,name.first,name.middle,name.last,date(row,h,"birthdate",false),text(row,h,"gender"),
                null,text(row,h,"mobile"),null,null,text(row,h,"fathername"),null,joining,date(row,h,"confirmdate",false),null,null,null,
                text(row,h,"email"),master("branch",text(row,h,"branch")),master("department",text(row,h,"department")),
                master("designation",text(row,h,"designation")),master("grade",text(row,h,"grade")),null,null,
                payment(text(row,h,"paymentmode")),name.full,text(row,h,"accountno","accountnumber"),text(row,h,"bankname"),null,
                upper(row,h,"ifsccode","ifsc"),upper(row,h,"pan"),lastFour(text(row,h,"aadharnumber")),digits(row,h,"uanno"),
                text(row,h,"pfno"),text(row,h,"esicoldno"),text(row,h,"title"),text(row,h,"maritalstatus"),text(row,h,"fathername"),
                text(row,h,"ticketnumber"),date(row,h,"fnfretiringdate",false),date(row,h,"pfjoindate",false),digits(row,h,"pran"),
                date(row,h,"groupjoindate","joiningdate"),text(row,h,"ccemail"),text(row,h,"division"),text(row,h,"unit"),
                text(row,h,"category"),text(row,h,"project"));
    }

    private Map<String,Integer> headers(Row row){Map<String,Integer> result=new HashMap<>();for(Cell cell:row)result.put(key(formatter.formatCellValue(cell)),cell.getColumnIndex());return result;}
    private static String key(String value){return value==null?"":value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    private static void require(Map<String,Integer> h,String... names){for(String name:names)if(!h.containsKey(name))throw bad("Missing required Excel column: "+name);}
    private boolean blank(Row row){for(Cell cell:row)if(clean(formatter.formatCellValue(cell))!=null)return false;return true;}
    private String text(Row row,Map<String,Integer> h,String... names){for(String name:names){Integer column=h.get(name);if(column!=null){String value=clean(formatter.formatCellValue(row.getCell(column,Row.MissingCellPolicy.RETURN_BLANK_AS_NULL)));if(value!=null)return value;}}return null;}
    private String upper(Row row,Map<String,Integer> h,String... names){String value=text(row,h,names);return value==null?null:value.toUpperCase(Locale.ROOT);}
    private String digits(Row row,Map<String,Integer> h,String... names){String value=text(row,h,names);if(value==null||value.equals("0"))return null;return value.replaceAll("\\D","");}
    private LocalDate date(Row row,Map<String,Integer> h,String name,boolean required){LocalDate value=date(row,h,name);if(required&&value==null)throw bad("Missing value: Join Date");return value;}
    private LocalDate date(Row row,Map<String,Integer> h,String... names){for(String name:names){Integer column=h.get(name);if(column==null)continue;Cell cell=row.getCell(column,Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);if(cell==null)continue;if(cell.getCellType()==CellType.NUMERIC&&DateUtil.isCellDateFormatted(cell))return cell.getLocalDateTimeCellValue().toLocalDate();String value=clean(formatter.formatCellValue(cell));if(value==null||value.equals("0"))continue;for(DateTimeFormatter format:DATE_FORMATS)try{return LocalDate.parse(value,format);}catch(DateTimeParseException ignored){}}return null;}
    private UUID master(String table,String value){if(value==null)return null;return db.sql("SELECT id FROM "+table+" WHERE tenant_id=:tenant AND UPPER(name)=UPPER(:name)").param("tenant",TenantContext.requireTenantId()).param("name",value).query(UUID.class).optional().orElseThrow(()->bad("Unknown "+table+": "+value));}
    private boolean exists(String number){return db.sql("SELECT COUNT(*) FROM employment WHERE tenant_id=:tenant AND employee_number=:number").param("tenant",TenantContext.requireTenantId()).param("number",number).query(Integer.class).single()>0;}
    private static String status(String value){if(value==null)return "ACTIVE";return switch(value.trim().toUpperCase(Locale.ROOT)){case "ACTIVE"->"ACTIVE";case "LEFT","EXITED"->"EXITED";case "NOTICE"->"NOTICE";case "PROBATION"->"PROBATION";case "INACTIVE"->"INACTIVE";default->throw bad("Unknown status: "+value);};}
    private static String payment(String value){if(value==null)return null;return switch(value.trim().toUpperCase(Locale.ROOT)){case "BANK","BANK TRANSFER","BANK_TRANSFER"->"BANK_TRANSFER";case "CASH"->"CASH";case "CHEQUE","CHECK"->"CHEQUE";default->throw bad("Unknown payment mode: "+value);};}
    private static String lastFour(String value){if(value==null)return null;String digits=value.replaceAll("\\D","");return digits.length()<4?null:digits.substring(digits.length()-4);}
    private static Name name(String full,String middle,String last){if(full==null)throw bad("Missing value: First Name");if(last!=null)return new Name(full,middle,last,String.join(" ",full,middle==null?"":middle,last).replaceAll("\\s+"," ").trim());String[] parts=full.trim().split("\\s+");if(parts.length<2)throw bad("Last Name is missing and cannot be derived from a single-word name");String first=parts[0],derivedLast=parts[parts.length-1],derivedMiddle=parts.length>2?String.join(" ",Arrays.copyOfRange(parts,1,parts.length-1)):middle;return new Name(first,derivedMiddle,derivedLast,full.trim());}
    private static String clean(String value){return value==null||value.isBlank()?null:value.trim();}
    private static String message(Exception e){if(e instanceof ResponseStatusException r&&r.getReason()!=null)return r.getReason();return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
    private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
    private record Name(String first,String middle,String last,String full){}
}
