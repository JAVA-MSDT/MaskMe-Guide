/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.javamsdt.masking.dto;


import io.github.javamsdt.maskme.api.annotation.MaskMe;
import io.github.javamsdt.maskme.implementation.condition.AlwaysMaskMeCondition;
import io.github.javamsdt.maskme.implementation.condition.MaskMeOnInput;

public record AddressDto(
        Long id,
        String street,
        String building,
        @MaskMe(conditions = {AlwaysMaskMeCondition.class})
        String city,
        String state,
        @MaskMe(conditions = {MaskMeOnInput.class}, maskValue = "[ZIP_MASKED]")
        String zipCode,
        String country,
        GeoLocationDto geoLocation
) {
}
