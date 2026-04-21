package com.exam.broker_service.cor;

import com.exam.broker_service.model.BaseRetryJob;
import java.util.HashMap;
import java.util.Map;

public class RetryContext {
    private BaseRetryJob job;
    private String serviceName; // product, order, payments
    private boolean success;
    private Map<String, String> stepResults = new HashMap<>();

    public RetryContext() {}

    public RetryContext(BaseRetryJob job, String serviceName, boolean success, Map<String, String> stepResults) {
        this.job = job;
        this.serviceName = serviceName;
        this.success = success;
        this.stepResults = stepResults != null ? stepResults : new HashMap<>();
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

    public Map<String, String> getStepResults() {
        return stepResults;
    }

    public void addStepResult(String step, String status) {
        this.stepResults.put(step, status);
    }

    public static class RetryContextBuilder {
        private BaseRetryJob job;
        private String serviceName;
        private boolean success;
        private Map<String, String> stepResults = new HashMap<>();

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

        public RetryContextBuilder stepResults(Map<String, String> stepResults) {
            this.stepResults = stepResults;
            return this;
        }

        public RetryContext build() {
            return new RetryContext(job, serviceName, success, stepResults);
        }
    }
}
