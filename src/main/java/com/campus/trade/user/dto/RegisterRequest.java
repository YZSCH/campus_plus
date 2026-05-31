package com.campus.trade.user.dto;

import lombok.Data;
import javax.validation.constraints.*;

@Data
public class RegisterRequest {
    @NotBlank @Pattern(regexp = "^\\d{11}$", message = "手机号格式不正确")
    private String phone;
    @NotBlank private String verificationCode;
    @NotBlank @Size(min = 2, max = 50) private String username;
    @NotBlank @Size(min = 8, max = 20)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,20}$", message = "密码须包含字母和数字")
    private String password;
    @NotBlank private String confirmPassword;
    @NotBlank @Size(max = 20) private String studentId;
    @NotBlank @Size(max = 50) private String realName;
    @NotBlank private String department;
    private String className;
    private String avatarBase64;
}
