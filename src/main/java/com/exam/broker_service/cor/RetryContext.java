package com.exam.broker_service.cor;

import com.exam.broker_service.model.BaseRetryJob;

public class RetryContext {
    private BaseRetryJob job;
    private String serviceName; // product, order, payments
    private boolean success;

    public RetryContext() {}

    public RetryContext(BaseRetryJob job, String serviceName, boolean success) {
        this.job = job;
        this.serviceName = serviceName;
        this.success = success;
    }

    public static RetryContextBuilder builder() {
        return new RetryContextBuilder();
    }

    public BaseRetryJob getJob() {
        return job;
    }

    public void setJob(BaseRetryJob job) {
        this.job = job;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public static class RetryContextBuilder {
        private BaseRetryJob job;
        private String serviceName;
        private boolean success;

        public RetryContextBuilder job(BaseRetryJob job) {
            this.job = job;
            return this;
        }

        public RetryContextBuilder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public RetryContextBuilder success(boolean success) {
            this.success = success;
            return this;
        }

        public RetryContext build() {
            return new RetryContext(job, serviceName, success);
        }
    }
}
