package com.example.backend.repository;

import com.example.backend.entity.Department;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(properties = {
                "jwt.secret=QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo0MTIzNDU2Nzg5MDEyMzQ=",
                "jwt.expirationMinutes=60"
})
@Sql("/sql/department-test-data.sql")
class DepartmentRepositoryTest {

        @Autowired
        private DepartmentRepository departmentRepository;

        @Test
        @DisplayName("指定日時点で有効な部署を取得できる（開始日から1か月経過）")
        void findEffectiveAtTest1() {

                LocalDate targetDate = LocalDate.of(2026, 5, 31);

                Department result = departmentRepository.findEffectiveAt(1L, targetDate);

                assertThat(result).isNotNull();
                assertThat(result.getDepartmentId()).isEqualTo(1L);
                assertThat(result.getDepartmentName()).isEqualTo("経営");
        }

        @Test
        @DisplayName("指定日時点で有効な部署を取得できる（開始日と同一）")
        void findEffectiveAtTest2() {

                LocalDate targetDate = LocalDate.of(2026, 4, 30);

                Department result = departmentRepository.findEffectiveAt(2L, targetDate);

                assertThat(result).isNotNull();
                assertThat(result.getDepartmentId()).isEqualTo(2L);
                assertThat(result.getDepartmentName()).isEqualTo("営業");
        }

        @Test
        @DisplayName("開始日の前日は取得できない")
        void findEffectiveAtTest3() {

                LocalDate targetDate = LocalDate.of(2026, 3, 31);

                Department result = departmentRepository.findEffectiveAt(3L, targetDate);

                assertThat(result).isNull();
        }

        @Test
        @DisplayName("履歴切替日の前日は切替前の情報を取得する")
        void findEffectiveAtTest4() {

                LocalDate targetDate = LocalDate.of(2026, 6, 30);

                Department result = departmentRepository.findEffectiveAt(5L, targetDate);

                assertThat(result).isNotNull();
                assertThat(result.getDepartmentId()).isEqualTo(5L);
                assertThat(result.getDepartmentName()).isEqualTo("開発2室");
        }

        @Test
        @DisplayName("履歴切替日の当日は切替後の情報を取得する")
        void findEffectiveAtTest5() {

                LocalDate targetDate = LocalDate.of(2026, 7, 1);

                Department result = departmentRepository.findEffectiveAt(5L, targetDate);

                assertThat(result).isNotNull();
                assertThat(result.getDepartmentId()).isEqualTo(5L);
                assertThat(result.getDepartmentName()).isEqualTo("保守・運用室");
        }

        @Test
        @DisplayName("部署情報が3つ存在する場合に、対象日時点での部署を取得できる（中間データ）")
        void findEffectiveAtTest6() {

                LocalDate targetDate = LocalDate.of(2026, 6, 30);

                Department result = departmentRepository.findEffectiveAt(6L, targetDate);

                assertThat(result).isNotNull();
                assertThat(result.getDepartmentId()).isEqualTo(6L);
                assertThat(result.getDepartmentName()).isEqualTo("A社開発室");
        }

        @Test
        @DisplayName("存在しない部署IDが指定された場合はnullを返す")
        void findEffectiveAtTest7() {

                LocalDate targetDate = LocalDate.of(2026, 6, 30);

                Department result = departmentRepository.findEffectiveAt(10L, targetDate);

                assertThat(result).isNull();
        }

        @Test
        @DisplayName("履歴が切り替わる前日は、切り替わる前のリストを取得できる")
        void findAllEffectiveAtTest1() {

                LocalDate targetDate = LocalDate.of(2026, 6, 30);
                List<Department> departments = departmentRepository.findAllEffectiveAt(targetDate);
                assertThat(departments).isNotNull();
                assertThat(departments)
                                .extracting(Department::getDepartmentName)
                                .containsExactlyInAnyOrder(
                                                "経営", "営業", "人事", "開発1室", "開発2室", "A社開発室");
        }

        @Test
        @DisplayName("履歴が切り替わる当日は、切り替わる後のリストを取得できる")
        void findAllEffectiveAtTest2() {

                LocalDate targetDate = LocalDate.of(2026, 7, 1);
                List<Department> departments = departmentRepository.findAllEffectiveAt(targetDate);
                assertThat(departments).isNotNull();
                assertThat(departments)
                                .extracting(Department::getDepartmentName)
                                .containsExactlyInAnyOrder(
                                                "経営", "営業", "人事", "開発1室", "保守・運用室", "B社開発室");
        }
}
