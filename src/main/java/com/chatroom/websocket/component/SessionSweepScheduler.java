package com.chatroom.websocket.component;

import com.chatroom.websocket.domain.SessionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class SessionSweepScheduler {
    private final SessionManager sessionManager;
    @Value("${session.expire-time}")
    private long sessionExpireTime;
    public SessionSweepScheduler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }
    @Scheduled(fixedDelay = 60000)
    public void sweepSession(){
        long now = System.currentTimeMillis();
        for (SessionContext ctx : sessionManager.getAllSessionContext()){
            if (now - ctx.getActiveTime() > sessionExpireTime){
                sessionManager.closeSession(ctx.getSessionId());
            }
        }
    }
}
