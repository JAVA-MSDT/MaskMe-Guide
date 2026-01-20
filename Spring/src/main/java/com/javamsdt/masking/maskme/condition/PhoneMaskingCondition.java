/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.javamsdt.masking.maskme.condition;

import com.javamsdt.masking.service.UserService;
import com.javamsdt.maskme.api.condition.MaskMeCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PhoneMaskingCondition implements MaskMeCondition {

    private final UserService userService;
    private String input;

    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {

        boolean anyUserPhoneMatched = userService.findUsers().stream()
                .anyMatch(user -> user.getPhone().equals(maskedFieldValue));

        // It will return true for only specific phone number, the rest in the collections will not be masked.
        return input != null
                && input.equals(maskedFieldValue)
                && anyUserPhoneMatched;
    }

    @Override
    public void setInput(Object input) {
        if (input instanceof String) {
            this.input = (String) input;
        }
    }
}
