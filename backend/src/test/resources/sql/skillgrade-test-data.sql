SET client_encoding = 'UTF8';

BEGIN;

DELETE FROM skill_grade;

INSERT INTO skill_grade (
    skill_grade,
    start_date,
    allowance,
    end_date
) VALUES
    (1, DATE '2026-04-01', 200000, NULL),

    (2, DATE '2026-04-01', 220000, DATE '2026-06-30'),
    (2, DATE '2026-07-01', 230000, NULL),

    (3, DATE '2026-04-01', 240000, NULL),

    (4, DATE '2026-04-01', 260000, DATE '2026-05-31'),
    (4, DATE '2026-06-01', 270000, DATE '2026-06-30'),
    (4, DATE '2026-07-01', 280000, NULL),

    (5, DATE '2026-04-01', 300000, NULL),
    (6, DATE '2026-04-01', 320000, NULL),
    (7, DATE '2026-04-01', 340000, NULL),
    (8, DATE '2026-04-01', 360000, NULL),
    (9, DATE '2026-04-01', 380000, NULL),
    (10, DATE '2026-04-01', 400000, NULL);

COMMIT;