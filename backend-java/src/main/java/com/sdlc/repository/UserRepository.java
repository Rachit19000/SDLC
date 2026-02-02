package com.sdlc.repository;

import com.sdlc.model.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    
    private final List<User> mockUsers = new ArrayList<>();
    
    public UserRepository() {
        // Initialize mock users (In production, this would be a database)
        mockUsers.add(new User("user_1", "rachitjainemail@gmail.com", "password123", "Rachit Jain"));
        mockUsers.add(new User("user_2", "test@example.com", "test123", "Test User"));
        mockUsers.add(new User("user_3", "admin@example.com", "admin123", "Admin User"));
    }
    
    public Optional<User> findByEmailAndPassword(String email, String password) {
        return mockUsers.stream()
                .filter(user -> user.getEmail().equals(email) && user.getPassword().equals(password))
                .findFirst();
    }
    
    public Optional<User> findByEmail(String email) {
        return mockUsers.stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst();
    }
    
    public Optional<User> findById(String id) {
        return mockUsers.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst();
    }
}
