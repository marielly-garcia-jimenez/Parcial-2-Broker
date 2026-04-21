package com.exam.broker_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RetryMessage {
    @JsonProperty("data")
    private Object data;
    
    @JsonProperty("sendEmail")
    private StepStatus sendEmail;
    
    @JsonProperty("updateRetryJobs")
    private StepStatus updateRetryJobs;

    public RetryMessage() {}

    public RetryMessage(Object data, StepStatus sendEmail, StepStatus updateRetryJobs) {
        this.data = data;
        this.sendEmail = sendEmail;
        this.updateRetryJobs = updateRetryJobs;
    }

    @Override
    public String toString() {
        return "RetryMessage{" +
                "data=" + data +
                ", sendEmail=" + sendEmail +
                ", updateRetryJobs=" + updateRetryJobs +
                '}';
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
        @JsonProperty("status")
        private String status;
        
        @JsonProperty("message")
        private String message;

        public StepStatus() {}

        public StepStatus(String status, String message) {
            this.status = status;
            this.message = message;
        }

        @Override
        public String toString() {
            return "StepStatus{" +
                    "status='" + status + '\'' +
                    ", message='" + message + '\'' +
                    '}';
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
