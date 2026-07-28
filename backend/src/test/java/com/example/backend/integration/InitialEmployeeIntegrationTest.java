package com.example.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.example.backend.security.JwtTokenProvider;

@SpringBootTest(properties = {
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.datasource.url=jdbc:h2:mem:initial_employee;MODE=PostgreSQL;NON_KEYWORDS=POSITION"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "/sql/initial-employee-test-data.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class InitialEmployeeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void initialEmployeeCanLogInWithEmployeeNumberAndPassword0001() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"employeeNo\":\"0001\",\"password\":\"0001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeNo").value("0001"));
    }

    @Test
    void employeeRegistrationAssigns0002AfterInitialEmployee() throws Exception {
        String token = jwtTokenProvider.createToken(1L, "0001");

        mockMvc.perform(post("/api/employees")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"employeeName":"New Employee","birthDate":"1990-01-02",
                         "postalCode":"1000001","address":"Tokyo","hireDate":"2026-07-01",
                         "departmentId":1,"skillGrade":1}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeNo").value("0002"));
    }
}
