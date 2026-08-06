package com.waracle.social_api.service.admin;

import com.waracle.social_api.dto.response.UserSummaryResponse;

import java.util.List;

public interface AdminService {
    void approveUser(Long id);

    List<UserSummaryResponse> getPendingUsers();
}
