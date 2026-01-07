package com.mentis.hrms.controller;

import com.mentis.hrms.model.Employee;
import com.mentis.hrms.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    @Autowired private EmployeeService employeeService;

    /* Employee sets own presence (ACTIVE / BREAK / OFFLINE) */
    @PostMapping("/set/{employeeId}")
    public ResponseEntity<Map<String,Object>> setPresence(
            @PathVariable String employeeId,
            @RequestParam String status){

        Map<String,Object> resp=new HashMap<>();
        try{
            Employee emp=employeeService.getEmployeeByEmployeeId(employeeId).orElse(null);
            if(emp==null) return ResponseEntity.notFound().build();

            emp.setPresenceStatus(status);
            emp.setLastPresenceUpdate(LocalDateTime.now());
            employeeService.updateEmployee(emp);

            resp.put("success",true);
            resp.put("status",status);
            resp.put("employeeId",employeeId);
            return ResponseEntity.ok(resp);
        }catch(Exception e){
            resp.put("success",false);
            resp.put("error",e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    /* HR / others fetch full presence list */
    @GetMapping("/list")
    public ResponseEntity<Map<String,Object>> getAllPresence(){
        List<Map<String,Object>> list = employeeService.getAllEmployees()
                .stream()
                .map(e -> {
                    Map<String,Object> m = new HashMap<>();
                    m.put("employeeId", e.getEmployeeId());
                    m.put("presenceStatus", e.getPresenceStatus());
                    m.put("lastUpdate", e.getLastPresenceUpdate());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(Map.of("presence", list));
    }
}