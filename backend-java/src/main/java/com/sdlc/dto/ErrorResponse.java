package com.sdlc.dto;

public class ErrorResponse {
    private ErrorDetail error;
    
    public ErrorResponse() {}
    
    public ErrorResponse(ErrorDetail error) {
        this.error = error;
    }
    
    public ErrorDetail getError() { return error; }
    public void setError(ErrorDetail error) { this.error = error; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private ErrorDetail error;
        
        public Builder error(ErrorDetail error) {
            this.error = error;
            return this;
        }
        
        public ErrorResponse build() {
            return new ErrorResponse(error);
        }
    }
    
    public static class ErrorDetail {
        private String code;
        private String message;
        private String details;
        
        public ErrorDetail() {}
        
        public ErrorDetail(String code, String message, String details) {
            this.code = code;
            this.message = message;
            this.details = details;
        }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String code;
            private String message;
            private String details;
            
            public Builder code(String code) {
                this.code = code;
                return this;
            }
            
            public Builder message(String message) {
                this.message = message;
                return this;
            }
            
            public Builder details(String details) {
                this.details = details;
                return this;
            }
            
            public ErrorDetail build() {
                return new ErrorDetail(code, message, details);
            }
        }
    }
    
    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .build())
                .build();
    }
    
    public static ErrorResponse of(String code, String message, String details) {
        return ErrorResponse.builder()
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .details(details)
                        .build())
                .build();
    }
}
