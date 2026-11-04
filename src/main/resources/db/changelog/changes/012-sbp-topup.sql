--liquibase formatted sql

--changeset pricing:012-sbp-topup
-- Пополнение своего счёта из другого банка по СБП (me2me) — без комиссии
INSERT INTO commission_rules (operation_type, source_type, destination_type, subscription, percent, min_fee, fixed_fee, label, sort_order)
SELECT 'OWN_TRANSFER', 'SBP', d.destination_type, s.subscription, 0, 0, 0, 'Из своего счёта в другом банке по СБП', d.sort_order
FROM (VALUES ('WALLET', 90), ('OUR_CARD', 91)) AS d (destination_type, sort_order)
CROSS JOIN (VALUES ('NONE'), ('PREMIUM'), ('PREMIUM_PLUS')) AS s (subscription);
