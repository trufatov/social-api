package com.waracle.social_api.controller;

import com.waracle.social_api.dto.response.MessageResponse;
import com.waracle.social_api.dto.response.UserSummaryResponse;
import com.waracle.social_api.service.admin.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users/pending")
    @ResponseStatus(HttpStatus.OK)
    public List<UserSummaryResponse> getPendingUsers() {
        return adminService.getPendingUsers();
    }

    @PutMapping("/users/{id}/approve")
    @ResponseStatus(HttpStatus.OK)
    public MessageResponse approveUser(@PathVariable Long id) {
        adminService.approveUser(id);
        return new MessageResponse("User approved successfully.");
    }
}
