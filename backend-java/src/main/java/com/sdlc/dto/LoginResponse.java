package com.sdlc.dto;

public class LoginResponse {
    private String token;
    private UserDto user;
    
    public LoginResponse() {}
    
    public LoginResponse(String token, UserDto user) {
        this.token = token;
        this.user = user;
    }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String token;
        private UserDto user;
        
        public Builder token(String token) {
            this.token = token;
            return this;
        }
        
        public Builder user(UserDto user) {
            this.user = user;
            return this;
        }
        
        public LoginResponse build() {
            return new LoginResponse(token, user);
        }
    }
    
    public static class UserDto {
        private String id;
        private String email;
        private String name;
        
        public UserDto() {}
        
        public UserDto(String id, String email, String name) {
            this.id = id;
            this.email = email;
            this.name = name;
        }
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private String id;
            private String email;
            private String name;
            
            public Builder id(String id) {
                this.id = id;
                return this;
            }
            
            public Builder email(String email) {
                this.email = email;
                return this;
            }
            
            public Builder name(String name) {
                this.name = name;
                return this;
            }
            
            public UserDto build() {
                return new UserDto(id, email, name);
            }
        }
    }
}
