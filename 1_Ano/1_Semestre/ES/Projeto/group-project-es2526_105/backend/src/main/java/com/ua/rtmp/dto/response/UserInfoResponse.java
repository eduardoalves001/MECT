package com.ua.rtmp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private String sub;
    private String email;
    private String name;
    private String preferredUsername;
    private List<String> roles;
}
