package com.example.backend.repository;

import com.example.backend.entity.Department;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "jwt.secret=QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo0MTIzNDU2Nzg5MDEyMzQ=",
        "jwt.expirationMinutes=60"
})
@Sql("/sql/department-test-data.sql")
@ActiveProfiles("test")
@Transactional
class DepartmentRepositoryTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Test
    @DisplayName("レコードが1件だけの場合（開始日当日）")
    void findEffectiveAtTest1() {

        Department result =
                departmentRepository.findEffectiveAt(
                        1L,
                        LocalDate.of(2026, 4, 1));

        assertThat(result).isNotNull();
        assertThat(result.getDepartmentName())
                .isEqualTo("経営");
    }

    @Test
    @DisplayName("レコードが1件だけの場合（開始日翌日）")
    void findEffectiveAtTest2() {

        Department result =
                departmentRepository.findEffectiveAt(
                        1L,
                        LocalDate.of(2026, 4, 2));

        assertThat(result).isNotNull();
        assertThat(result.getDepartmentName())
                .isEqualTo("経営");
    }

    @Test
    @DisplayName("開始日前日は取得できない")
    void findEffectiveAtTest3() {

        Department result =
                departmentRepository.findEffectiveAt(
                        1L,
                        LocalDate.of(2026, 3, 31));

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("1つ目の終了日当日")
    void findEffectiveAtTest4() {

        Department result =
                departmentRepository.findEffectiveAt(
                        2L,
                        LocalDate.of(2026, 4, 30));

        assertThat(result).isNotNull();
        assertThat(result.getDepartmentName())
                .isEqualTo("営業");
    }

    @Test
    @DisplayName("2つ目の開始日当日")
    void findEffectiveAtTest5() {

        Department result =
                departmentRepository.findEffectiveAt(
                        2L,
                        LocalDate.of(2026, 5, 1));

        assertThat(result).isNotNull();
        assertThat(result.getDepartmentName())
                .isEqualTo("AI推進部");
    }

    @Test
    @DisplayName("2つ目の終了日当日")
    void findEffectiveAtTest6() {

        Department result =
                departmentRepository.findEffectiveAt(
                        2L,
                        LocalDate.of(2026, 5, 31));

        assertThat(result).isNotNull();
        assertThat(result.getDepartmentName())
                .isEqualTo("AI推進部");
    }

    @Test
    @DisplayName("3つ目の開始日当日")
    void findEffectiveAtTest7() {

        Department result =
                departmentRepository.findEffectiveAt(
                        2L,
                        LocalDate.of(2026, 6, 1));

        assertThat(result).isNotNull();
        assertThat(result.getDepartmentName())
                .isEqualTo("DX推進部");
    }

    @Test
    @DisplayName("終了日当日は取得できる")
    void findEffectiveAtTest8() {

        Department result =
                departmentRepository.findEffectiveAt(
                        3L,
                        LocalDate.of(2026, 6, 30));

        assertThat(result).isNotNull();
        assertThat(result.getDepartmentName())
                .isEqualTo("A社保守部");
    }

    @Test
    @DisplayName("終了日翌日は取得できない")
    void findEffectiveAtTest9() {

        Department result =
                departmentRepository.findEffectiveAt(
                        3L,
                        LocalDate.of(2026, 7, 1));

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("どのレコードよりも過去の日付")
    void findAllEffectiveAtTest1() {

        List<Department> result =
                departmentRepository.findAllEffectiveAt(
                        LocalDate.of(2026, 3, 31));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("変更日の前日")
    void findAllEffectiveAtTest2() {

        List<Department> result =
                departmentRepository.findAllEffectiveAt(
                        LocalDate.of(2026, 4, 30));

        assertThat(result)
                .extracting(Department::getDepartmentName)
                .containsExactlyInAnyOrder(
                        "経営",
                        "営業",
                        "開発１室",
                        "B社開発室");
    }

    @Test
    @DisplayName("変更日の当日")
    void findAllEffectiveAtTest3() {

        List<Department> result =
                departmentRepository.findAllEffectiveAt(
                        LocalDate.of(2026, 5, 1));

        assertThat(result)
                .extracting(Department::getDepartmentName)
                .containsExactlyInAnyOrder(
                        "経営",
                        "AI推進部",
                        "A社開発室",
                        "C社開発室");
    }

    @Test
    @DisplayName("最新履歴を取得できる（履歴1件）")
    void findLatestByDepartmentIdTest1() {

        Department result =
                departmentRepository.findLatestByDepartmentId(
                        1L);

        assertThat(result).isNotNull();
        assertThat(result.getDepartmentName())
                .isEqualTo("経営");
    }

    @Test
    @DisplayName("最新履歴を取得できる（履歴複数）")
    void findLatestByDepartmentIdTest2() {

        Department result =
                departmentRepository.findLatestByDepartmentId(
                        2L);

        assertThat(result).isNotNull();
        assertThat(result.getDepartmentName())
                .isEqualTo("DX推進部");
    }

    @Test
    @DisplayName("最新履歴を取得できる（全履歴終了済み）")
    void findLatestByDepartmentIdTest3() {

        Department result =
                departmentRepository.findLatestByDepartmentId(
                        3L);

        assertThat(result).isNotNull();
        assertThat(result.getDepartmentName())
                .isEqualTo("A社保守部");
    }

    @Test
    @DisplayName("存在しないIDの場合はnull")
    void findLatestByDepartmentIdTest4() {

        Department result =
                departmentRepository.findLatestByDepartmentId(
                        999L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("終了日更新（履歴1件）")
    void updateEndDateTest1() {

        Department department =
                departmentRepository.findLatestByDepartmentId(1L);

        department.setEndDate(
                LocalDate.of(2026, 12, 31));

        int result =
                departmentRepository.updateEndDate(department);

        assertThat(result).isEqualTo(1);

        Department updated =
                departmentRepository.findLatestByDepartmentId(1L);

        assertThat(updated.getEndDate())
                .isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("終了日更新（履歴複数）")
    void updateEndDateTest2() {

        Department department =
                departmentRepository.findLatestByDepartmentId(2L);

        department.setEndDate(
                LocalDate.of(2026, 12, 31));

        int result =
                departmentRepository.updateEndDate(department);

        assertThat(result).isEqualTo(1);

        Department updated =
                departmentRepository.findLatestByDepartmentId(2L);

        assertThat(updated.getEndDate())
                .isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("存在しないレコードの更新は0件")
    void updateEndDateTest3() {

        Department department = new Department();

        department.setDepartmentId(999L);
        department.setStartDate(
                LocalDate.of(2026, 1, 1));
        department.setEndDate(
                LocalDate.of(2026, 12, 31));

        int result =
                departmentRepository.updateEndDate(department);

        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("部署IDの最大値を取得できる")
    void findMaxIdTest1() {

        Long result = departmentRepository.findMaxId();

        assertThat(result).isEqualTo(4L);
    }

    @Test
    @DisplayName("部署履歴削除（履歴1件）")
    void deleteDepartmentTest1() {

        Department department =
                departmentRepository.findLatestByDepartmentId(1L);

        int result =
                departmentRepository.deleteDepartment(department);

        assertThat(result).isEqualTo(1);

        Department deleted =
                departmentRepository.findLatestByDepartmentId(1L);

        assertThat(deleted).isNull();
    }

    @Test
    @DisplayName("部署履歴削除（履歴複数の最新レコード）")
    void deleteDepartmentTest2() {

        Department department =
                departmentRepository.findLatestByDepartmentId(2L);

        int result =
                departmentRepository.deleteDepartment(department);

        assertThat(result).isEqualTo(1);

        Department latest =
                departmentRepository.findLatestByDepartmentId(2L);

        assertThat(latest.getDepartmentName())
                .isEqualTo("AI推進部");
    }

    @Test
    @DisplayName("部署履歴削除（全履歴終了済みの最新レコード）")
    void deleteDepartmentTest3() {

        Department department =
                departmentRepository.findLatestByDepartmentId(3L);

        int result =
                departmentRepository.deleteDepartment(department);

        assertThat(result).isEqualTo(1);

        Department latest =
                departmentRepository.findLatestByDepartmentId(3L);

        assertThat(latest.getDepartmentName())
                .isEqualTo("A社開発室");
    }

    @Test
    @DisplayName("存在しない部署の削除は0件")
    void deleteDepartmentTest4() {

        Department department = new Department();

        department.setDepartmentId(999L);
        department.setStartDate(
                LocalDate.of(2026, 1, 1));

        int result =
                departmentRepository.deleteDepartment(department);

        assertThat(result).isEqualTo(0);
    }
}