package com.android.app.exam_app_backend.payload;

import com.android.app.exam_app_backend.entity.enums.UserStatus;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class UserUpdateRequest {

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

    private UserStatus status;

    private List<String> roles;
}
