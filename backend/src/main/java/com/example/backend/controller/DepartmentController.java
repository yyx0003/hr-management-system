package com.example.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.department.CreateDepartmentRequest;
import com.example.backend.dto.department.UpdateDepartmentRequest;
import com.example.backend.entity.Department;
import com.example.backend.service.DepartmentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /** 全履歴取得 */
    @GetMapping
    public ResponseEntity<List<Department>> findAll() {
        return ResponseEntity.ok(departmentService.findAll());
    }

    /** 部署履歴更新 */
    @PutMapping
    public ResponseEntity<Department> updateDepartment(
            @RequestBody UpdateDepartmentRequest request) {

        return ResponseEntity.ok(departmentService.updateDepartment(
                request.departmentId(),
                request.startDate(),
                request.departmentName()));
    }

    /** 新規部署登録 */
    @PostMapping
    public ResponseEntity<Department> createDepartment(
            @RequestBody CreateDepartmentRequest request) {

        return ResponseEntity.ok(departmentService.createDepartment(
                request.departmentName(),
                request.startDate()));
    }

    /** 部署履歴削除 */
    @DeleteMapping("/{departmentId}/{startDate}")
    public ResponseEntity<?> deleteDepartment(
            @PathVariable Long departmentId,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate) {

        departmentService.deleteDepartment(
                departmentId,
                startDate);
        return ResponseEntity.ok().build();
    }
}