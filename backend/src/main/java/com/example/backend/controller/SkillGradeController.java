package com.example.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.skillgrade.UpdateSkillGradeRequest;
import com.example.backend.entity.SkillGrade;
import com.example.backend.service.SkillGradeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/skillgrade")
@RequiredArgsConstructor
public class SkillGradeController {

    private final SkillGradeService skillGradeService;

    /** 全履歴取得 */
    @GetMapping
    public ResponseEntity<List<SkillGrade>> findAll() {

        return ResponseEntity.ok(
                skillGradeService.findAll());
    }

    @GetMapping("/{targetDate}")
    public ResponseEntity<List<SkillGrade>> findAllEffectiveAt(
        @PathVariable
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate targetDate) {
        return ResponseEntity.ok(
                skillGradeService.findAllEffectiveAt(targetDate)
        );
    }

    /** 職能資格履歴更新 */
    @PutMapping
    public ResponseEntity<SkillGrade> updateSkillGrade(
            @RequestBody UpdateSkillGradeRequest request) {

        return ResponseEntity.ok(
                skillGradeService.updateSkillGrade(
                        request.skillGrade(),
                        request.allowance(),
                        request.startDate()));
    }

    /** 職能資格履歴削除 */
    @DeleteMapping("/{skillGrade}/{startDate}")
    public ResponseEntity<?> deleteSkillGrade(
            @PathVariable Integer skillGrade,
            @PathVariable
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate) {

        skillGradeService.deleteSkillGrade(
                skillGrade,
                startDate);
        return ResponseEntity.ok().build();
    }
}