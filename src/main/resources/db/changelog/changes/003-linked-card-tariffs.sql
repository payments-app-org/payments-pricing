--liquibase formatted sql

--changeset pricing:003-linked-card-tariffs
-- Тарифы для привязанной карты другого банка как источника. По СБП с неё переводить нельзя — правила нет.
WITH base (operation_type, source_type, destination_type, percent, min_fee, label, sort_order) AS (VALUES
    ('OWN_TRANSFER', 'OTHER_BANK_CARD', 'WALLET',          3.00,  0, 'С карты другого банка',  65),
    ('OWN_TRANSFER', 'OTHER_BANK_CARD', 'OUR_CARD',        2.00,  0, 'С карты другого банка',  80),
    ('CARD2CARD',    'OTHER_BANK_CARD', 'OUR_CARD',        2.00, 50, 'На карту МТС Банка',     90),
    ('CARD2CARD',    'OTHER_BANK_CARD', 'OTHER_BANK_CARD', 2.00, 50, 'На карту другого банка', 100)
)
INSERT INTO commission_rules (operation_type, source_type, destination_type, subscription, percent, min_fee, fixed_fee, label, sort_order)
SELECT b.operation_type, b.source_type, b.destination_type, s.subscription,
       CASE WHEN s.subscription = 'NONE' THEN b.percent
            WHEN s.subscription = 'PREMIUM' THEN GREATEST(b.percent - 0.5, 0.5)
            ELSE GREATEST(b.percent - 1.0, 0.3) END,
       CASE WHEN b.min_fee = 0 THEN 0 WHEN s.subscription = 'NONE' THEN b.min_fee WHEN s.subscription = 'PREMIUM' THEN 30 ELSE 20 END,
       0, b.label, b.sort_order
FROM base b CROSS JOIN (VALUES ('NONE'), ('PREMIUM'), ('PREMIUM_PLUS')) AS s (subscription);
