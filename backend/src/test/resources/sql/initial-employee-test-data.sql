DELETE FROM salary_result;
DELETE FROM attendance;
DELETE FROM employee_qualification;
DELETE FROM employee;
DELETE FROM department;
DELETE FROM "position";
DELETE FROM skill_grade;
DELETE FROM qualification;

ALTER SEQUENCE employee_no_seq RESTART WITH 1;

INSERT INTO department (department_id, start_date, department_name, end_date)
VALUES (1, DATE '2026-04-01', 'Initial Department', NULL);

INSERT INTO skill_grade (skill_grade, start_date, allowance, end_date)
VALUES (1, DATE '2026-04-01', 200000, NULL);

INSERT INTO employee (
    start_date, employee_no, password_hash, employee_name, birth_date,
    postal_code, address, phone_number, email_address, hire_date,
    retire_date, department_id, skill_grade, position_id, end_date
) VALUES (
    DATE '2026-04-01', '0001',
    '$2a$10$DCVd3OAMoDq40RpGNRnYCO6TuhelMMaU.ovgc0Y44MyTBQJ./Lth2',
    'Initial Login Employee', DATE '2000-01-01',
    '1000001', 'Tokyo', NULL, NULL, DATE '2026-04-01',
    NULL, 1, 1, NULL, NULL
);

ALTER SEQUENCE employee_no_seq RESTART WITH 2;
