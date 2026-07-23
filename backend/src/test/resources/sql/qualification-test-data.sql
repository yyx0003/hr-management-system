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
    (1, DATE '2026-04-01', '基本情報技術者試験', FALSE, 3000, NULL),

    (2, DATE '2026-04-01', '応用情報技術者', FALSE, 5000, DATE '2026-06-30'),
    (2, DATE '2026-07-01', '応用情報技術者', FALSE, 6000, NULL),

    (3, DATE '2026-04-01', 'システムアーキテクト', TRUE, 30000, NULL),

    (4, DATE '2026-04-01', 'プロジェクトマネージャ', TRUE, 30000, DATE '2026-05-31'),
    (4, DATE '2026-06-01', 'プロジェクトマネージャ', TRUE, 35000, DATE '2026-06-30'),
    (4, DATE '2026-07-01', 'プロジェクトマネージャ', TRUE, 40000, NULL),

    (5, DATE '2026-04-01', 'ネットワークスペシャリスト', TRUE, 30000, DATE '2026-04-30'),
    (5, DATE '2026-05-01', 'ネットワークスペシャリスト', TRUE, 35000, NULL),

    (6, DATE '2026-04-01', 'データベーススペシャリスト', TRUE, 30000, NULL),

    (7, DATE '2026-04-01', 'エンベデッドシステムスペシャリスト', TRUE, 30000, NULL);

COMMIT;