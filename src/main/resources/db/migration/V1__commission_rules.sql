-- Комиссия зависит от вида операции и подписки пользователя
CREATE TABLE commission_rules (
    id                SERIAL PRIMARY KEY,
    operation_type    VARCHAR(30)  NOT NULL
        CHECK (operation_type IN ('PHONE_TRANSFER', 'CARD_TRANSFER', 'OWN_TRANSFER')),
    subscription      VARCHAR(20)  NOT NULL
        CHECK (subscription IN ('NONE', 'PREMIUM', 'PREMIUM_PLUS')),
    percent           NUMERIC(5, 2) NOT NULL CHECK (percent >= 0),
    UNIQUE (operation_type, subscription)
);

INSERT INTO commission_rules (operation_type, subscription, percent) VALUES
    ('PHONE_TRANSFER', 'NONE',         1.00),
    ('PHONE_TRANSFER', 'PREMIUM',      0.50),
    ('PHONE_TRANSFER', 'PREMIUM_PLUS', 0.00),
    ('CARD_TRANSFER',  'NONE',         1.50),
    ('CARD_TRANSFER',  'PREMIUM',      1.00),
    ('CARD_TRANSFER',  'PREMIUM_PLUS', 0.50),
    ('OWN_TRANSFER',   'NONE',         0.00),
    ('OWN_TRANSFER',   'PREMIUM',      0.00),
    ('OWN_TRANSFER',   'PREMIUM_PLUS', 0.00);
