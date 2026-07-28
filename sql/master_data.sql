SET client_encoding = 'UTF8';

-- =====================================================================
-- 基本マスタデータ
-- 対象テーブル:
--   department / position / skill_grade / qualification
--
-- 前提:
--   schema.sqlを先に実行すること。
--   初期適用開始日は2026-04-01とする。
--
-- 【重要】Windows環境のpsqlはデフォルトでSJIS（日本語）として読み込もうとし、
-- 文字化け・エラーの原因になるため、冒頭で明示的にUTF-8を指定している。
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 部署
-- ---------------------------------------------------------------------
INSERT INTO department (
    department_id,
    start_date,
    department_name,
    end_date
) VALUES
    (1, DATE '2026-04-01', '経営',    NULL),
    (2, DATE '2026-04-01', '営業',    NULL),
    (3, DATE '2026-04-01', '人事',    NULL),
    (4, DATE '2026-04-01', '開発1室', NULL),
    (5, DATE '2026-04-01', '開発2室', NULL),
    (6, DATE '2026-04-01', '開発3室', NULL);

-- ---------------------------------------------------------------------
-- 役職
-- positionはダブルクォート付きで指定する。
-- ---------------------------------------------------------------------
INSERT INTO "position" (
    position_id,
    start_date,
    position_name,
    position_allowance,
    end_date
) VALUES
    (1, DATE '2026-04-01', '部長',   50000, NULL),
    (2, DATE '2026-04-01', '副部長', 30000, NULL),
    (3, DATE '2026-04-01', '課長',   20000, NULL),
    (4, DATE '2026-04-01', '係長',   10000, NULL);

-- ---------------------------------------------------------------------
-- 職能資格
-- allowanceは給与のベース金額。
-- ---------------------------------------------------------------------
INSERT INTO skill_grade (
    skill_grade,
    start_date,
    allowance,
    end_date
) VALUES
    (1,  DATE '2026-04-01', 200000, NULL),
    (2,  DATE '2026-04-01', 220000, NULL),
    (3,  DATE '2026-04-01', 250000, NULL),
    (4,  DATE '2026-04-01', 300000, NULL),
    (5,  DATE '2026-04-01', 350000, NULL),
    (6,  DATE '2026-04-01', 400000, NULL),
    (7,  DATE '2026-04-01', 450000, NULL),
    (8,  DATE '2026-04-01', 500000, NULL),
    (9,  DATE '2026-04-01', 550000, NULL),
    (10, DATE '2026-04-01', 600000, NULL);

-- ---------------------------------------------------------------------
-- 資格
-- is_advance:
--   false = 通常資格
--   true  = 高度資格
--
-- 高度資格を複数保有する場合の
-- 「2つ目以降は1資格につき10,000円加算」は給与Serviceで計算する。
-- ---------------------------------------------------------------------
INSERT INTO qualification (
    qualification_id,
    start_date,
    qualification_name,
    is_advance,
    qualification_allowance,
    end_date
) VALUES
    (1, DATE '2026-04-01', '基本情報技術者試験',                 FALSE,  3000, NULL),
    (2, DATE '2026-04-01', '応用情報技術者',                     FALSE,  5000, NULL),
    (3, DATE '2026-04-01', 'システムアーキテクト',               TRUE,  30000, NULL),
    (4, DATE '2026-04-01', 'プロジェクトマネージャ',             TRUE,  30000, NULL),
    (5, DATE '2026-04-01', 'ネットワークスペシャリスト',         TRUE,  30000, NULL),
    (6, DATE '2026-04-01', 'データベーススペシャリスト',         TRUE,  30000, NULL),
    (7, DATE '2026-04-01', 'エンベデッドシステムスペシャリスト', TRUE,  30000, NULL);

-- ---------------------------------------------------------------------
-- 初期ログイン社員
-- 部署・職能資格は有効な初期マスタを使用し、役職は未設定とする。
-- password_hash はパスワード「0001」の BCrypt ハッシュである。
-- ---------------------------------------------------------------------
INSERT INTO employee (
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
    DATE '2026-04-01',
    '0001',
    '$2a$10$DCVd3OAMoDq40RpGNRnYCO6TuhelMMaU.ovgc0Y44MyTBQJ./Lth2',
    '初期ログイン社員',
    DATE '2000-01-01',
    '1000001',
    '東京都千代田区',
    NULL,
    NULL,
    DATE '2026-04-01',
    NULL,
    1,
    1,
    NULL,
    NULL
);

-- 初期社員が0001を使用するため、次回の社員登録は0002から採番する。
ALTER SEQUENCE employee_no_seq RESTART WITH 2;

COMMIT;
