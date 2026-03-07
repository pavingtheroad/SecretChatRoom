package com.chatroom.security;

import com.chatroom.security.component.JwtProvider;
import com.chatroom.security.entity.SecurityUser;
import com.chatroom.util.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtProvider jwtProvider;
    @Test
    void should_return_401_when_no_token() throws Exception {
        mockMvc.perform(get("/admin/test"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void should_return_401_when_invalid_token() throws Exception {
        mockMvc.perform(get("/admin/test")
                .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void should_return_401_when_expired_token() throws Exception {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        String token = jwtProvider.generateJwt(new SecurityUser("userPKId1", "123456", authorities));
        Thread.sleep(6000L);
        mockMvc.perform(get("/admin/test")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void user_should_not_access_admin_endpoint() throws Exception {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        String token = jwtProvider.generateJwt(new SecurityUser("userPKId1", "123456", authorities));

        mockMvc.perform(get("/admin/test")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
    @Test
    void admin_should_access_admin_endpoint() throws Exception {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        String token = jwtProvider.generateJwt(new SecurityUser("userPKId1", "123456", authorities));

        mockMvc.perform(get("/admin/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
    @TestConfiguration
    @RestController
    static class SecurityTestController {
        @GetMapping("/admin/test")
        public ResponseEntity<ApiResponse<Void>> adminTest() {
            return ResponseEntity.ok(new ApiResponse<>(
                    "success",
                    "admin test success",
                    null,
                    null
            ));
        }
    }
}
