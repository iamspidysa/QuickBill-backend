package com.saurabh.quickbill.controller;

import com.saurabh.quickbill.io.UserRequest;
import com.saurabh.quickbill.io.UserResponse;
import com.saurabh.quickbill.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
// This Controller is only for Admin
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerUser(@Valid @RequestBody UserRequest request){
            return userService.createUser(request);
    }

    @GetMapping("/users")
    public List<UserResponse> readUsers(){
        return userService.readUsers();
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable String id){
            userService.deleteUser(id);

    }
}
