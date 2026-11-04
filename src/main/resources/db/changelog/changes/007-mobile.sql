--liquibase formatted sql

--changeset pricing:007-mobile
-- Мобильная связь — без комиссии, как у большинства банков
ALTER TABLE commission_rules DROP CONSTRAINT commission_rules_operation_type_check;
ALTER TABLE commission_rules ADD CONSTRAINT commission_rules_operation_type_check CHECK (operation_type IN
    ('SBP_PHONE', 'CARD2CARD', 'OWN_TRANSFER', 'ABROAD', 'QR', 'WALLET_TRANSFER', 'REQUISITES', 'REQUISITES_OUR', 'UIN_PAYMENT',
     'MOBILE_PAYMENT'));

INSERT INTO commission_rules (operation_type, source_type, destination_type, subscription, percent, min_fee, fixed_fee, label, sort_order)
SELECT 'MOBILE_PAYMENT', src.source_type, 'MOBILE', s.subscription, 0, 0, 0, 'Мобильная связь', src.sort_order
FROM (VALUES ('WALLET', 10), ('OUR_CARD', 20)) AS src (source_type, sort_order)
CROSS JOIN (VALUES ('NONE'), ('PREMIUM'), ('PREMIUM_PLUS')) AS s (subscription);
