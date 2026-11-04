--liquibase formatted sql

--changeset pricing:008-bill
-- Оплата поставщикам — один тип BILL, тариф по группе поставщика (destination_type).
-- Связь, госуслуги, транспорт, игры — без комиссии (цена номиналов у игр уже с комиссией поставщика),
-- зарубежная связь — 2%, мин. 20 ₽ (подписки дешевле).
DELETE FROM commission_rules WHERE operation_type IN ('UIN_PAYMENT', 'MOBILE_PAYMENT');
ALTER TABLE commission_rules DROP CONSTRAINT commission_rules_operation_type_check;
ALTER TABLE commission_rules ADD CONSTRAINT commission_rules_operation_type_check CHECK (operation_type IN
    ('SBP_PHONE', 'CARD2CARD', 'OWN_TRANSFER', 'ABROAD', 'QR', 'WALLET_TRANSFER', 'REQUISITES', 'REQUISITES_OUR', 'BILL'));

WITH base (destination_type, percent, min_fee, label, sort_order) AS (VALUES
    ('MOBILE',         0.00,  0, 'Мобильная связь',       10),
    ('GOV',            0.00,  0, 'Госуслуги, штрафы, налоги', 20),
    ('TRANSPORT',      0.00,  0, 'Транспорт',             30),
    ('GAMES',          0.00,  0, 'Игры и сервисы',        40),
    ('FOREIGN_MOBILE', 2.00, 20, 'Зарубежные операторы',  50)
)
INSERT INTO commission_rules (operation_type, source_type, destination_type, subscription, percent, min_fee, fixed_fee, label, sort_order)
SELECT 'BILL', src.source_type, b.destination_type, s.subscription,
       CASE WHEN b.percent = 0 OR s.subscription = 'NONE' THEN b.percent
            WHEN s.subscription = 'PREMIUM' THEN 1.50 ELSE 1.00 END,
       CASE WHEN b.min_fee = 0 THEN 0 WHEN s.subscription = 'NONE' THEN b.min_fee WHEN s.subscription = 'PREMIUM' THEN 15 ELSE 10 END,
       0, b.label, b.sort_order + src.shift
FROM base b
CROSS JOIN (VALUES ('WALLET', 0), ('OUR_CARD', 1)) AS src (source_type, shift)
CROSS JOIN (VALUES ('NONE'), ('PREMIUM'), ('PREMIUM_PLUS')) AS s (subscription);
