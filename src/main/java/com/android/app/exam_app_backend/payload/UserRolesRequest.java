package com.android.app.exam_app_backend.payload;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Getter
@Setter
public class UserRolesRequest {

    @NotEmpty
    private List<String> roles;
}
