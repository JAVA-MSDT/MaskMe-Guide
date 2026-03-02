package com.javamsdt.hardcoded.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record UserDto(
    Long id,
    String name,
    String email,
    String password,
    String phone,
    AddressDto address,
    LocalDate birthDate,
    String genderId,
    String genderName,
    BigDecimal balance,
    Instant createdAt
) {
    
    public UserDto mask(String maskInput, String phoneInput) {
        return new UserDto(
            1000L,
            maskInput != null && maskInput.equals("maskMe") ? email + "-" + genderId : name,
            "",
            "************",
            phoneInput != null && phoneInput.equals(phone) ? "***" : phone,
            address != null ? address.mask(maskInput) : null,
            LocalDate.of(1800, 1, 1),
            genderId,
            genderName,
            BigDecimal.ZERO,
            Instant.parse("1900-01-01T00:00:00.00Z")
        );
    }
}
