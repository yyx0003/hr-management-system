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

import com.example.backend.dto.qualification.CreateQualificationRequest;
import com.example.backend.dto.qualification.UpdateQualificationRequest;
import com.example.backend.entity.Qualification;
import com.example.backend.service.QualificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/qualification")
@RequiredArgsConstructor
public class QualificationController {

    private final QualificationService qualificationService;

    /** 全履歴取得 */
    @GetMapping
    public ResponseEntity<List<Qualification>> findAll() {
        return ResponseEntity.ok(qualificationService.findAll());
    }

    /** 対象日での有効な資格を取得 */
    @GetMapping("/{targetDate}")
    public ResponseEntity<List<Qualification>> findAllEffectiveAt(
        @PathVariable
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate targetDate) {
        return ResponseEntity.ok(qualificationService
                .findAllEffectiveAt(targetDate)
        );
    }

    /** 資格履歴更新 */
    @PutMapping
    public ResponseEntity<Qualification> updateQualification(
            @RequestBody UpdateQualificationRequest request) {

        return ResponseEntity.ok(
                qualificationService.updateQualification(
                        request.qualificationId(),
                        request.startDate(),
                        request.qualificationName(),
                        request.isAdvance(),
                        request.qualificationAllowance()));
    }

    /** 新規資格登録 */
    @PostMapping
    public ResponseEntity<Qualification> createQualification(
            @RequestBody CreateQualificationRequest request) {

        return ResponseEntity.ok(
                qualificationService.createQualification(
                        request.qualificationName(),
                        request.isAdvance(),
                        request.qualificationAllowance(),
                        request.startDate()));
    }

    /** 資格履歴削除 */
    @DeleteMapping("/{qualificationId}/{startDate}")
    public ResponseEntity<?> deleteQualification(
            @PathVariable Long qualificationId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {

        qualificationService.deleteQualification(
                qualificationId,
                startDate);
        return ResponseEntity.ok().build();
    }
}