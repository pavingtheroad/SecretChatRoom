package com.chatroom.exceptionHandler;

import com.chatroom.security.exception.JwtGenerateException;
import com.chatroom.user.exception.AuthorityException;
import com.chatroom.user.exception.EmailOccupiedException;
import com.chatroom.user.exception.UserException;
import com.chatroom.user.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserException.class)
    public ResponseEntity<String> handleUser(UserException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
    @ExceptionHandler(EmailOccupiedException.class)
    public ResponseEntity<String> handleEmailOccupied(EmailOccupiedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(AuthorityException.class)
    public ResponseEntity<String> handleAuthority(AuthorityException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }
    @ExceptionHandler(JwtGenerateException.class)
    public ResponseEntity<String> handleJwtGenerate(JwtGenerateException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
}
