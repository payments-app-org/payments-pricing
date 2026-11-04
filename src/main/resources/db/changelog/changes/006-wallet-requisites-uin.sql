--liquibase formatted sql

--changeset pricing:006-wallet-requisites-uin
-- Тарифы новых операций: на кошелёк другого клиента, по реквизитам юрлицу (в другой банк / в наш), госуслуги по УИН
ALTER TABLE commission_rules DROP CONSTRAINT commission_rules_operation_type_check;
ALTER TABLE commission_rules ADD CONSTRAINT commission_rules_operation_type_check CHECK (operation_type IN
    ('SBP_PHONE', 'CARD2CARD', 'OWN_TRANSFER', 'ABROAD', 'QR', 'WALLET_TRANSFER', 'REQUISITES', 'REQUISITES_OUR', 'UIN_PAYMENT'));

WITH base (operation_type, source_type, destination_type, percent, min_fee, label, sort_order) AS (VALUES
    ('WALLET_TRANSFER', 'WALLET',   'WALLET',           1.00,  0, 'На кошелёк другого клиента', 10),
    ('WALLET_TRANSFER', 'OUR_CARD', 'WALLET',           1.00,  0, 'На кошелёк другого клиента', 20),
    ('REQUISITES',      'WALLET',   'LEGAL_ENTITY',     1.00, 30, 'Юрлицу в другой банк',       10),
    ('REQUISITES',      'OUR_CARD', 'LEGAL_ENTITY',     1.00, 30, 'Юрлицу в другой банк',       20),
    ('REQUISITES_OUR',  'WALLET',   'OUR_LEGAL_ENTITY', 0.50,  0, 'Юрлицу в нашем банке',       10),
    ('REQUISITES_OUR',  'OUR_CARD', 'OUR_LEGAL_ENTITY', 0.50,  0, 'Юрлицу в нашем банке',       20),
    ('UIN_PAYMENT',     'WALLET',   'GOV',              0.00,  0, 'Госуслуги по УИН',           10),
    ('UIN_PAYMENT',     'OUR_CARD', 'GOV',              0.00,  0, 'Госуслуги по УИН',           20)
)
INSERT INTO commission_rules (operation_type, source_type, destination_type, subscription, percent, min_fee, fixed_fee, label, sort_order)
SELECT b.operation_type, b.source_type, b.destination_type, s.subscription,
       CASE WHEN b.percent = 0 OR s.subscription = 'NONE' THEN b.percent
            WHEN s.subscription = 'PREMIUM' THEN GREATEST(b.percent - 0.5, 0.3)
            ELSE GREATEST(b.percent - 1.0, 0.2) END,
       CASE WHEN b.min_fee = 0 THEN 0 WHEN s.subscription = 'NONE' THEN b.min_fee WHEN s.subscription = 'PREMIUM' THEN 20 ELSE 10 END,
       0, b.label, b.sort_order
FROM base b CROSS JOIN (VALUES ('NONE'), ('PREMIUM'), ('PREMIUM_PLUS')) AS s (subscription);
