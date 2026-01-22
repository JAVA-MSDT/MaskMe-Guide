/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.javamsdt.quarkusmasking.dto;


import com.javamsdt.maskme.api.annotation.ExcludeMaskMe;
import com.javamsdt.maskme.api.annotation.MaskMe;
import com.javamsdt.maskme.implementation.condition.AlwaysMaskMeCondition;
import com.javamsdt.maskme.implementation.condition.MaskMeOnInput;
import com.javamsdt.quarkusmasking.maskme.condition.PhoneMaskingCondition;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record UserDto(
        @MaskMe(conditions = {AlwaysMaskMeCondition.class}, maskValue = "1000")
        Long id,
        @MaskMe(conditions = {MaskMeOnInput.class}, maskValue = "{email}-{genderId}")
        String name,

        @MaskMe(conditions = {AlwaysMaskMeCondition.class}, maskValue = "")
        String email,
        @MaskMe(conditions = {AlwaysMaskMeCondition.class})
        String password,

        @MaskMe(conditions = {PhoneMaskingCondition.class})
        String phone,
        AddressDto address,
        @MaskMe(conditions = {AlwaysMaskMeCondition.class}, maskValue = "01/01/1800")
        LocalDate birthDate,
        String genderId,
        @ExcludeMaskMe
        String genderName,
        @MaskMe(conditions = {AlwaysMaskMeCondition.class}, maskValue = "")
        BigDecimal balance,
        @MaskMe(conditions = {AlwaysMaskMeCondition.class}, maskValue = "1900-01-01T00:00:00.00Z")
        Instant createdAt
) {
}
