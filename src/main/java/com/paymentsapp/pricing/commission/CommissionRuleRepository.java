package com.paymentsapp.pricing.commission;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;

public interface CommissionRuleRepository extends JpaRepository<CommissionRule, Integer> {

    // Кэш запросов Hibernate: тарифы меняются только миграциями
    @QueryHints(@QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "true"))
    Optional<CommissionRule> findByOperationTypeAndSourceTypeAndDestinationTypeAndSubscription(
            OperationType operationType, Party sourceType, Party destinationType, Subscription subscription);

    @QueryHints(@QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "true"))
    List<CommissionRule> findBySubscriptionOrderBySortOrder(Subscription subscription);
}
