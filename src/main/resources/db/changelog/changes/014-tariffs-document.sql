--liquibase formatted sql

--changeset pricing:014-premium-rename
-- «МТС Premium» → «Премиум»
ALTER TABLE commission_rules DROP CONSTRAINT commission_rules_subscription_check;
UPDATE commission_rules SET subscription = 'PREMIUM' WHERE subscription = 'MTS_PREMIUM';
ALTER TABLE commission_rules ADD CONSTRAINT commission_rules_subscription_check
    CHECK (subscription IN ('NONE', 'PREMIUM', 'BANK_PREMIUM'));

--changeset pricing:014-tariffs-document
-- Тарифы по документу «Тарифы на переводы и платежи» (frontend/src/data/documents.ts — его наша версия).
-- Строки документа — куда, столбцы — откуда. Одинаковы для всех подписок; отличия «Банк Премиум» — ниже.
WITH t (operation_type, source_type, destination_type, percent, min_fee, fixed_fee, label, sort_order) AS (VALUES
    -- 1. Между своими счетами
    ('OWN_TRANSFER', 'OUR_CARD',         'OUR_CARD',         0.0,  0, 0, 'Между картами МТС Банка',       70),
    ('OWN_TRANSFER', 'OTHER_BANK_CARD',  'OUR_CARD',         0.5,  0, 0, 'С карты другого банка',         75),
    ('OWN_TRANSFER', 'WALLET',           'OUR_CARD',         1.5, 50, 0, 'На карту МТС Банка',            10),
    ('OWN_TRANSFER', 'WALLET',           'PARTNER_CARD',     3.0,  0, 0, 'На карту МТС Деньги',           30),
    ('OWN_TRANSFER', 'OUR_CARD',         'OTHER_BANK_CARD',  1.5, 50, 0, 'На карту другого банка',        80),
    ('OWN_TRANSFER', 'WALLET',           'OTHER_BANK_CARD',  1.5, 50, 0, 'На карту другого банка',        35),
    ('OWN_TRANSFER', 'OUR_CARD',         'WALLET',           0.0,  0, 0, 'С карты МТС Банка',             40),
    ('OWN_TRANSFER', 'PARTNER_CARD',     'WALLET',           0.0,  0, 0, 'С карты МТС Деньги',            60),
    ('OWN_TRANSFER', 'OTHER_BANK_CARD',  'WALLET',           3.0,  0, 0, 'С карты другого банка',         65),
    ('OWN_TRANSFER', 'OPERATOR_ACCOUNT', 'WALLET',           3.0,  0, 0, 'С лицевого счёта МТС',          50),
    ('OWN_TRANSFER', 'WALLET',           'OPERATOR_ACCOUNT', 0.0,  0, 0, 'На лицевой счёт МТС',           20),
    -- 2. По номеру карты
    ('CARD2CARD',    'OUR_CARD',         'OTHER_BANK_CARD',  1.5, 50, 0, 'На карту другого банка',        80),
    ('CARD2CARD',    'OTHER_BANK_CARD',  'OTHER_BANK_CARD',  1.5, 50, 0, 'На карту другого банка',        90),
    ('CARD2CARD',    'WALLET',           'OTHER_BANK_CARD',  1.5, 50, 0, 'На карту другого банка',        30),
    ('CARD2CARD',    'OUR_CARD',         'OUR_CARD',         0.0,  0, 0, 'На карту МТС Банка',            70),
    ('CARD2CARD',    'OTHER_BANK_CARD',  'OUR_CARD',         0.5,  0, 0, 'На карту МТС Банка',            85),
    ('CARD2CARD',    'WALLET',           'OUR_CARD',         1.5, 50, 0, 'На карту МТС Банка',            10),
    ('CARD2CARD',    'WALLET',           'PARTNER_CARD',     3.0,  0, 0, 'На карту МТС Деньги',           20),
    ('CARD2CARD',    'OUR_CARD',         'WALLET',           0.0,  0, 0, 'С карты МТС Банка',             40),
    ('CARD2CARD',    'PARTNER_CARD',     'WALLET',           0.0,  0, 0, 'С карты МТС Деньги',            50),
    ('CARD2CARD',    'OTHER_BANK_CARD',  'WALLET',           3.0,  0, 0, 'С карты другого банка',         60),
    -- 5. По реквизитам (7.21 — юрлицу в другой банк, 7.22 — на счёт в МТС Банке)
    ('REQUISITES',     'OUR_CARD', 'LEGAL_ENTITY',     0.0,  0, 0, 'Юрлицу в другой банк',   10),
    ('REQUISITES',     'WALLET',   'LEGAL_ENTITY',     1.0, 50, 0, 'Юрлицу в другой банк',   20),
    ('REQUISITES_OUR', 'OUR_CARD', 'OUR_LEGAL_ENTITY', 0.0,  0, 0, 'Юрлицу в нашем банке',   10),
    ('REQUISITES_OUR', 'WALLET',   'OUR_LEGAL_ENTITY', 1.0,  0, 0, 'Юрлицу в нашем банке',   20),
    -- 7.1. Оплата по QR
    ('QR',           'WALLET',           'QR',               1.0,  0, 0, 'Оплата по QR',                  10),
    ('QR',           'OUR_CARD',         'QR',               0.0,  0, 0, 'Оплата по QR',                  20)
)
INSERT INTO commission_rules (operation_type, source_type, destination_type, subscription, percent, min_fee, fixed_fee, label, sort_order)
SELECT t.operation_type, t.source_type, t.destination_type, s.subscription, t.percent, t.min_fee, t.fixed_fee, t.label, t.sort_order
FROM t CROSS JOIN (VALUES ('NONE'), ('PREMIUM'), ('BANK_PREMIUM')) AS s (subscription)
ON CONFLICT (operation_type, source_type, destination_type, subscription)
DO UPDATE SET percent = EXCLUDED.percent, min_fee = EXCLUDED.min_fee, fixed_fee = EXCLUDED.fixed_fee,
              max_fee = NULL, free_monthly_amount = NULL;

-- 3. По номеру телефона через СБП — только с карт; с «Моего кошелька» перевода по СБП нет.
-- До 100 000 ₽ в месяц бесплатно, с превышения 0,5% (мин. 1 ₽, макс. 1 500 ₽)
DELETE FROM commission_rules WHERE operation_type = 'SBP_PHONE' AND source_type = 'WALLET' AND destination_type = 'SBP';
UPDATE commission_rules SET percent = 0.5, min_fee = 1, fixed_fee = 0, max_fee = 1500, free_monthly_amount = 100000
WHERE operation_type = 'SBP_PHONE' AND destination_type = 'SBP';

-- 7. Платежи (BILL): с карты МТС Банка / с «Моего кошелька» / с карты другого банка
WITH g (destination_type, card_pct, wallet_pct, other_pct, min_fee, fixed_fee) AS (VALUES
    ('MOBILE',          0.0, 1.0, 1.5,  0,  0),   -- 7.4 (платежи в пользу МТС — 7.3, 0%, — не выделяем)
    ('SECURITY',        0.0, 1.0, 1.5,  0,  0),   -- 7.6
    ('DONATIONS',       0.0, 1.5, 1.5,  0,  0),   -- 7.8
    ('TRAVEL',          0.0, 1.0, 1.5,  0,  0),   -- 7.9 Бронирование
    ('GOV',             0.0, 1.5, 1.5, 10,  0),   -- 7.10 (минимум — только с кошелька и чужой карты)
    ('UTILITIES',       0.0, 1.0, 1.5,  0,  0),   -- 7.11
    ('GAMES',           0.0, 0.0, 0.0,  0, 50),   -- 7.12 — 50 ₽ с любого источника
    ('INTERNET',        0.0, 2.0, 2.0,  0,  0),   -- 7.13
    ('EDUCATION',       0.0, 3.0, 3.0,  0,  0),   -- 7.16
    ('INSURANCE',       0.0, 1.0, 1.5,  0,  0),   -- 7.25
    ('SHOPS',           0.0, 1.0, 1.5,  0,  0),   -- 7.26 Товары
    ('TRANSPORT',       0.0, 1.0, 1.5,  0,  0),   -- 7.27
    ('MISC',            0.0, 0.0, 0.0,  0,  0),
    ('EMONEY',          1.0, 1.0, 1.0, 10,  0),
    ('FOREIGN_MOBILE',  2.0, 2.0, 2.0, 20,  0)
), r AS (
    SELECT destination_type, 'OUR_CARD' AS source_type, card_pct AS percent,
           CASE WHEN card_pct > 0 THEN min_fee ELSE 0 END AS min_fee, fixed_fee FROM g
    UNION ALL SELECT destination_type, 'WALLET', wallet_pct, CASE WHEN wallet_pct > 0 THEN min_fee ELSE 0 END, fixed_fee FROM g
    UNION ALL SELECT destination_type, 'OTHER_BANK_CARD', other_pct, CASE WHEN other_pct > 0 THEN min_fee ELSE 0 END, fixed_fee FROM g
)
INSERT INTO commission_rules (operation_type, source_type, destination_type, subscription, percent, min_fee, fixed_fee, label, sort_order)
SELECT 'BILL', r.source_type, r.destination_type, s.subscription, r.percent, r.min_fee, r.fixed_fee, 'Оплата услуг', 100
FROM r CROSS JOIN (VALUES ('NONE'), ('PREMIUM'), ('BANK_PREMIUM')) AS s (subscription)
ON CONFLICT (operation_type, source_type, destination_type, subscription)
DO UPDATE SET percent = EXCLUDED.percent, min_fee = EXCLUDED.min_fee, fixed_fee = EXCLUDED.fixed_fee;

--changeset pricing:014-bank-premium
-- «Банк Премиум»: по СБП бесплатно до 1 500 000 ₽ в месяц, на карту другого банка — 99 ₽ за перевод
UPDATE commission_rules SET free_monthly_amount = 1500000
WHERE subscription = 'BANK_PREMIUM' AND operation_type = 'SBP_PHONE' AND destination_type = 'SBP';
UPDATE commission_rules SET percent = 0, min_fee = 0, fixed_fee = 99, max_fee = NULL
WHERE subscription = 'BANK_PREMIUM' AND operation_type = 'CARD2CARD' AND source_type = 'OUR_CARD' AND destination_type = 'OTHER_BANK_CARD';
