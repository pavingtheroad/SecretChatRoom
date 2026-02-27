package com.chatroom.user.controller;

import com.chatroom.security.lifecycle.SecurityConfig;
import com.chatroom.user.domain.UserStatus;
import com.chatroom.user.dto.UserInfoForAdmin;
import com.chatroom.user.exception.AuthorityException;
import com.chatroom.user.exception.UserCanceledException;
import com.chatroom.user.exception.UserNotFoundException;
import com.chatroom.user.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminUserController.class

)
@Import(AdminUserControllerTest.TestSecurityConfig.class)
public class AdminUserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private AdminUserService adminUserService;
    @Test
    void shouldReturn403_whenUserForbidden_insertRoleToUser() throws Exception {
        String userId = "userId";
        String roleCode = "ADMIN";
        mockMvc.perform(put("/admin/role")
                        .param("userId", userId)
                        .param("roleCode", roleCode)
                        .with(csrf())
                        .with(user("userId").roles("USER")))
                .andExpect(status().isForbidden());
    }
    @Test
    void shouldReturn404_whenUserNotFound_insertRoleToUser() throws Exception {
        String userId = "userId";
        String roleCode = "ADMIN";
        doThrow(UserNotFoundException.class)
                .when(adminUserService)
                        .insertRoleToUser(userId, roleCode);
        mockMvc.perform(put("/admin/role")
                        .param("userId", userId)
                        .param("roleCode", roleCode)
                        .with(csrf())
                        .with(user("userId").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
    @Test
    void shouldReturn409_whenRoleAlreadyExist_insertRoleToUser() throws Exception {
        String userId = "userId";
        String roleCode = "ADMIN";
        doThrow(DuplicateKeyException.class)
                .when(adminUserService)
                        .insertRoleToUser(userId, roleCode);
        mockMvc.perform(put("/admin/role")
                        .param("userId", userId)
                        .param("roleCode", roleCode)
                        .with(csrf())
                        .with(user("userId").roles("ADMIN")))
                .andExpect(status().isConflict());
    }
    @Test
    void shouldReturn200_whenSuccess_insertRoleToUser() throws Exception {
        String userId = "userId";
        String roleCode = "ADMIN";
        mockMvc.perform(put("/admin/role")
                    .param("userId", userId)
                    .param("roleCode", roleCode)
                        .with(csrf())
                        .with(user("userId").roles("ADMIN")))
                .andExpect(status().isOk());
    }
    @Test
    void shouldReturn401_whenUserUnauthorized_banUser() throws Exception {
        String userId = "userId";
        doThrow(AuthorityException.class)
                .when(adminUserService).banUser(userId, "adminId");
        mockMvc.perform(put("/admin/banned/{userId}", userId)
                .with(csrf())
                .with(user("adminId").roles("ADMIN")))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void shouldReturn404_whenUserNotFound_banUser() throws Exception {
        String userId = "userId";
        doThrow(UserNotFoundException.class)
                .when(adminUserService).banUser(userId, "adminId");
        mockMvc.perform(put("/admin/banned/{userId}", userId)
                .with(csrf())
                .with(user("adminId").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
    @Test
    void shouldReturn409_whenUserAlreadyBanned_banUser() throws Exception {
        String userId = "userId";
        doThrow(DuplicateKeyException.class)
                .when(adminUserService).banUser(userId, "adminId");
        mockMvc.perform(put("/admin/banned/{userId}", userId)
                .with(csrf())
                .with(user("adminId").roles("ADMIN")))
                .andExpect(status().isConflict());
    }
    @Test
    void shouldReturn200_whenSuccess_banUser() throws Exception {
        String userId = "userId";
        mockMvc.perform(put("/admin/banned/{userId}", userId)
                .with(csrf())
                .with(user("adminId").roles("ADMIN")))
                .andExpect(status().isOk());
    }
    @Test
    void shouldReturn404_whenUserNotFound_getUserByUserId() throws Exception {
        String userId = "userId";
        doThrow(UserNotFoundException.class)
                .when(adminUserService).getUserInfo(userId);
        mockMvc.perform(get("/admin/user-profile/{userId}", userId)
                .with(user("adminId").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
    @Test
    void shouldReturn410_whenUserWasBanned_getUserByUserId() throws Exception {
        String userId = "userId";
        doThrow(UserCanceledException.class)
                .when(adminUserService).getUserInfo(userId);
        mockMvc.perform(get("/admin/user-profile/{userId}", userId)
                .with(user("adminId").roles("ADMIN")))
                .andExpect(status().isGone());
    }
    @Test
    void shouldReturnUserInfo_whenSuccess_getUserByUserId() throws Exception {
        String userId = "userId";
        UserInfoForAdmin userInfo = new UserInfoForAdmin(userId, "userName", null, UserStatus.ACTIVE, List.of("ADMIN", "USER"));
        when(adminUserService.getUserInfo(userId))
                .thenReturn(userInfo);
        mockMvc.perform(get("/admin/user-profile/{userId}", userId)
                .with(user("adminId").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.roleCode[1]").value("USER"));
    }
    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated()
                    );
            return http.build();
        }
    }
}
