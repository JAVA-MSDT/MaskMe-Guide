/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.javamsdt.masking.domain;

import io.github.javamsdt.maskme.api.annotation.ExcludeMaskMe;
import io.github.javamsdt.maskme.api.annotation.MaskMe;
import io.github.javamsdt.maskme.implementation.condition.AlwaysMaskMeCondition;
import io.github.javamsdt.maskme.implementation.condition.MaskMeOnInput;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private Long id;
    @MaskMe(conditions = {MaskMeOnInput.class}, maskValue = "*****")
    private String name;
    @MaskMe(conditions = {AlwaysMaskMeCondition.class})
    private String email;
    @MaskMe(conditions = {AlwaysMaskMeCondition.class})
    private String password;
    private String phone;
    @ExcludeMaskMe
    private Address address;
    @MaskMe(conditions = {AlwaysMaskMeCondition.class})
    private LocalDate birthDate;
    private Gender gender;
    @MaskMe(conditions = {AlwaysMaskMeCondition.class})
    private BigDecimal balance;
    private Instant createdAt;

}
