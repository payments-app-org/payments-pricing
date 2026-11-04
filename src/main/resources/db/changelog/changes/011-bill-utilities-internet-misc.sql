--liquibase formatted sql

--changeset pricing:011-bill-utilities-internet-misc
-- ЖКХ, интернет и ТВ, «Разное» — без комиссии
INSERT INTO commission_rules (operation_type, source_type, destination_type, subscription, percent, min_fee, fixed_fee, label, sort_order)
SELECT 'BILL', src.source_type, b.destination_type, s.subscription, 0, 0, 0, b.label, b.sort_order + src.shift
FROM (VALUES ('UTILITIES', 'Услуги ЖКХ', 130), ('INTERNET', 'Интернет и ТВ, Телефония', 140), ('MISC', 'Разное', 150))
    AS b (destination_type, label, sort_order)
CROSS JOIN (VALUES ('WALLET', 0), ('OUR_CARD', 1)) AS src (source_type, shift)
CROSS JOIN (VALUES ('NONE'), ('PREMIUM'), ('PREMIUM_PLUS')) AS s (subscription);
