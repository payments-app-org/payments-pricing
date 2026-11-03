--liquibase formatted sql

--changeset pricing:001-commission-rules
-- Комиссия зависит от вида операции и подписки пользователя. id правила — это тариф, он попадает в котировку и платёж.
CREATE TABLE commission_rules (
    id                SERIAL PRIMARY KEY,
    operation_type    VARCHAR(30)   NOT NULL
        CHECK (operation_type IN ('SBP_PHONE', 'CARD2CARD', 'OWN_TRANSFER')),
    subscription      VARCHAR(20)   NOT NULL
        CHECK (subscription IN ('NONE', 'PREMIUM', 'PREMIUM_PLUS')),
    percent           NUMERIC(5, 2) NOT NULL CHECK (percent >= 0),
    UNIQUE (operation_type, subscription)
);

INSERT INTO commission_rules (operation_type, subscription, percent) VALUES
    ('SBP_PHONE',    'NONE',         1.00),
    ('SBP_PHONE',    'PREMIUM',      0.50),
    ('SBP_PHONE',    'PREMIUM_PLUS', 0.00),
    ('CARD2CARD',    'NONE',         1.50),
    ('CARD2CARD',    'PREMIUM',      1.00),
    ('CARD2CARD',    'PREMIUM_PLUS', 0.50),
    ('OWN_TRANSFER', 'NONE',         0.00),
    ('OWN_TRANSFER', 'PREMIUM',      0.00),
    ('OWN_TRANSFER', 'PREMIUM_PLUS', 0.00);
