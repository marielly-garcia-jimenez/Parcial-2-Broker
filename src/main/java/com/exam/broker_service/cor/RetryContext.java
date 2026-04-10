package com.exam.broker_service.cor;

import com.exam.broker_service.model.BaseRetryJob;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RetryContext {
    private BaseRetryJob job;
    private String serviceName; // product, order, payments
    private boolean success;
}
