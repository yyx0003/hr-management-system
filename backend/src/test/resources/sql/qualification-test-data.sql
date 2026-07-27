SET client_encoding = 'UTF8';

BEGIN;

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
    (3, DATE '2026-06-01', 'プロジェクトマネージャ', TRUE, 30000, DATE '2026-06-30');

COMMIT;