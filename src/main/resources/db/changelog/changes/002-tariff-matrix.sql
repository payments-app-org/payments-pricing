--liquibase formatted sql

--changeset pricing:002-tariff-matrix
-- Комиссия зависит от вида операции, откуда списываем, куда зачисляем и подписки.
-- Комиссия = max(сумма × percent / 100, min_fee) + fixed_fee. id правила — тариф, он попадает в котировку и платёж.
-- Значения временно завышены, чтобы комиссию было видно при разработке.
DROP TABLE commission_rules;

CREATE TABLE commission_rules (
    id                SERIAL PRIMARY KEY,
    operation_type    VARCHAR(30)   NOT NULL
        CHECK (operation_type IN ('SBP_PHONE', 'CARD2CARD', 'OWN_TRANSFER', 'ABROAD', 'QR')),
    -- Откуда: WALLET — «Мой кошелёк», OUR_CARD — карта нашего банка, остальные — внешние источники
    source_type       VARCHAR(30)   NOT NULL,
    -- Куда: карта, кошелёк, СБП, оплата связи, перевод за рубеж, QR
    destination_type  VARCHAR(30)   NOT NULL,
    subscription      VARCHAR(20)   NOT NULL CHECK (subscription IN ('NONE', 'PREMIUM', 'PREMIUM_PLUS')),
    percent           NUMERIC(5, 2)  NOT NULL CHECK (percent >= 0),
    min_fee           NUMERIC(19, 2) NOT NULL DEFAULT 0,
    fixed_fee         NUMERIC(19, 2) NOT NULL DEFAULT 0,
    -- Подпись строки в «Лимитах и условиях счёта»
    label             VARCHAR(100)  NOT NULL,
    sort_order        INTEGER       NOT NULL,
    UNIQUE (operation_type, source_type, destination_type, subscription)
);

-- Базовые тарифы (без подписки). Премиум: процент −0,5 п.п., минимум 30 ₽; Премиум+: −1 п.п. (но не ниже 0,3%), минимум 20 ₽.
-- Нулевые тарифы ниже ноля не опускаются; за рубеж — всегда 0%.
WITH base (operation_type, source_type, destination_type, percent, min_fee, fixed_fee, label, sort_order) AS (VALUES
    -- Переводы между счетами: с кошелька / на кошелёк
    ('OWN_TRANSFER', 'WALLET',           'OUR_CARD',         1.50, 50, 0,  'На карту МТС Банка',         10),
    ('OWN_TRANSFER', 'WALLET',           'OPERATOR_ACCOUNT', 1.00,  0, 0,  'На лицевой счёт МТС',        20),
    ('OWN_TRANSFER', 'WALLET',           'PARTNER_CARD',     3.00,  0, 0,  'На карту МТС Деньги',        30),
    ('OWN_TRANSFER', 'OUR_CARD',         'WALLET',           1.00,  0, 0,  'С карты МТС Банка',          40),
    ('OWN_TRANSFER', 'OPERATOR_ACCOUNT', 'WALLET',           3.00,  0, 0,  'С лицевого счёта МТС',       50),
    ('OWN_TRANSFER', 'PARTNER_CARD',     'WALLET',           1.00,  0, 0,  'С карты МТС Деньги',         60),
    ('OWN_TRANSFER', 'OUR_CARD',         'OUR_CARD',         1.00,  0, 0,  'Между картами МТС Банка',    70),
    -- За рубеж
    ('ABROAD',       'WALLET',           'ABROAD',           0.00,  0, 0,  'За рубеж',                   10),
    ('ABROAD',       'OUR_CARD',         'ABROAD',           0.00,  0, 0,  'За рубеж',                   20),
    -- По номеру телефона
    ('SBP_PHONE',    'WALLET',           'SBP',              3.00, 50, 0,  'Через СБП',                  10),
    ('SBP_PHONE',    'WALLET',           'WALLET',           1.00,  0, 0,  'На другой кошелёк',          20),
    ('SBP_PHONE',    'WALLET',           'MOBILE',           1.00,  0, 0,  'Оплата сотовой связи',       30),
    ('SBP_PHONE',    'OUR_CARD',         'SBP',              2.00, 30, 0,  'Через СБП',                  40),
    -- По номеру карты
    ('CARD2CARD',    'WALLET',           'OUR_CARD',         1.50, 50, 0,  'На карту МТС Банка',         10),
    ('CARD2CARD',    'WALLET',           'PARTNER_CARD',     3.00,  0, 0,  'На карту МТС Деньги',        20),
    ('CARD2CARD',    'WALLET',           'OTHER_BANK_CARD',  1.50, 50, 0,  'На карту другого банка',     30),
    ('CARD2CARD',    'OUR_CARD',         'WALLET',           1.00,  0, 0,  'С карты МТС Банка',          40),
    ('CARD2CARD',    'PARTNER_CARD',     'WALLET',           1.00,  0, 0,  'С карты МТС Деньги',         50),
    ('CARD2CARD',    'OTHER_BANK_CARD',  'WALLET',           3.00,  0, 0,  'С карты другого банка',      60),
    ('CARD2CARD',    'OUR_CARD',         'OUR_CARD',         1.00,  0, 0,  'На карту МТС Банка',         70),
    ('CARD2CARD',    'OUR_CARD',         'OTHER_BANK_CARD',  2.00, 50, 0,  'На карту другого банка',     80),
    -- Оплата по QR
    ('QR',           'WALLET',           'QR',               1.50,  0, 10, 'Оплата по QR',               10)
)
INSERT INTO commission_rules (operation_type, source_type, destination_type, subscription, percent, min_fee, fixed_fee, label, sort_order)
SELECT b.operation_type, b.source_type, b.destination_type, s.subscription,
       CASE
           WHEN b.percent = 0 OR s.subscription = 'NONE' THEN b.percent
           WHEN s.subscription = 'PREMIUM' THEN GREATEST(b.percent - 0.5, 0.5)
           ELSE GREATEST(b.percent - 1.0, 0.3)
       END,
       CASE WHEN b.min_fee = 0 THEN 0 WHEN s.subscription = 'NONE' THEN b.min_fee WHEN s.subscription = 'PREMIUM' THEN 30 ELSE 20 END,
       CASE WHEN b.fixed_fee = 0 THEN 0 WHEN s.subscription = 'NONE' THEN b.fixed_fee WHEN s.subscription = 'PREMIUM' THEN 5 ELSE 0 END,
       b.label, b.sort_order
FROM base b CROSS JOIN (VALUES ('NONE'), ('PREMIUM'), ('PREMIUM_PLUS')) AS s (subscription);
