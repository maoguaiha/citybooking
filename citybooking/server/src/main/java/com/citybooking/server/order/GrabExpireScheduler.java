package com.citybooking.server.order;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@EnableScheduling
public class GrabExpireScheduler {

    private final DispatchService dispatchService;

    public GrabExpireScheduler(DispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void run() {
        dispatchService.expireGrabOrders();
    }
}
