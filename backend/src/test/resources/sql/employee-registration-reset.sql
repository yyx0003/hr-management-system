DELETE FROM salary_result;
DELETE FROM attendance;
DELETE FROM employee_qualification;
DELETE FROM employee;
DELETE FROM department;
DELETE FROM "position";
DELETE FROM skill_grade;
DELETE FROM qualification;

ALTER TABLE employee_qualification
    DROP CONSTRAINT IF EXISTS ck_employee_registration_forced_failure;
ALTER SEQUENCE employee_no_seq RESTART WITH 1;

INSERT INTO department (department_id, start_date, department_name, end_date)
VALUES (1, DATE '2020-01-01', 'Development', NULL);

INSERT INTO "position" (position_id, start_date, position_name, position_allowance, end_date)
VALUES (1, DATE '2020-01-01', 'Engineer', 0, NULL);

INSERT INTO skill_grade (skill_grade, start_date, allowance, end_date)
VALUES (3, DATE '2020-01-01', 0, NULL);

INSERT INTO qualification (
    qualification_id, start_date, qualification_name, is_advance, qualification_allowance, end_date)
VALUES
    (1, DATE '2020-01-01', 'Qualification One', FALSE, 0, NULL),
    (2, DATE '2020-01-01', 'Qualification Two', FALSE, 0, NULL);
