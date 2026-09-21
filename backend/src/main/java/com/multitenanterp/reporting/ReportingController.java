package com.multitenanterp.reporting;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {
    private final ReportingService service;
    public ReportingController(ReportingService service) { this.service=service; }
    @GetMapping("/dashboard")
    public Map<String,Object> dashboard(@RequestParam LocalDate date) { return service.dashboard(date); }
    @GetMapping("/{name}")
    public ReportingService.Report report(@PathVariable String name, @RequestParam LocalDate from, @RequestParam LocalDate to) { return service.report(name,from,to); }
    @GetMapping(value="/{name}/csv", produces="text/csv;charset=UTF-8")
    public ResponseEntity<String> csv(@PathVariable String name, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(name+"-"+from+"-"+to+".csv").build().toString())
                .body(ReportingService.csv(service.report(name,from,to)));
    }
}
