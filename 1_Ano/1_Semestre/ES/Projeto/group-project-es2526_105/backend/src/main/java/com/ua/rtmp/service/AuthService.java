package com.ua.rtmp.service;

import com.ua.rtmp.dto.response.UserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public UserInfoResponse getUserInfo(Authentication authentication) {
        log.info("getUserInfo started");
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("getUserInfo failed: user not authenticated");
            throw new IllegalArgumentException("User not authenticated");
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        
        UserInfoResponse userInfo = new UserInfoResponse();
        userInfo.setSub(jwt.getClaimAsString("sub"));
        userInfo.setEmail(jwt.getClaimAsString("email"));
        userInfo.setName(jwt.getClaimAsString("name"));
        userInfo.setPreferredUsername(jwt.getClaimAsString("preferred_username"));
        
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        userInfo.setRoles(roles);

        log.info("getUserInfo completed successfully: userId={}, username={}, rolesCount={}",
                userInfo.getSub(), userInfo.getPreferredUsername(), roles.size());
        log.debug("User roles: {}", roles);
        return userInfo;
    }

    public String getAuthenticatedUsername(Authentication authentication) {
        log.debug("getAuthenticatedUsername started");
        
        if (authentication == null) {
            log.error("getAuthenticatedUsername failed: not authenticated");
            throw new IllegalArgumentException("Not authenticated");
        }
        
        String username = authentication.getName();
        log.debug("getAuthenticatedUsername completed successfully: username={}", username);
        return username;
    }

    public boolean isAuthenticated(Authentication authentication) {
        boolean authenticated = authentication != null && authentication.isAuthenticated();
        log.debug("isAuthenticated: result={}", authenticated);
        return authenticated;
    }
}
