package com.saurabh.quickbill.controller;

import com.saurabh.quickbill.io.AuthRequest;
import com.saurabh.quickbill.io.AuthResponse;
import com.saurabh.quickbill.service.UserService;
import com.saurabh.quickbill.service.impl.AppUserDetailsService;
import com.saurabh.quickbill.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AppUserDetailsService appUserDetailsService;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    //Login EndPoint

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        authenticate(request.getEmail(),request.getPassword());
        final UserDetails userDetails = appUserDetailsService.loadUserByUsername(request.getEmail());
        final String jwtToken = jwtUtil.generateToken(userDetails);
        String role = userService.getUserRole(request.getEmail());
        return new AuthResponse(request.getEmail(),jwtToken,role);

    }

    private void authenticate(String email, String password)  {
        // BadCredentialsException / DisabledException propagate up to
        // GlobalExceptionHandler, which maps them to proper HTTP responses.
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email,password));
    }

//    @PostMapping("/encode")
//    public String encodePassword(@RequestBody Map<String,String> request){
//        return passwordEncoder.encode(request.get("password"));
//    }
}
