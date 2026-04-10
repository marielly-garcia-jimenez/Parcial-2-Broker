package com.exam.broker_service.cor;

public interface RetryHandler {
    void handle(RetryContext context);
    void setNext(RetryHandler next);
}
