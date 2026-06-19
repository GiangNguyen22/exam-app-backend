package com.android.app.exam_app_backend.payload;

import com.android.app.exam_app_backend.entity.enums.UserStatus;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class UserCreateRequest {

    @NotBlank
    @Size(max = 50)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @Size(max = 255)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 20)
    private String studentId;

    @Size(max = 20)
    private String employeeCode;

    private UserStatus status = UserStatus.ACTIVE;

    private List<String> roles;
}
