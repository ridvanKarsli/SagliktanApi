package com.ridvankarsli.sagliktanapi.dto.response;

public record AdminStatsResponse(
        long totalUsers,
        long totalPosts,
        long totalComments,
        long pendingReports
) {
}
