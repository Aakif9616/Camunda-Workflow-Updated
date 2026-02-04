package com.dynamicworkflow.service;

import com.dynamicworkflow.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    // In-memory user storage
    private final Map<String, User> userStore = new ConcurrentHashMap<>();
    private final Map<String, String> usernameToUserIdMap = new ConcurrentHashMap<>();
    
    public UserService() {
        // Initialize default users
        initializeDefaultUsers();
    }
    
    private void initializeDefaultUsers() {
        // Create default HR user
        User hrUser = new User("hr", "hr123", "hr@company.com", "HR", "Manager", "HR");
        userStore.put(hrUser.getUserId(), hrUser);
        usernameToUserIdMap.put(hrUser.getUsername(), hrUser.getUserId());
        
        // Create default Team Lead user
        User tlUser = new User("teamlead", "tl123", "teamlead@company.com", "Team", "Lead", "TEAM_LEAD");
        userStore.put(tlUser.getUserId(), tlUser);
        usernameToUserIdMap.put(tlUser.getUsername(), tlUser.getUserId());
        
        // Create default Project Manager user
        User pmUser = new User("projectmanager", "pm123", "pm@company.com", "Project", "Manager", "PROJECT_MANAGER");
        userStore.put(pmUser.getUserId(), pmUser);
        usernameToUserIdMap.put(pmUser.getUsername(), pmUser.getUserId());
        
        // Create default Head HR user
        User headHRUser = new User("headhr", "hhr123", "headhr@company.com", "Head HR", "Director", "HEAD_HR");
        userStore.put(headHRUser.getUserId(), headHRUser);
        usernameToUserIdMap.put(headHRUser.getUsername(), headHRUser.getUserId());
        
        // Create default Company Manager user
        User companyManagerUser = new User("companymanager", "cm123", "companymanager@company.com", "Company", "Manager", "COMPANY_MANAGER");
        userStore.put(companyManagerUser.getUserId(), companyManagerUser);
        usernameToUserIdMap.put(companyManagerUser.getUsername(), companyManagerUser.getUserId());
        
        logger.info("Initialized {} default users", userStore.size());
    }
    
    public User registerUser(String username, String password, String email, String firstName, String lastName, String role) {
        // Check if username already exists
        if (usernameToUserIdMap.containsKey(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }
        
        // Check if email already exists
        boolean emailExists = userStore.values().stream()
            .anyMatch(user -> user.getEmail().equalsIgnoreCase(email));
        if (emailExists) {
            throw new RuntimeException("Email already exists: " + email);
        }
        
        // Create new user
        User newUser = new User(username, password, email, firstName, lastName, role);
        userStore.put(newUser.getUserId(), newUser);
        usernameToUserIdMap.put(username, newUser.getUserId());
        
        logger.info("Registered new user: {} with role: {}", username, role);
        return newUser;
    }
    
    public Optional<User> authenticateUser(String username, String password) {
        String userId = usernameToUserIdMap.get(username);
        if (userId == null) {
            return Optional.empty();
        }
        
        User user = userStore.get(userId);
        if (user != null && user.getPassword().equals(password) && user.isActive()) {
            user.setLastLoginAt(LocalDateTime.now());
            logger.info("User authenticated successfully: {}", username);
            return Optional.of(user);
        }
        
        logger.warn("Authentication failed for user: {}", username);
        return Optional.empty();
    }
    
    public Optional<User> getUserById(String userId) {
        return Optional.ofNullable(userStore.get(userId));
    }
    
    public Optional<User> getUserByUsername(String username) {
        String userId = usernameToUserIdMap.get(username);
        return userId != null ? Optional.ofNullable(userStore.get(userId)) : Optional.empty();
    }
    
    public boolean isUsernameAvailable(String username) {
        return !usernameToUserIdMap.containsKey(username);
    }
    
    public boolean isEmailAvailable(String email) {
        return userStore.values().stream()
            .noneMatch(user -> user.getEmail().equalsIgnoreCase(email));
    }
    
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userStore.size());
        
        Map<String, Long> roleStats = new HashMap<>();
        userStore.values().forEach(user -> {
            roleStats.merge(user.getRole(), 1L, Long::sum);
        });
        stats.put("roleStats", roleStats);
        
        return stats;
    }
    
    public void updateUserLastLogin(String userId) {
        User user = userStore.get(userId);
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
        }
    }
}