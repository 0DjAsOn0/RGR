package com.rgr.messanger.service.impl;

import com.rgr.messanger.entity.user.Role;
import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.exception.ResourceNotFoundException;
import com.rgr.messanger.repository.UserRepo;
import com.rgr.messanger.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public User update(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepo.update(user);
        return user;
    }

    @Override
    @Transactional
    public User create(User user) {
        if(userRepo.findByUsername(user.getUsername()).isPresent()){
            throw new IllegalStateException("User already exist");
        }
        if(!user.getPassword().equals(user.getPasswordConfirmation())){
            throw new IllegalStateException("Password and password confirmation do not match");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepo.create(user);
        Set<Role> roles = Set.of(Role.ROLE_USER);
        user.setRoles(roles);
        roles.forEach(role -> userRepo.insertUserRole(user.getId(), role));
        return user;
    }

    @Override
    @Transactional
    public boolean isMessageOwner(Long userId, Long messageId) {

        return userRepo.isMessageOwner(userId, messageId);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        userRepo.delete(id);
    }
}
