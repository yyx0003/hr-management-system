SET client_encoding = 'UTF8';

BEGIN;

DELETE FROM skill_grade;

INSERT INTO skill_grade (
    skill_grade,
    start_date,
    allowance,
    end_date
) VALUES
    (1, DATE '2026-04-01', 10000, NULL),

    (2, DATE '2026-04-01', 20000, DATE '2026-04-30'),
    (2, DATE '2026-05-01', 25000, DATE '2026-05-31'),
    (2, DATE '2026-06-01', 30000, NULL),

    (3, DATE '2026-04-01', 30000, DATE '2026-04-30'),
    (3, DATE '2026-05-01', 35000, DATE '2026-05-31'),
    (3, DATE '2026-06-01', 40000, DATE '2026-06-30'),

    (4, DATE '2026-04-01', 45000, NULL),
    (5, DATE '2026-04-01', 50000, NULL),
    (6, DATE '2026-04-01', 55000, NULL),
    (7, DATE '2026-04-01', 60000, NULL),
    (8, DATE '2026-04-01', 65000, NULL),
    (9, DATE '2026-04-01', 70000, NULL),
    (10, DATE '2026-04-01', 75000, NULL);

COMMIT;