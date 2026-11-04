--liquibase formatted sql

--changeset pricing:013-fee-cap-and-free-limit
-- max_fee — комиссия не больше; free_monthly_amount — сколько в месяц без комиссии (комиссия — только с превышения)
ALTER TABLE commission_rules ADD COLUMN max_fee NUMERIC(19, 2) CHECK (max_fee IS NULL OR max_fee >= 0);
ALTER TABLE commission_rules ADD COLUMN free_monthly_amount NUMERIC(19, 2) CHECK (free_monthly_amount IS NULL OR free_monthly_amount >= 0);

--changeset pricing:013-real-tariffs-none
-- Реальные тарифы без подписки
-- По номеру телефона через СБП: до 100 000 ₽ в месяц бесплатно, дальше 0,5%, но не больше 1 500 ₽ за перевод
UPDATE commission_rules SET percent = 0.5, min_fee = 0, fixed_fee = 0, max_fee = 1500, free_monthly_amount = 100000
WHERE subscription = 'NONE' AND operation_type = 'SBP_PHONE' AND destination_type = 'SBP';
-- С карты на карту другого банка по номеру — 1%, минимум 50 ₽
UPDATE commission_rules SET percent = 1, min_fee = 50, fixed_fee = 0
WHERE subscription = 'NONE' AND operation_type = 'CARD2CARD' AND source_type = 'OUR_CARD' AND destination_type = 'OTHER_BANK_CARD';
-- Внутри МТС Банка — бесплатно; себе в другой банк — бесплатно (по закону до 30 млн ₽ в месяц)
UPDATE commission_rules SET percent = 0, min_fee = 0, fixed_fee = 0
WHERE subscription = 'NONE' AND source_type = 'OUR_CARD'
  AND ((operation_type IN ('CARD2CARD', 'OWN_TRANSFER') AND destination_type = 'OUR_CARD')
       OR (operation_type = 'OWN_TRANSFER' AND destination_type = 'OTHER_BANK_CARD'));

--changeset pricing:013-subscriptions
-- Подписки как в банке. «МТС Premium» (развлекательная) банковские тарифы не меняет — как без подписки.
-- Пакет «МТС Банк Premium» — тарифы без подписки, кроме переводов по СБП и на карты других банков.
ALTER TABLE commission_rules DROP CONSTRAINT commission_rules_subscription_check;
UPDATE commission_rules SET subscription = CASE subscription WHEN 'PREMIUM' THEN 'MTS_PREMIUM' ELSE 'BANK_PREMIUM' END
WHERE subscription IN ('PREMIUM', 'PREMIUM_PLUS');
ALTER TABLE commission_rules ADD CONSTRAINT commission_rules_subscription_check
    CHECK (subscription IN ('NONE', 'MTS_PREMIUM', 'BANK_PREMIUM'));

UPDATE commission_rules r
SET percent = n.percent, min_fee = n.min_fee, fixed_fee = n.fixed_fee, max_fee = n.max_fee,
    free_monthly_amount = n.free_monthly_amount
FROM commission_rules n
WHERE n.subscription = 'NONE' AND r.subscription <> 'NONE'
  AND n.operation_type = r.operation_type AND n.source_type = r.source_type AND n.destination_type = r.destination_type;

INSERT INTO commission_rules (operation_type, source_type, destination_type, subscription, percent, min_fee, fixed_fee,
                              max_fee, free_monthly_amount, label, sort_order)
SELECT n.operation_type, n.source_type, n.destination_type, s.subscription, n.percent, n.min_fee, n.fixed_fee,
       n.max_fee, n.free_monthly_amount, n.label, n.sort_order
FROM commission_rules n CROSS JOIN (VALUES ('MTS_PREMIUM'), ('BANK_PREMIUM')) AS s (subscription)
WHERE n.subscription = 'NONE'
  AND NOT EXISTS (SELECT 1 FROM commission_rules x WHERE x.subscription = s.subscription
                  AND x.operation_type = n.operation_type AND x.source_type = n.source_type
                  AND x.destination_type = n.destination_type);

-- «МТС Банк Premium»: по СБП бесплатно до 1 500 000 ₽ в месяц
UPDATE commission_rules SET free_monthly_amount = 1500000
WHERE subscription = 'BANK_PREMIUM' AND operation_type = 'SBP_PHONE' AND destination_type = 'SBP';
-- на карту другого банка по номеру — 1%, минимум 1 000 ₽
UPDATE commission_rules SET percent = 1, min_fee = 1000, fixed_fee = 0
WHERE subscription = 'BANK_PREMIUM' AND operation_type = 'CARD2CARD' AND source_type = 'OUR_CARD' AND destination_type = 'OTHER_BANK_CARD';
