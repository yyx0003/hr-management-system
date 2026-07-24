-- =====================================================================
-- 人事給与管理システム DDL（PostgreSQL）
-- テーブル設計書__6_.xlsx ／ ER_v2_drawio に基づき作成
-- =====================================================================
--
-- 【設計方針の要点】
-- ・department（部署）／qualification（資格）／position（役職）／skill_grade（職能資格）は
--   いずれも複合主キー（ID + start_date）で「現在値と履歴を1テーブルで管理」する。
--   IDは業務側で採番するBIGINT（自動採番ではない）で、同一IDを複数行で共有する。
-- ・employee（社員）も同様に employee_id（BIGSERIAL）+ start_date の複合主キーであり、
--   1人の社員が複数行（部署・職能資格・役職の変更履歴分）を持つ。
--   department_id・skill_grade・position_idはemployee自身に格納し、外部キーは設定しない
--   （テーブル設計書の記載どおり）。
-- ・上記のとおりemployee_id単独では一意にならないため、salary_result／attendance／
--   employee_qualificationのemployee_id列にも外部キー制約は設定しない。
--   【補足】technicalには「参照側にもstart_dateを持たせて複合FKにする」ことは可能だが、
--   attendance等は"特定バージョンの社員"ではなく"社員そのもの"に紐づく情報のため、
--   そのような複合FKはデータモデルとして不自然になる。よって本スキーマでは採用しない。
--   参照整合性はアプリケーション側（Service層）で保証すること。具体的には：
--     1) INSERT／UPDATE前に参照先IDの存在確認を行う
--     2) 退職者削除バッチでは子テーブル→employeeの順で削除する（削除順序を明確にする）
--     3) テストケースに「存在しないID」を渡す異常系を含める
-- ・"position"はPostgreSQLの予約語（POSITION関数）と衝突するため、ダブルクォートで囲む。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 社員（employee）
-- ---------------------------------------------------------------------
CREATE SEQUENCE employee_no_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE employee (
    employee_id      BIGSERIAL       NOT NULL,  -- 【注意】新規社員登録時はDB自動採番でよいが、
                                                 -- 履歴行（部署・役職・職能資格の変更）追加時は
                                                 -- 必ず既存employee_idを明示的に指定してINSERTすること。
                                                 -- 指定を忘れるとDBが新しいIDを自動生成し、同一人物が
                                                 -- 別人として扱われてしまう（エラーにはならないため要注意）。
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
    department_id                              BIGINT          NOT NULL,  -- 論理参照: department.department_id（外部キーなし）
    skill_grade                                  BIGINT          NOT NULL,  -- 論理参照: skill_grade.skill_grade（外部キーなし）
    position_id                                    BIGINT,                  -- 論理参照: position.position_id（役職なしはNULL、外部キーなし）
    end_date                                          DATE,
    created_at                                          TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at                                          TIMESTAMP       NOT NULL DEFAULT now(),
    CONSTRAINT pk_employee PRIMARY KEY (employee_id, start_date),
    CONSTRAINT ck_employee_date_range CHECK (end_date IS NULL OR end_date >= start_date),
    -- 退職日は入社日以降でなければならない
    CONSTRAINT ck_employee_employment_date_range CHECK (retire_date IS NULL OR retire_date >= hire_date),
    -- 履歴行のstart_dateは入社日より前になり得ない（システム移行日の概念は無いため常に成立する）
    CONSTRAINT ck_employee_start_date CHECK (start_date >= hire_date),
    -- 職能資格の等級は1〜10の範囲
    CONSTRAINT ck_employee_skill_grade CHECK (skill_grade BETWEEN 1 AND 10)
);
COMMENT ON TABLE employee IS '社員の基本情報を管理するテーブル（employee_id+start_dateの複合主キーで現在値・履歴を一体管理）';
COMMENT ON COLUMN employee.employee_no IS '業務用の社員番号。CSV取込・出力、画面表示で使用する。';
COMMENT ON COLUMN employee.retire_date IS '退職年月日（3カ月経過後に社員データが削除）';
-- 同一employee_idにつき「現在有効（end_date IS NULL）」な行は1件のみとする
--CREATE UNIQUE INDEX uq_employee_current ON employee (employee_id) WHERE end_date IS NULL;
-- 【修正】employeeは履歴テーブルであり、同一人物の複数行が同じemployee_noを共有するため、
-- employee_no単体の完全なUNIQUE制約は誤り（履歴行の追加でエラーになる）。
-- 「現在有効な行の中で重複しない」という制約に修正する。
--CREATE UNIQUE INDEX uq_employee_no_current ON employee (employee_no) WHERE end_date IS NULL;
-- 【未対応】同一employee_idの適用期間（start_date～end_date）が重複しないことはDB側では
-- 保証していない（PostgreSQLのEXCLUDE制約を使えば可能だが、本プロジェクトでは複雑さを避けて
-- Service層でのチェックに委ねる）。department／qualification／"position"／skill_gradeも同様。

-- ---------------------------------------------------------------------
-- 部署（department）※現在値・履歴を複合主キーで一体管理
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
    -- 部署変更は月初（1日）からのみ適用可能（要件どおり月の途中での変更は不可）。
    -- 資格・役職・職能資格マスタには同様の制約は無いため、他のテーブルには追加しないこと。
    CONSTRAINT ck_department_start_date_month_begin CHECK (EXTRACT(DAY FROM start_date) = 1)
);
COMMENT ON TABLE department IS '過去分を含む部署を管理するテーブル';
--CREATE UNIQUE INDEX uq_department_current ON department (department_id) WHERE end_date IS NULL;

-- ---------------------------------------------------------------------
-- 資格（qualification）※現在値・履歴を複合主キーで一体管理
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
COMMENT ON COLUMN qualification.is_advance IS '高度資格か判定するフラグ（高度資格ならtrue）';
--CREATE UNIQUE INDEX uq_qualification_current ON qualification (qualification_id) WHERE end_date IS NULL;

-- ---------------------------------------------------------------------
-- 役職（position）※現在値・履歴を複合主キーで一体管理
-- 【注意】"position" はPostgreSQLの予約語のため、ダブルクォートで囲む。
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
--CREATE UNIQUE INDEX uq_position_current ON "position" (position_id) WHERE end_date IS NULL;

-- ---------------------------------------------------------------------
-- 職能資格（skill_grade）※現在値・履歴を複合主キーで一体管理
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
COMMENT ON COLUMN skill_grade.skill_grade IS '職能資格の等級番号（1〜10）';
--CREATE UNIQUE INDEX uq_skill_grade_current ON skill_grade (skill_grade) WHERE end_date IS NULL;

-- ---------------------------------------------------------------------
-- 社員取得資格（employee_qualification）
-- ---------------------------------------------------------------------
CREATE TABLE employee_qualification (
    employee_id         BIGINT      NOT NULL,  -- 論理参照: employee.employee_id（外部キーなし）
    qualification_id      BIGINT      NOT NULL,  -- 論理参照: qualification.qualification_id（外部キーなし）
    acquisition_date         DATE        NOT NULL,
    created_at                 TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT pk_employee_qualification PRIMARY KEY (employee_id, qualification_id)
);
COMMENT ON TABLE employee_qualification IS '社員が取得した資格のテーブル';

-- ---------------------------------------------------------------------
-- 祝日（holiday）
-- ---------------------------------------------------------------------
CREATE TABLE holiday (
    holiday_date    DATE            NOT NULL,
    holiday_type      VARCHAR(20)     NOT NULL,
    holiday_name        VARCHAR(20)     NOT NULL,
    created_at             TIMESTAMP       NOT NULL DEFAULT now(),
    updated_at                TIMESTAMP       NOT NULL DEFAULT now(),
    CONSTRAINT pk_holiday PRIMARY KEY (holiday_date)
);
COMMENT ON TABLE holiday IS '過去分の祝日、夏季休暇、冬期休暇を管理するテーブル';

-- ---------------------------------------------------------------------
-- 勤怠データ（attendance）
-- 【修正】work_typeにNOT NULL・CHECKを付与（DEFAULTは設けない）し、
-- work_typeごとに出退勤時刻の有無・前後関係を検証するCHECKを追加した（レビュー指摘反映）。
-- ---------------------------------------------------------------------
CREATE TABLE attendance (
    employee_id       BIGINT      NOT NULL,  -- 論理参照: employee.employee_id（外部キーなし）
    work_date            DATE        NOT NULL,
    attendance_time         TIME,
    leaving_time               TIME,
    work_type                     VARCHAR(20) NOT NULL,
    created_at                       TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at                          TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT pk_attendance PRIMARY KEY (employee_id, work_date),
    CONSTRAINT ck_attendance_work_type CHECK (work_type IN ('NORMAL', 'PAID_LEAVE', 'ABSENCE', 'HOLIDAY_WORK')),
    -- 【修正】work_typeごとに出退勤時刻の有無・前後関係を検証する。
    -- NORMAL・HOLIDAY_WORKは両方必須で退勤＞出勤、PAID_LEAVE・ABSENCEは両方NULL必須。
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
COMMENT ON COLUMN attendance.work_type IS '通常勤務、有給休暇、欠勤、休日出勤を区分（NORMAL/PAID_LEAVE/ABSENCE/HOLIDAY_WORK）。DBのデフォルト値は設けない（アプリ側の実装漏れをDBが隠蔽しないため）。';

-- ---------------------------------------------------------------------
-- 給与実績（salary_result）
-- 【修正】total_work_hours／total_overtime_hoursをREALからNUMERICへ変更
-- （浮動小数点誤差を避けるため、レビュー指摘反映）。
-- ---------------------------------------------------------------------
CREATE TABLE salary_result (
    employee_id             BIGINT      NOT NULL,  -- 論理参照: employee.employee_id（外部キーなし。参照整合性はService層で確認すること）
    target_year               BIGINT      NOT NULL,
    target_month                 BIGINT      NOT NULL,
    department_id                   BIGINT      NOT NULL,  -- 論理参照: department.department_id（複合PKのため単純なFK制約は設定不可）
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
COMMENT ON TABLE salary_result IS '従業員の月ごとの給与を管理するテーブル';
COMMENT ON COLUMN salary_result.department_id IS '対象年月時点での部署ID（計算時点のスナップショット）';

-- ---------------------------------------------------------------------
-- 検索性能向上のためのインデックス
-- ---------------------------------------------------------------------
CREATE INDEX idx_employee_retire_date ON employee (retire_date);
CREATE INDEX idx_attendance_work_date ON attendance (work_date);
CREATE INDEX idx_salary_result_target ON salary_result (target_year, target_month);
