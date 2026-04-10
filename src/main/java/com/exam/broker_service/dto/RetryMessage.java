package com.exam.broker_service.dto;

public class RetryMessage {
    private Object data;
    private StepStatus sendEmail;
    private StepStatus updateRetryJobs;

    public RetryMessage() {}

    public RetryMessage(Object data, StepStatus sendEmail, StepStatus updateRetryJobs) {
        this.data = data;
        this.sendEmail = sendEmail;
        this.updateRetryJobs = updateRetryJobs;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public StepStatus getSendEmail() {
        return sendEmail;
    }

    public void setSendEmail(StepStatus sendEmail) {
        this.sendEmail = sendEmail;
    }

    public StepStatus getUpdateRetryJobs() {
        return updateRetryJobs;
    }

    public void setUpdateRetryJobs(StepStatus updateRetryJobs) {
        this.updateRetryJobs = updateRetryJobs;
    }

    public static class StepStatus {
        private String status;
        private String message;

        public StepStatus() {}

        public StepStatus(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
