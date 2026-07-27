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

import com.example.backend.dto.position.CreatePositionRequest;
import com.example.backend.dto.position.UpdatePositionRequest;
import com.example.backend.entity.Position;
import com.example.backend.service.PositionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/position")
@RequiredArgsConstructor
public class PositionController {

        private final PositionService positionService;

        /** 全履歴取得 */
        @GetMapping
        public ResponseEntity<List<Position>> findAll() {
                return ResponseEntity.ok(
                                positionService.findAll());
        }

        /** 役職履歴更新 */
        @PutMapping
        public ResponseEntity<Position> updatePosition(
                        @RequestBody UpdatePositionRequest request) {

                return ResponseEntity.ok(
                                positionService.updatePosition(
                                                request.positionId(),
                                                request.startDate(),
                                                request.positionName(),
                                                request.positionAllowance()));
        }

        /** 新規役職登録 */
        @PostMapping
        public ResponseEntity<Position> createPosition(
                        @RequestBody CreatePositionRequest request) {

                return ResponseEntity.ok(
                                positionService.createPosition(
                                                request.positionName(),
                                                request.positionAllowance(),
                                                request.startDate()));
        }

    /** 役職履歴削除 */
    @DeleteMapping("/{positionId}/{startDate}")
    public ResponseEntity<?> deletePosition(
            @PathVariable Long positionId,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate) {

        positionService.deletePosition(
                positionId,
                startDate);
        return ResponseEntity.ok().build();
    }
}