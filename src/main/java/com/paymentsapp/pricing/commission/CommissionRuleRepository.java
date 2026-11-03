package com.paymentsapp.pricing.commission;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule, Integer> {

    Optional<CommissionRule> findByOperationTypeAndSubscription(OperationType operationType, Subscription subscription);
}
