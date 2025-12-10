package com.executor.server.service;

import com.executor.entity.Authority;
import com.executor.entity.User;
import com.executor.server.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserManagingService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserManagingService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(String name, String password, Authority.USER_ROLES role) {
        User user = new User();
        user.setUsername(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);

        user.addAuthority(role);

        userRepo.save(user);
    }
}
