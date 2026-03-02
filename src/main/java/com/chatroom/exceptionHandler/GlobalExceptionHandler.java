package com.chatroom.exceptionHandler;

import com.chatroom.room.exception.RoomAlreadyExistsException;
import com.chatroom.room.exception.RoomAuthorityException;
import com.chatroom.room.exception.RoomNotFoundException;
import com.chatroom.security.exception.JwtGenerateException;
import com.chatroom.user.exception.*;
import com.chatroom.util.ApiResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ApiResponse<Void>> handleUser(UserException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiResponse<>(
                        "BAD_REQUEST",
                        e.getMessage(),
                        null,
                        null
                )
        );
    }
    @ExceptionHandler(EmailOccupiedException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmailOccupied(EmailOccupiedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiResponse<>(
                        "CONFLICT",
                        "邮箱已存在" + e.getMessage(),
                        null,
                        null
                )
        );
    }
    @ExceptionHandler(UserCanceledException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserCanceled(UserCanceledException e) {
        return ResponseEntity.status(HttpStatus.GONE).body(
                new ApiResponse<>(
                        "NOT_FOUND",
                        "user had been canceled" + e.getMessage(),
                        null,
                        null
                )
        );
    }
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiResponse<>(
                        "NOT_FOUND",
                        e.getMessage(),
                        null,
                        null
                )
        );
    }
    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleRoomNotFound(RoomNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiResponse<>(
                        "NOT_FOUND",
                        e.getMessage(),
                        null,
                        null
                )
        );
    }
    @ExceptionHandler(AuthorityException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthority(AuthorityException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ApiResponse<>(
                        "UNAUTHORIZED",
                        "用户权限不足" + e.getMessage(),
                        null,
                        null
                )
        );
    }
    @ExceptionHandler(JwtGenerateException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwtGenerate(JwtGenerateException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ApiResponse<>(
                        "INTERNAL_SERVER_ERROR",
                        "Server could not generate token" + e.getMessage(),
                        null,
                        null
                )
        );
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(
                new ApiResponse<>(
                        "BAD_REQUEST",
                        e.getMessage(),
                        null,
                        null
                )
        );
    }
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(DuplicateKeyException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiResponse<>(
                        "CONFLICT",
                        "重复添加数据" + e.getMessage(),
                        null,
                        null
                )
        );
    }
    @ExceptionHandler(RoomAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleRoomAlreadyExists(RoomAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiResponse<>(
                        "CONFLICT",
                        "房间已存在",
                        null,
                        null
                )
        );
    }
    @ExceptionHandler(RoomAuthorityException.class)
    public ResponseEntity<ApiResponse<Void>> handleRoomAuthority(RoomAuthorityException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ApiResponse<>(
                        "UNAUTHORIZED",
                        "没有权限",
                        null,
                        null
                )
        );
    }
}
