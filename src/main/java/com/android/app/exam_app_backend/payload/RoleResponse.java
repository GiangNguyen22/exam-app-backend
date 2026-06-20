package com.android.app.exam_app_backend.payload;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RoleResponse {
    private Long id;
    private String name;
    private String description;
    private List<String> permissions;
}
