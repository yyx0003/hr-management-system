-- =====================================================================
-- =====================================================================
--
-- =====================================================================

-- ---------------------------------------------------------------------
-- ---------------------------------------------------------------------
CREATE TABLE employee (
    employee_id      BIGSERIAL       NOT NULL,
    start_date        DATE            NOT NULL,
    employee_no        VARCHAR(20)     NOT NULL,
    password_hash        VARCHAR(255)    NOT NULL,
    employee_name          VARCHAR(100)    NOT NULL,
    birth_date                DATE            NOT NULL,
    postal_code                  VARCHAR(8)      NOT NULL,
    address                        VARCHAR(255)    NOT NULL,
    phone_number                      VARCHAR(20),
    email_address                        VARCHAR(255),
    hire_date                              DATE            NOT NULL,
    retire_date                              DATE,
    department_id                              BIGINT          NOT NULL,
    skill_grade                                  BIGINT          NOT NULL,
    position_id                                    BIGINT,
    end_date                                          DATE,
    created_at                                          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at                                          TIMESTAMP       NOT NULL DEFAULT now(),
    CONSTRAINT pk_employee PRIMARY KEY (employee_id, start_date),
    CONSTRAINT ck_employee_date_range CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_employee_employment_date_range CHECK (retire_date IS NULL OR retire_date >= hire_date),
    CONSTRAINT ck_employee_skill_grade CHECK (skill_grade BETWEEN 1 AND 10)
);
CREATE UNIQUE INDEX uq_employee_current ON employee (employee_id) WHERE end_date IS NULL;
CREATE UNIQUE INDEX uq_employee_no_current ON employee (employee_no) WHERE end_date IS NULL;

-- ---------------------------------------------------------------------
-- ---------------------------------------------------------------------
CREATE TABLE department (
    department_id      BIGINT          NOT NULL,
    start_date           DATE            NOT NULL,
    department_name        VARCHAR(255)    NOT NULL,
    end_date                  DATE,
    created_at                  TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMP       NOT NULL DEFAULT now(),
    CONSTRAINT pk_department PRIMARY KEY (department_id, start_date),
    CONSTRAINT ck_department_date_range CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_department_start_date_month_begin CHECK (EXTRACT(DAY FROM start_date) = 1)
);
CREATE UNIQUE INDEX uq_department_current ON department (department_id) WHERE end_date IS NULL;

-- ---------------------------------------------------------------------
-- ---------------------------------------------------------------------
CREATE TABLE qualification (
    qualification_id        BIGINT          NOT NULL,
    start_date                 DATE            NOT NULL,
    qualification_name           VARCHAR(255)    NOT NULL,
    is_advance                      BOOLEAN         NOT NULL,
    qualification_allowance            BIGINT          NOT NULL,
    end_date                              DATE,
    created_at                              TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at                              TIMESTAMP       NOT NULL DEFAULT now(),
    CONSTRAINT pk_qualification PRIMARY KEY (qualification_id, start_date),
    CONSTRAINT ck_qualification_date_range CHECK (end_date IS NULL OR end_date >= start_date)
);
CREATE UNIQUE INDEX uq_qualification_current ON qualification (qualification_id) WHERE end_date IS NULL;

-- ---------------------------------------------------------------------
-- ---------------------------------------------------------------------
CREATE TABLE "position" (
    position_id          BIGINT          NOT NULL,
    start_date              DATE            NOT NULL,
    position_name              VARCHAR(255)    NOT NULL,
    position_allowance            BIGINT          NOT NULL,
    end_date                        DATE,
    created_at                        TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at                        TIMESTAMP       NOT NULL DEFAULT now(),
    CONSTRAINT pk_position PRIMARY KEY (position_id, start_date),
    CONSTRAINT ck_position_date_range CHECK (end_date IS NULL OR end_date >= start_date)
);
CREATE UNIQUE INDEX uq_position_current ON "position" (position_id) WHERE end_date IS NULL;

-- ---------------------------------------------------------------------
-- ---------------------------------------------------------------------
CREATE TABLE skill_grade (
    skill_grade    BIGINT      NOT NULL,
    start_date        DATE        NOT NULL,
    allowance             BIGINT      NOT NULL,
    end_date                 DATE,
    created_at                  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at                     TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT pk_skill_grade PRIMARY KEY (skill_grade, start_date),
    CONSTRAINT ck_skill_grade_date_range CHECK (end_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_skill_grade_value CHECK (skill_grade BETWEEN 1 AND 10)
);
CREATE UNIQUE INDEX uq_skill_grade_current ON skill_grade (skill_grade) WHERE end_date IS NULL;

-- ---------------------------------------------------------------------
-- ---------------------------------------------------------------------
CREATE TABLE employee_qualification (
    employee_id         BIGINT      NOT NULL,
    qualification_id      BIGINT      NOT NULL,
    acquisition_date         DATE        NOT NULL,
    created_at                 TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT pk_employee_qualification PRIMARY KEY (employee_id, qualification_id)
);

-- ---------------------------------------------------------------------
-- ---------------------------------------------------------------------
CREATE TABLE holiday (
    holiday_date    DATE            NOT NULL,
    holiday_type      VARCHAR(20)     NOT NULL,
    holiday_name        VARCHAR(20)     NOT NULL,
    created_at             TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP       NOT NULL DEFAULT now(),
    CONSTRAINT pk_holiday PRIMARY KEY (holiday_date)
);

-- ---------------------------------------------------------------------
-- ---------------------------------------------------------------------
CREATE TABLE attendance (
    employee_id       BIGINT      NOT NULL,
    work_date            DATE        NOT NULL,
    attendance_time         TIME,
    leaving_time               TIME,
    work_type                     VARCHAR(20) NOT NULL,
    created_at                       TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at                          TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT pk_attendance PRIMARY KEY (employee_id, work_date),
    CONSTRAINT ck_attendance_work_type CHECK (work_type IN ('NORMAL', 'PAID_LEAVE', 'ABSENCE', 'HOLIDAY_WORK')),
    CONSTRAINT ck_attendance_time_by_type CHECK (
        (
            work_type IN ('NORMAL', 'HOLIDAY_WORK')
            AND attendance_time IS NOT NULL AND leaving_time IS NOT NULL
            AND leaving_time > attendance_time
        )
        OR (
            work_type IN ('PAID_LEAVE', 'ABSENCE')
            AND attendance_time IS NULL AND leaving_time IS NULL
        )
    )
);

-- ---------------------------------------------------------------------
-- ---------------------------------------------------------------------
CREATE TABLE salary_result (
    employee_id             BIGINT      NOT NULL,
    target_year               BIGINT      NOT NULL,
    target_month                 BIGINT      NOT NULL,
    department_id                   BIGINT      NOT NULL,
    total_work_hours                   NUMERIC(6,2) NOT NULL,
    total_overtime_hours                  NUMERIC(6,2) NOT NULL DEFAULT 0,
    total_salary                             BIGINT      NOT NULL,
    created_at                                  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at                                     TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT pk_salary_result PRIMARY KEY (employee_id, target_year, target_month),
    CONSTRAINT ck_salary_result_target_month CHECK (target_month BETWEEN 1 AND 12),
    CONSTRAINT ck_salary_result_amounts CHECK (
        total_work_hours >= 0 AND total_overtime_hours >= 0 AND total_salary >= 0
    )
);

-- ---------------------------------------------------------------------
-- ---------------------------------------------------------------------
CREATE INDEX idx_employee_retire_date ON employee (retire_date);
CREATE INDEX idx_attendance_work_date ON attendance (work_date);
CREATE INDEX idx_salary_result_target ON salary_result (target_year, target_month);
