package com.example.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.backend.entity.Employee;
import com.example.backend.entity.EmployeeQualification;
import com.example.backend.repository.EmployeeQualificationRepository;
import com.example.backend.repository.EmployeeRepository;
import com.example.backend.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.datasource.url=jdbc:h2:mem:employee_registration;MODE=PostgreSQL;NON_KEYWORDS=POSITION"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/employee-registration-reset.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class EmployeeRegistrationIntegrationTest {

    private static final LocalDate HIRE_DATE = LocalDate.of(2026, 7, 1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeQualificationRepository employeeQualificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registersEmployeeAndQualificationsWithAuthenticatedRequest() throws Exception {
        MvcResult result = postEmployee(requestWithQualifications("1", "2"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeNo").value("0001"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        long employeeId = response.get("employeeId").asLong();
        Employee employee = employeeRepository
                .findEffectiveAndEmployedByEmployeeNoAt("0001", HIRE_DATE);

        assertThat(employee).isNotNull();
        assertThat(employee.getEmployeeId()).isEqualTo(employeeId);
        assertThat(employee.getStartDate()).isEqualTo(HIRE_DATE);
        assertThat(employee.getEndDate()).isNull();
        assertThat(passwordEncoder.matches("0001", employee.getPasswordHash())).isTrue();

        List<EmployeeQualification> qualifications =
                employeeQualificationRepository.findByEmployeeIdOrderByAcquisitionDate(employeeId);
        assertThat(qualifications)
                .extracting(EmployeeQualification::getQualificationId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void registersEmployeeWhenQualificationsAreOmittedNullOrEmpty() throws Exception {
        postEmployee(baseRequest() + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeNo").value("0001"));

        postEmployee(baseRequest() + ",\"qualifications\":null}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeNo").value("0002"));

        postEmployee(baseRequest() + ",\"qualifications\":[]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeNo").value("0003"));

        Employee firstEmployee = employeeRepository
                .findEffectiveAndEmployedByEmployeeNoAt("0001", HIRE_DATE);
        Employee secondEmployee = employeeRepository
                .findEffectiveAndEmployedByEmployeeNoAt("0002", HIRE_DATE);
        Employee thirdEmployee = employeeRepository
                .findEffectiveAndEmployedByEmployeeNoAt("0003", HIRE_DATE);
        assertThat(employeeQualificationRepository
                .findByEmployeeIdOrderByAcquisitionDate(firstEmployee.getEmployeeId())).isEmpty();
        assertThat(employeeQualificationRepository
                .findByEmployeeIdOrderByAcquisitionDate(secondEmployee.getEmployeeId())).isEmpty();
        assertThat(employeeQualificationRepository
                .findByEmployeeIdOrderByAcquisitionDate(thirdEmployee.getEmployeeId())).isEmpty();
    }

    @Test
    void rejectsNullQualificationArrayElementAsBadRequest() throws Exception {
        postEmployee(baseRequest() + ",\"qualifications\":[null]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(employeeRepository.findEffectiveAndEmployedByEmployeeNoAt("0001", HIRE_DATE)).isNull();
    }

    @Test
    void rejectsQualificationWithoutQualificationIdAsBadRequest() throws Exception {
        postEmployee(baseRequest()
                + ",\"qualifications\":[{\"acquisitionDate\":\"2020-01-01\"}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("選択した資格を入力してください。"));
    }

    @Test
    void rejectsInvalidDateAndNumberJsonAsBadRequest() throws Exception {
        postEmployee(baseRequest().replace("\"1990-01-02\"", "\"not-a-date\"") + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        postEmployee(baseRequest().replace("\"departmentId\":1", "\"departmentId\":\"not-a-number\"") + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsMalformedJsonAsBadRequestWithCommonErrorResponse() throws Exception {
        postEmployee("{\"employeeName\":\"Taro Yamada\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void rollsBackEmployeeAndLeavesEmployeeNumberSequenceAdvancedWhenQualificationInsertFails() throws Exception {
        // qualification_id=2 fails only after the employee and qualification_id=1 have been inserted.
        jdbcTemplate.execute("ALTER TABLE employee_qualification "
                + "ADD CONSTRAINT ck_employee_registration_forced_failure CHECK (qualification_id <> 2)");
        Integer qualificationCountBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee_qualification", Integer.class);

        postEmployee(requestWithQualifications("1", "2"))
                .andExpect(status().isInternalServerError());

        assertThat(employeeRepository.findEffectiveAndEmployedByEmployeeNoAt("0001", HIRE_DATE)).isNull();
        Integer qualificationCountAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM employee_qualification", Integer.class);
        assertThat(qualificationCountAfter).isEqualTo(qualificationCountBefore);

        postEmployee(baseRequest() + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeNo").value("0002"));
    }

    @Test
    void rejectsEmployeeNumberBeyond9999WithoutInsertingEmployee() throws Exception {
        jdbcTemplate().execute("ALTER SEQUENCE employee_no_seq RESTART WITH 10000");

        postEmployee(baseRequest() + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        assertThat(employeeRepository.findEffectiveAndEmployedByEmployeeNoAt("10000", HIRE_DATE)).isNull();
    }

    private org.springframework.test.web.servlet.ResultActions postEmployee(String request) throws Exception {
        String token = jwtTokenProvider.createToken(999L, "admin");
        return mockMvc.perform(post("/api/employees")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request));
    }

    private String baseRequest() {
        return "{\"employeeName\":\"Taro Yamada\",\"birthDate\":\"1990-01-02\","
                + "\"postalCode\":\"1000001\",\"address\":\"Tokyo\","
                + "\"hireDate\":\"2026-07-01\",\"departmentId\":1,\"skillGrade\":3";
    }

    private String requestWithQualifications(String... qualificationIds) {
        StringBuilder request = new StringBuilder(baseRequest()).append(",\"qualifications\":[");
        for (int index = 0; index < qualificationIds.length; index++) {
            if (index > 0) {
                request.append(',');
            }
            request.append("{\"qualificationId\":").append(qualificationIds[index])
                    .append(",\"acquisitionDate\":\"2020-01-01\"}");
        }
        return request.append("]}").toString();
    }

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate() {
        return jdbcTemplate;
    }
}
