--liquibase formatted sql

--changeset pricing:009-bill-more-groups
-- Тарифы оплаты поставщикам новых групп. Пополнение электронного кошелька — 1% (мин. 10 ₽), остальное без комиссии.
WITH base (destination_type, percent, min_fee, label, sort_order) AS (VALUES
    ('EMONEY',    1.00, 10, 'Электронные деньги', 60),
    ('EDUCATION', 0.00,  0, 'Образование',        70),
    ('SHOPS',     0.00,  0, 'Интернет-магазины',  80),
    ('TRAVEL',    0.00,  0, 'Путешествия',        90),
    ('INSURANCE', 0.00,  0, 'Страхование',       100)
)
INSERT INTO commission_rules (operation_type, source_type, destination_type, subscription, percent, min_fee, fixed_fee, label, sort_order)
SELECT 'BILL', src.source_type, b.destination_type, s.subscription,
       CASE WHEN b.percent = 0 OR s.subscription = 'NONE' THEN b.percent
            WHEN s.subscription = 'PREMIUM' THEN 0.70 ELSE 0.50 END,
       CASE WHEN b.min_fee = 0 THEN 0 WHEN s.subscription = 'NONE' THEN b.min_fee ELSE 5 END,
       0, b.label, b.sort_order + src.shift
FROM base b
CROSS JOIN (VALUES ('WALLET', 0), ('OUR_CARD', 1)) AS src (source_type, shift)
CROSS JOIN (VALUES ('NONE'), ('PREMIUM'), ('PREMIUM_PLUS')) AS s (subscription);
