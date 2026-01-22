/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.javamsdt.javamasking.maskme.condition;

import com.javamsdt.javamasking.service.UserService;
import com.javamsdt.maskme.api.condition.MaskMeCondition;
import lombok.RequiredArgsConstructor;

/**
 * Custom masking condition that masks phone numbers based on runtime input
 * and validation against a user database.
 * <p>
 * This condition demonstrates how to create custom masking logic with
 * external dependencies (UserService) in a pure Java environment.
 * </p>
 *
 * <p><b>Masking Logic:</b></p>
 * <ul>
 *   <li>Checks if the input matches the field value</li>
 *   <li>Validates if the phone number exists in the user database</li>
 *   <li>Only masks if both conditions are true</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * @MaskMe(conditions = {PhoneMaskingCondition.class}, maskValue = "****")
 * String phone;
 *
 * // In code:
 * UserDto masked = MaskMeInitializer.mask(userDto, PhoneMaskingCondition.class, "01000000000");
 * }</pre>
 *
 * @author Ahmed Samy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class PhoneMaskingCondition implements MaskMeCondition {

    /**
     * User service for validating phone numbers against the database.
     */
    private final UserService userService;

    /**
     * Runtime input value to compare against the field value.
     * Set via {@link #setInput(Object)} method.
     */
    private String input;

    /**
     * Determines whether the field should be masked based on:
     * <ol>
     *   <li>Input matches the field value</li>
     *   <li>Phone number exists in the user database</li>
     * </ol>
     *
     * @param maskedFieldValue            the current value of the field being evaluated
     * @param objectContainingMaskedField the object containing the field
     * @return true if the field should be masked, false otherwise
     */
    @Override
    public boolean shouldMask(Object maskedFieldValue, Object objectContainingMaskedField) {

        boolean anyUserPhoneMatched = userService.findUsers().stream()
                .anyMatch(user -> user.getPhone().equals(maskedFieldValue));

        // It will return true for only specific phone number, the rest in the collections will not be masked.
        return input != null
                && input.equals(maskedFieldValue)
                && anyUserPhoneMatched;
    }

    /**
     * Sets the runtime input value for this condition.
     * <p>
     * This method is called by the MaskMe library before evaluating the condition.
     * The input is typically provided when calling MaskMeInitializer.mask().
     * </p>
     *
     * @param input the input value (expected to be a String phone number)
     */
    @Override
    public void setInput(Object input) {
        if (input instanceof String inputValue) {
            this.input = inputValue;
        }
    }
}
