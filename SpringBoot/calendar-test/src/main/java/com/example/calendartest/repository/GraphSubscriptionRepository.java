package com.example.calendartest.repository;

import com.example.calendartest.entity.GraphSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GraphSubscriptionRepository extends JpaRepository<GraphSubscription, Long> {
}