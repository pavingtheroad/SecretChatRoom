package com.chatroom.facade;

import com.chatroom.room.exception.RoomNotFoundException;
import com.chatroom.room.service.RoomOwnerService;
import com.chatroom.room.service.RoomServiceImpl;
import com.chatroom.user.exception.AuthorityException;
import com.chatroom.user.exception.UserNotFoundException;
import com.chatroom.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;


@Service
public class UserLifecycleService {
    private static final Logger log = LoggerFactory.getLogger(UserLifecycleService.class);
    private final UserService US;
    private final RoomServiceImpl RS;
    private final RoomOwnerService ROS;
    public UserLifecycleService(UserService userService, RoomServiceImpl roomServiceImpl, RoomOwnerService roomOwnerService){
        this.US = userService;
        this.RS = roomServiceImpl;
        this.ROS = roomOwnerService;
    }
    public void cancelUser(String targetUserId, String operatorUserId) throws UserNotFoundException, AuthorityException{
        Long targetUserPKId = US.cancelUser(targetUserId, operatorUserId);

        Set<String> roomIds = RS.joinedRoomsId(targetUserPKId.toString());
        for (String roomId : roomIds) {
            try {
                ROS.removeUserFromRoom(roomId, targetUserPKId.toString());
            } catch (RoomNotFoundException | UserNotFoundException e) {
                log.warn("Room cleanup skipped: roomId={}, userId={}", roomId, targetUserPKId);
            } catch (Exception e) {
                log.error("Room cleanup failed: roomId={}, userId={}", roomId, targetUserPKId, e);
            }
        }
    }
}
