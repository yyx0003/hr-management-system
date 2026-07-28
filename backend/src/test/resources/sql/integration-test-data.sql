SET client_encoding = 'UTF8';

-- =====================================================================
-- マスタデータ結合テスト用データ
-- 削除テストを行う際に、他のマスタデータが紐づいているかの検証のために使用
-- =====================================================================
BEGIN;

-- 部署
DELETE FROM department;
INSERT INTO department (
    department_id,
    start_date,
    department_name,
    end_date
) VALUES
    (1, DATE '2026-04-01', '経営', NULL),
    (2, DATE '2026-04-01', '営業', DATE '2026-04-30'),
    (2, DATE '2026-05-01', 'AI推進部', DATE '2026-05-31'),
    (2, DATE '2026-06-01', 'DX推進部', NULL),
    (3, DATE '2026-04-01', '開発１室', DATE '2026-04-30'),
    (3, DATE '2026-05-01', 'A社開発室', DATE '2026-05-31'),
    (3, DATE '2026-06-01', 'A社保守部', DATE '2026-06-30'),
    (4, DATE '2026-04-01', 'B社開発室', DATE '2026-4-30'),
    (4, DATE '2026-05-01', 'C社開発室', DATE '2026-10-31'),
    (4, DATE '2026-11-01', 'D社開発室', NULL),
    (5, DATE '2026-04-01', '削除失敗用', DATE '2026-10-31'),
    (5, DATE '2026-11-01', '削除失敗用', NULL);

-- 役職
DELETE FROM "position";
INSERT INTO "position" (
    position_id,
    start_date,
    position_name,
    position_allowance,
    end_date
) VALUES
    (1, DATE '2026-04-01', '主任', 10000, NULL),

    (2, DATE '2026-04-01', '係長', 20000, DATE '2026-04-30'),
    (2, DATE '2026-05-01', '係長', 25000, DATE '2026-05-31'),
    (2, DATE '2026-06-01', '係長', 30000, NULL),

    (3, DATE '2026-04-01', '課長', 40000, DATE '2026-04-30'),
    (3, DATE '2026-05-01', '課長', 50000, DATE '2026-05-31'),
    (3, DATE '2026-06-01', '課長', 60000, DATE '2026-06-30'),

    (4, DATE '2026-04-01', '部長', 50000, DATE '2026-4-30'),
    (4, DATE '2026-05-01', '部長', 60000, DATE '2026-10-31'),
    (4, DATE '2026-11-01', '部長', 70000, NULL),
    (5, DATE '2026-04-01', '削除失敗用', 10000, DATE '2026-10-31'),
    (5, DATE '2026-11-01', '削除失敗用', 10000, NULL);


-- 職能資格
DELETE FROM skill_grade;
INSERT INTO skill_grade (
    skill_grade,
    start_date,
    allowance,
    end_date
) VALUES
    (1, DATE '2026-04-01', 10000, NULL),

    (2, DATE '2026-04-01', 20000, DATE '2026-04-30'),
    (2, DATE '2026-05-01', 25000, DATE '2026-08-31'),
    (2, DATE '2026-09-01', 30000, NULL),

    (3, DATE '2026-04-01', 30000, DATE '2026-04-30'),
    (3, DATE '2026-05-01', 35000, DATE '2026-05-31'),
    (3, DATE '2026-06-01', 40000, DATE '2026-08-31'),
    (3, DATE '2026-09-01', 40000, NULL),

    (4, DATE '2026-04-01', 45000, DATE '2026-08-31'),
    (4, DATE '2026-09-01', 45000, DATE '2026-12-31'),

    (5, DATE '2026-04-01', 50000, NULL),
    (6, DATE '2026-04-01', 55000, NULL),
    (7, DATE '2026-04-01', 60000, NULL),
    (8, DATE '2026-04-01', 65000, NULL),
    (9, DATE '2026-04-01', 70000, NULL),
    (10, DATE '2026-04-01', 75000, NULL);

-- 資格
DELETE FROM qualification;

INSERT INTO qualification (
    qualification_id,
    start_date,
    qualification_name,
    is_advance,
    qualification_allowance,
    end_date
) VALUES
    (1, DATE '2026-04-01', '基本情報技術者', FALSE, 5000, NULL),

    (2, DATE '2026-04-01', '応用情報技術者', FALSE, 10000, DATE '2026-04-30'),
    (2, DATE '2026-05-01', '応用情報技術者', FALSE, 12000, DATE '2026-05-31'),
    (2, DATE '2026-06-01', '応用情報技術者', FALSE, 15000, NULL),

    (3, DATE '2026-04-01', 'プロジェクトマネージャ', TRUE, 20000, DATE '2026-04-30'),
    (3, DATE '2026-05-01', 'プロジェクトマネージャ', TRUE, 25000, DATE '2026-05-31'),
    (3, DATE '2026-06-01', 'プロジェクトマネージャ', TRUE, 30000, DATE '2026-06-30'),

    (4, DATE '2026-04-01', '資格4', TRUE, 50000, DATE '2026-4-30'),
    (4, DATE '2026-05-01', '資格4', TRUE, 60000, DATE '2026-10-31'),
    (4, DATE '2026-11-01', '資格4', TRUE, 70000, NULL),
    (5, DATE '2026-04-01', '削除失敗用', TRUE, 30000, DATE '2026-10-31'),
    (5, DATE '2026-11-01', '削除失敗用', TRUE, 30000, NULL);
;

-- 社員
DELETE FROM employee;
INSERT INTO employee (
    employee_id,
    start_date,
    employee_no,
    password_hash,
    employee_name,
    birth_date,
    postal_code,
    address,
    phone_number,
    email_address,
    hire_date,
    retire_date,
    department_id,
    skill_grade,
    position_id,
    end_date
) VALUES (
    999,
    DATE '2026-04-01',
    'E000999',
    'dummy-password',
    '削除検証用社員',
    DATE '2000-01-01',
    '1000001',
    '東京都千代田区',
    '09012345678',
    'test@example.com',
    DATE '2026-04-01',
    NULL,
    5,
    3,
    5,
    NULL
);

DELETE FROM salary_result;
INSERT INTO salary_result (
    employee_id,
    target_year,
    target_month,
    department_id,
    total_work_hours,
    total_overtime_hours,
    total_salary
) VALUES (
    999,
    2026,
    6,
    5,
    160.00,
    10.00,
    300000
);

DELETE FROM employee_qualification;
INSERT INTO employee_qualification (
    employee_id,
    qualification_id,
    acquisition_date
) VALUES (
    999,
    5,
    DATE '2026-04-01'
);