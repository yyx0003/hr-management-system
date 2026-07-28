SET client_encoding = 'UTF8';

BEGIN;

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
    (3, DATE '2026-06-01', '課長', 60000, DATE '2026-06-30');

COMMIT;