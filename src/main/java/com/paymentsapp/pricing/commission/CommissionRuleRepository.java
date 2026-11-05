package com.paymentsapp.pricing.commission;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule, Integer> {

    Optional<CommissionRule> findByOperationTypeAndSourceTypeAndDestinationTypeAndSubscription(
            OperationType operationType, Party sourceType, Party destinationType, Subscription subscription);

    List<CommissionRule> findBySubscriptionOrderBySortOrder(Subscription subscription);
}
