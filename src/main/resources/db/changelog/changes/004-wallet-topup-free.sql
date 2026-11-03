--liquibase formatted sql

--changeset pricing:004-wallet-topup-free
-- Пополнение «Моего кошелька» со своих карт — без комиссии
UPDATE commission_rules SET percent = 0, min_fee = 0, fixed_fee = 0
WHERE operation_type = 'OWN_TRANSFER' AND destination_type = 'WALLET' AND source_type IN ('OUR_CARD', 'OTHER_BANK_CARD');

-- Со своего счёта на свою привязанную карту другого банка (деньги доставляет процессинг)
INSERT INTO commission_rules (operation_type, source_type, destination_type, subscription, percent, min_fee, fixed_fee, label, sort_order)
SELECT 'OWN_TRANSFER', b.source_type, 'OTHER_BANK_CARD', s.subscription,
       CASE s.subscription WHEN 'NONE' THEN b.percent WHEN 'PREMIUM' THEN b.percent - 0.5 ELSE b.percent - 1.0 END,
       CASE s.subscription WHEN 'NONE' THEN 50 WHEN 'PREMIUM' THEN 30 ELSE 20 END,
       0, 'На карту другого банка', b.sort_order
FROM (VALUES ('WALLET', 1.50, 35), ('OUR_CARD', 2.00, 75)) AS b (source_type, percent, sort_order)
CROSS JOIN (VALUES ('NONE'), ('PREMIUM'), ('PREMIUM_PLUS')) AS s (subscription);
