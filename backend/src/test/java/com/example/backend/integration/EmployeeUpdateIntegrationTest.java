package com.example.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.example.backend.entity.Employee;
import com.example.backend.entity.EmployeeQualification;
import com.example.backend.common.MessageService;
import com.example.backend.repository.EmployeeQualificationRepository;
import com.example.backend.repository.EmployeeRepository;
import com.example.backend.security.JwtTokenProvider;

@SpringBootTest(properties = {
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.datasource.url=jdbc:h2:mem:employee_update;MODE=PostgreSQL;NON_KEYWORDS=POSITION"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeUpdateIntegrationTest {

    private static final Long EMPLOYEE_ID = 100L;
    private static final String EMPLOYEE_NO = "0100";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private EmployeeQualificationRepository employeeQualificationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MessageService messageService;

    private LocalDate today;
    private LocalDate currentStart;
    private LocalDate nextMonthStart;
    private String originalPasswordHash;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        currentStart = today.minusMonths(2).withDayOfMonth(1);
        nextMonthStart = today.withDayOfMonth(1).plusMonths(1);
        jdbcTemplate.execute("DELETE FROM salary_result");
        jdbcTemplate.execute("DELETE FROM attendance");
        jdbcTemplate.execute("DELETE FROM employee_qualification");
        jdbcTemplate.execute("DELETE FROM employee");
        jdbcTemplate.execute("DELETE FROM department");
        jdbcTemplate.execute("DELETE FROM \"position\"");
        jdbcTemplate.execute("DELETE FROM skill_grade");
        jdbcTemplate.execute("DELETE FROM qualification");
        jdbcTemplate.execute("ALTER TABLE employee_qualification "
                + "DROP CONSTRAINT IF EXISTS ck_employee_update_failure");

        LocalDate masterStart = today.minusMonths(3).withDayOfMonth(1);
        insertDepartment(1L, masterStart);
        insertDepartment(2L, nextMonthStart);
        insertDepartment(3L, nextMonthStart);
        insertPosition(1L, masterStart);
        insertPosition(2L, nextMonthStart);
        insertSkillGrade(3, masterStart);
        insertSkillGrade(4, nextMonthStart);
        insertQualification(1L, masterStart);
        insertQualification(2L, masterStart);
        insertQualification(3L, today);

        originalPasswordHash = passwordEncoder.encode("initial-password");
        jdbcTemplate.update("""
                INSERT INTO employee (employee_id, start_date, employee_no, password_hash, employee_name,
                    birth_date, postal_code, address, phone_number, email_address, hire_date, retire_date,
                    department_id, skill_grade, position_id, end_date)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, NULL)
                """, EMPLOYEE_ID, currentStart, EMPLOYEE_NO, originalPasswordHash, "Original Name",
                LocalDate.of(1990, 1, 2), "1000001", "Original Address", "09011112222",
                "original@example.com", LocalDate.of(2020, 4, 1), 1L, 3, 1L);
        jdbcTemplate.update("INSERT INTO employee_qualification "
                + "(employee_id, qualification_id, acquisition_date) VALUES (?, ?, ?)",
                EMPLOYEE_ID, 1L, LocalDate.of(2020, 1, 1));
    }

    @Test
    void requiresJwt() throws Exception {
        mockMvc.perform(put("/api/employees/{employeeNo}", EMPLOYEE_NO)
                        .contentType(MediaType.APPLICATION_JSON).content(request(1L, 1L, 3, "[qualification(1, \"2020-01-01\") ]")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatesPersonalInformationImmediatelyAndCreatesNextMonthOrganizationHistory() throws Exception {
        putEmployee(request(2L, 2L, 4, qualifications(1, "2020-01-01", 3, today.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(EMPLOYEE_ID))
                .andExpect(jsonPath("$.employeeNo").value(EMPLOYEE_NO));

        List<Employee> histories = employeeRepository.findByEmployeeId(EMPLOYEE_ID);
        assertThat(histories).hasSize(2);
        Employee oldHistory = historyAt(histories, currentStart);
        Employee nextHistory = historyAt(histories, nextMonthStart);
        assertThat(oldHistory.getEndDate()).isEqualTo(nextMonthStart.minusDays(1));
        assertThat(oldHistory.getEmployeeName()).isEqualTo("Updated Name");
        assertThat(nextHistory.getEmployeeName()).isEqualTo("Updated Name");
        assertThat(nextHistory.getDepartmentId()).isEqualTo(2L);
        assertThat(nextHistory.getPositionId()).isEqualTo(2L);
        assertThat(nextHistory.getSkillGrade()).isEqualTo(4);
        assertThat(nextHistory.getEmployeeId()).isEqualTo(EMPLOYEE_ID);
        assertThat(nextHistory.getEmployeeNo()).isEqualTo(EMPLOYEE_NO);
        assertThat(nextHistory.getPasswordHash()).isEqualTo(originalPasswordHash);
        assertThat(nextHistory.getHireDate()).isEqualTo(LocalDate.of(2020, 4, 1));
        assertQualifications(1L, 3L);
    }

    @Test
    void updatesExistingScheduledHistoryWithoutAddingAnotherAndCopiesPersonalInformationToBoth() throws Exception {
        putEmployee(request(2L, 2L, 4, qualifications(1, "2020-01-01"))).andExpect(status().isOk());
        putEmployee(request(1L, null, 3, qualifications(1, "2021-02-02"), "Second Update"))
                .andExpect(status().isOk());

        List<Employee> histories = employeeRepository.findByEmployeeId(EMPLOYEE_ID);
        assertThat(histories).hasSize(2);
        assertThat(historyAt(histories, currentStart).getEmployeeName()).isEqualTo("Second Update");
        Employee scheduled = historyAt(histories, nextMonthStart);
        assertThat(scheduled.getEmployeeName()).isEqualTo("Second Update");
        assertThat(scheduled.getDepartmentId()).isEqualTo(1L);
        assertThat(scheduled.getPositionId()).isNull();
        assertThat(scheduled.getSkillGrade()).isEqualTo(3);
        assertThat(employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(EMPLOYEE_ID))
                .extracting(EmployeeQualification::getAcquisitionDate)
                .containsExactly(LocalDate.of(2021, 2, 2));
    }

    @Test
    void updatesScheduledOrganizationFromBToCDuringTheSameMonthWithoutAddingHistory() throws Exception {
        putEmployee(request(2L, 2L, 4, qualifications(1, "2020-01-01"))).andExpect(status().isOk());
        putEmployee(request(3L, 2L, 4, qualifications(1, "2020-01-01"))).andExpect(status().isOk());

        List<Employee> histories = employeeRepository.findByEmployeeId(EMPLOYEE_ID);
        assertThat(histories).hasSize(2);
        assertThat(historyAt(histories, currentStart).getDepartmentId()).isEqualTo(1L);
        assertThat(historyAt(histories, nextMonthStart).getDepartmentId()).isEqualTo(3L);
    }

    @Test
    void doesNotCreateHistoryWhenOrganizationIsUnchangedAndSynchronizesEmptyQualifications() throws Exception {
        putEmployee(request(1L, 1L, 3, "null"))
                .andExpect(status().isOk());

        putEmployee(request(1L, 1L, 3, "[]"))
                .andExpect(status().isOk());

        assertThat(employeeRepository.findByEmployeeId(EMPLOYEE_ID)).hasSize(1);
        assertThat(employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(EMPLOYEE_ID)).isEmpty();
    }

    @Test
    void synchronizesRetireDateAcrossAllHistoriesAndAllowsCancellation() throws Exception {
        putEmployee(request(2L, 2L, 4, qualifications(1, "2020-01-01"))).andExpect(status().isOk());
        LocalDate retireDate = today.plusDays(10);
        putEmployee(request(2L, 2L, 4, qualifications(1, "2020-01-01"), "Updated Name", retireDate))
                .andExpect(status().isOk());
        assertThat(employeeRepository.findByEmployeeId(EMPLOYEE_ID))
                .extracting(Employee::getRetireDate).containsOnly(retireDate);

        putEmployee(request(2L, 2L, 4, qualifications(1, "2020-01-01"), "Updated Name", null))
                .andExpect(status().isOk());
        assertThat(employeeRepository.findByEmployeeId(EMPLOYEE_ID))
                .extracting(Employee::getRetireDate).containsOnlyNulls();
    }

    @Test
    void rejectsInvalidAndUnavailableEmployeesAndInvalidInputWithCommonErrorResponse() throws Exception {
        putEmployee("9999", request(1L, 1L, 3, qualifications(1, "2020-01-01")))
                .andExpect(status().isNotFound());
        putEmployee(request(1L, 1L, 3, qualifications(1, "2020-01-01")).replace("Updated Name", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
        jdbcTemplate.update("UPDATE employee SET retire_date = ? WHERE employee_id = ?", today.minusDays(1), EMPLOYEE_ID);
        putEmployee(request(1L, 1L, 3, qualifications(1, "2020-01-01")))
                .andExpect(status().isNotFound());
        putEmployee("invalid", request(1L, 1L, 3, qualifications(1, "2020-01-01")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsEndedEmployeeAtUpdateDate() throws Exception {
        jdbcTemplate.update("UPDATE employee SET end_date = ? WHERE employee_id = ?", today.minusDays(1), EMPLOYEE_ID);
        putEmployee(request(1L, 1L, 3, qualifications(1, "2020-01-01")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void validatesRetireDateBoundaryAndMasterValidity() throws Exception {
        putEmployee(request(1L, 1L, 3, qualifications(1, "2020-01-01"), "Updated Name",
                LocalDate.of(2020, 3, 31))).andExpect(status().isBadRequest());
        putEmployee(request(99L, 1L, 3, qualifications(1, "2020-01-01"))).andExpect(status().isBadRequest());
        putEmployee(request(1L, 99L, 3, qualifications(1, "2020-01-01"))).andExpect(status().isBadRequest());
        putEmployee(request(1L, 1L, 99, qualifications(1, "2020-01-01"))).andExpect(status().isBadRequest());
        putEmployee(request(1L, 1L, 3, qualifications(99, "2020-01-01"))).andExpect(status().isBadRequest());
        putEmployee(request(1L, 1L, 3, qualifications(1, "2020-01-01"), "Updated Name",
                LocalDate.of(2020, 4, 1))).andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidQualificationListsAndHandlesNullPosition() throws Exception {
        putEmployee(request(1L, null, 3, "[null]"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        putEmployee(request(1L, null, 3, qualifications(1, "2020-01-01", 1, "2021-01-01")))
                .andExpect(status().isBadRequest());
        putEmployee(request(1L, null, 3, "[]"))
                .andExpect(status().isOk());
        assertThat(employeeRepository.findByEmployeeId(EMPLOYEE_ID)).hasSize(2);
        assertThat(historyAt(employeeRepository.findByEmployeeId(EMPLOYEE_ID), nextMonthStart).getPositionId()).isNull();
    }

    @Test
    void validatesQualificationRequiredFieldsWithUpdateMessages() throws Exception {
        putEmployee(request(1L, 1L, 3, "[{\"acquisitionDate\":\"2020-01-01\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(messageService.getMessage("scr040.qualificationId.required")));
        putEmployee(request(1L, 1L, 3, "[{\"qualificationId\":1}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(messageService.getMessage("scr040.acquisitionDate.required")));
    }

    @Test
    void returnsCommonErrorResponseForMalformedJsonAndInvalidTypes() throws Exception {
        putEmployee("{").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(messageService.getMessage("request.body.invalid")));
        putEmployee(request(1L, 1L, 3, qualifications(1, "not-a-date")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(messageService.getMessage("request.body.invalid")));
        putEmployee(request(1L, 1L, 3, qualifications(1, "2020-01-01"))
                .replace("\"departmentId\":1", "\"departmentId\":\"invalid\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(messageService.getMessage("request.body.invalid")));
    }

    @Test
    void rollsBackHistoryAndQualificationsWhenQualificationSynchronizationFails() throws Exception {
        jdbcTemplate.update("INSERT INTO employee_qualification (employee_id, qualification_id, acquisition_date) VALUES (?, ?, ?)",
                EMPLOYEE_ID, 3L, LocalDate.of(2019, 1, 1));
        jdbcTemplate.execute("ALTER TABLE employee_qualification "
                + "ADD CONSTRAINT ck_employee_update_failure CHECK (qualification_id <> 2)");

        putEmployee(request(2L, 2L, 4, qualifications(1, "2021-01-01", 2, "2021-01-01"),
                "Rollback Name", today.plusDays(10)))
                .andExpect(status().isInternalServerError());

        assertThat(employeeRepository.findByEmployeeId(EMPLOYEE_ID)).hasSize(1);
        Employee current = historyAt(employeeRepository.findByEmployeeId(EMPLOYEE_ID), currentStart);
        assertThat(current.getEmployeeName()).isEqualTo("Original Name");
        assertThat(current.getEndDate()).isNull();
        assertThat(current.getRetireDate()).isNull();
        assertThat(employeeRepository.findByEmployeeId(EMPLOYEE_ID)
                .stream().noneMatch(history -> nextMonthStart.equals(history.getStartDate()))).isTrue();
        List<EmployeeQualification> qualifications = employeeQualificationRepository
                .findByEmployeeIdOrderByAcquisitionDate(EMPLOYEE_ID);
        assertThat(qualifications).extracting(EmployeeQualification::getQualificationId)
                .containsExactlyInAnyOrder(1L, 3L);
        assertThat(qualifications.stream().filter(item -> item.getQualificationId().equals(1L)).findFirst().orElseThrow()
                .getAcquisitionDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(qualifications.stream().noneMatch(item -> item.getQualificationId().equals(2L))).isTrue();
    }

    @Test
    void concurrentUpdatesCreateOnlyOneScheduledHistory() throws Exception {
        // H2のロック実装はPostgreSQLと完全には一致しない。PostgreSQLでは同一試験を別途実施する。
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        String requestA = concurrentRequest("並行更新A", "東京都A", 2L, 2L, 4,
                qualifications(1, "2020-01-01", 2, "2021-01-01"));
        String requestB = concurrentRequest("並行更新B", "東京都B", 3L, 1L, 3,
                qualifications(1, "2022-02-02", 3, "2023-03-03"));
        try {
            Future<Integer> first = executor.submit(() -> executeConcurrentUpdate(ready, start, requestA));
            Future<Integer> second = executor.submit(() -> executeConcurrentUpdate(ready, start, requestB));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo(200);
        } finally {
            executor.shutdownNow();
        }

        List<Employee> histories = employeeRepository.findByEmployeeId(EMPLOYEE_ID);
        assertThat(histories).hasSize(2);
        assertThat(histories.stream().filter(history -> nextMonthStart.equals(history.getStartDate()))).hasSize(1);
        assertThat(histories.stream().map(Employee::getStartDate).distinct()).hasSize(2);
        Employee current = historyAt(histories, currentStart);
        Employee scheduled = historyAt(histories, nextMonthStart);
        assertThat(current.getDepartmentId()).isEqualTo(1L);
        assertThat(scheduled.getDepartmentId()).isIn(2L, 3L);

        if (scheduled.getDepartmentId().equals(2L)) {
            assertConcurrentFinalState(current, scheduled, "並行更新A", "東京都A", 2L, 2L, 4,
                    1L, LocalDate.of(2020, 1, 1), 2L, LocalDate.of(2021, 1, 1));
        } else {
            assertConcurrentFinalState(current, scheduled, "並行更新B", "東京都B", 3L, 1L, 3,
                    1L, LocalDate.of(2022, 2, 2), 3L, LocalDate.of(2023, 3, 3));
        }
    }

    private void assertConcurrentFinalState(Employee current, Employee scheduled,
                                            String employeeName, String address,
                                            Long departmentId, Long positionId, Integer skillGrade,
                                            Long firstQualificationId, LocalDate firstAcquisitionDate,
                                            Long secondQualificationId, LocalDate secondAcquisitionDate) {
        assertThat(current.getEmployeeName()).isEqualTo(employeeName);
        assertThat(current.getAddress()).isEqualTo(address);
        assertThat(scheduled.getEmployeeName()).isEqualTo(employeeName);
        assertThat(scheduled.getAddress()).isEqualTo(address);
        assertThat(scheduled.getDepartmentId()).isEqualTo(departmentId);
        assertThat(scheduled.getPositionId()).isEqualTo(positionId);
        assertThat(scheduled.getSkillGrade()).isEqualTo(skillGrade);
        assertThat(employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(EMPLOYEE_ID))
                .extracting(EmployeeQualification::getQualificationId, EmployeeQualification::getAcquisitionDate)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(firstQualificationId, firstAcquisitionDate),
                        org.assertj.core.groups.Tuple.tuple(secondQualificationId, secondAcquisitionDate));
    }

    private int executeConcurrentUpdate(CountDownLatch ready, CountDownLatch start, String request) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent update start was not released");
        }
        return putEmployee(request).andReturn().getResponse().getStatus();
    }

    private org.springframework.test.web.servlet.ResultActions putEmployee(String request) throws Exception {
        return putEmployee(EMPLOYEE_NO, request);
    }

    private org.springframework.test.web.servlet.ResultActions putEmployee(String employeeNo, String request) throws Exception {
        return mockMvc.perform(put("/api/employees/{employeeNo}", employeeNo)
                .header("Authorization", "Bearer " + jwtTokenProvider.createToken(999L, "admin"))
                .contentType(MediaType.APPLICATION_JSON).content(request));
    }

    private String request(Long departmentId, Long positionId, Integer skillGrade, String qualifications) {
        return request(departmentId, positionId, skillGrade, qualifications, "Updated Name", null);
    }

    private String request(Long departmentId, Long positionId, Integer skillGrade, String qualifications, String name) {
        return request(departmentId, positionId, skillGrade, qualifications, name, null);
    }

    private String request(Long departmentId, Long positionId, Integer skillGrade, String qualifications,
                           String name, LocalDate retireDate) {
        return requestWithAddress(departmentId, positionId, skillGrade, qualifications, name,
                "Updated Address", retireDate);
    }

    private String concurrentRequest(String name, String address, Long departmentId, Long positionId,
                                     Integer skillGrade, String qualifications) {
        return requestWithAddress(departmentId, positionId, skillGrade, qualifications, name, address, null);
    }

    private String requestWithAddress(Long departmentId, Long positionId, Integer skillGrade, String qualifications,
                                      String name, String address, LocalDate retireDate) {
        return "{\"employeeName\":\"" + name + "\",\"birthDate\":\"1991-02-03\","
                + "\"postalCode\":\"1000002\",\"address\":\"" + address + "\","
                + "\"phoneNumber\":\"09099998888\",\"emailAddress\":\"updated@example.com\","
                + "\"departmentId\":" + departmentId + ",\"positionId\":"
                + (positionId == null ? "null" : positionId) + ",\"skillGrade\":" + skillGrade
                + ",\"retireDate\":" + (retireDate == null ? "null" : "\"" + retireDate + "\"")
                + ",\"qualifications\":" + qualifications + "}";
    }

    private String qualifications(Object... values) {
        StringBuilder result = new StringBuilder("[");
        for (int index = 0; index < values.length; index += 2) {
            if (index > 0) result.append(',');
            result.append("{\"qualificationId\":").append(values[index])
                    .append(",\"acquisitionDate\":\"").append(values[index + 1]).append("\"}");
        }
        return result.append(']').toString();
    }

    private Employee historyAt(List<Employee> histories, LocalDate startDate) {
        return histories.stream().filter(history -> startDate.equals(history.getStartDate())).findFirst().orElseThrow();
    }

    private void assertQualifications(Long... ids) {
        assertThat(employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(EMPLOYEE_ID))
                .extracting(EmployeeQualification::getQualificationId).containsExactlyInAnyOrder(ids);
    }

    private void insertDepartment(Long id, LocalDate startDate) {
        jdbcTemplate.update("INSERT INTO department (department_id, start_date, department_name, end_date) VALUES (?, ?, ?, NULL)", id, startDate, "Department " + id);
    }
    private void insertPosition(Long id, LocalDate startDate) {
        jdbcTemplate.update("INSERT INTO \"position\" (position_id, start_date, position_name, position_allowance, end_date) VALUES (?, ?, ?, 0, NULL)", id, startDate, "Position " + id);
    }
    private void insertSkillGrade(Integer grade, LocalDate startDate) {
        jdbcTemplate.update("INSERT INTO skill_grade (skill_grade, start_date, allowance, end_date) VALUES (?, ?, 0, NULL)", grade, startDate);
    }
    private void insertQualification(Long id, LocalDate startDate) {
        jdbcTemplate.update("INSERT INTO qualification (qualification_id, start_date, qualification_name, is_advance, qualification_allowance, end_date) VALUES (?, ?, ?, FALSE, 0, NULL)", id, startDate, "Qualification " + id);
    }
}
