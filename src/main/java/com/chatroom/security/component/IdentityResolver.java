package com.chatroom.security.component;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class IdentityResolver {
    public static Long currentUserPKId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new AccessDeniedException("Unauthenticated");
        }
        return Long.parseLong(auth.getName());
    }
}
