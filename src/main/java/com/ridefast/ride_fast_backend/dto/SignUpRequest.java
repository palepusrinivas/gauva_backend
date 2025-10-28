package com.ridefast.ride_fast_backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignUpRequest {

  @Email(message = "Invalid email address")
  private String email;

  @NotEmpty(message = "full name is required")
  private String fullName;

  @NotEmpty(message = "password is required")
  private String password;

  @NotEmpty(message = "phone is required")
  @Size(min = 10, message = "Invalid phone number")
  @JsonAlias({"mobile"})
  private String phone;

//  @JsonAlias({"language", "lang"})
//  private String currentLanguageKey;


    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        if (phone != null) {
            this.phone = phone.replace("+91", "").replaceAll("\\s+", "");
        } else {
            this.phone = null;
        }
    }
}
