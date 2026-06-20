package com.android.app.exam_app_backend.payload;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class RolePermissionsUpdateRequest {

    @NotNull
    private List<String> permissions;
}
