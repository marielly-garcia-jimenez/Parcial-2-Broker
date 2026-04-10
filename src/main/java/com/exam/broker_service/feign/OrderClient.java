package com.exam.broker_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "order-service")
public interface OrderClient {
    @PostMapping("/ordenes/retry")
    Object retrySave(@RequestBody Object order);
}
