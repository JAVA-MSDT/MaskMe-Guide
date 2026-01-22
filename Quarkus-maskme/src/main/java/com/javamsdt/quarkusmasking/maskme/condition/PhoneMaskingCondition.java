/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.javamsdt.quarkusmasking.maskme.condition;

import com.javamsdt.maskme.api.condition.MaskMeCondition;
import com.javamsdt.quarkusmasking.service.UserService;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Custom masking condition for Quarkus that masks phone numbers based on runtime input
 * and validation against a user database.
 * <p>
 * This condition demonstrates CDI integration in Quarkus with constructor-based
 * dependency injection. The {@code @Unremovable} annotation is CRITICAL to prevent
 * Quarkus from removing this bean during build-time optimization.
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
 * // In REST endpoint:
 * @GET
 * public List<UserDto> getUsers(@HeaderParam("Mask-Phone") String maskPhone) {
 *     return users.stream()
 *         .map(user -> MaskMeInitializer.mask(userDto, PhoneMaskingCondition.class, maskPhone))
 *         .toList();
 * }
 * }</pre>
 *
 * <p><b>Why @Unremovable?</b></p>
 * <p>
 * Quarkus performs build-time optimization and removes beans that aren't directly
 * injected anywhere. Since MaskMe looks up conditions programmatically via CDI,
 * Quarkus doesn't see them as "used" and would remove them without @Unremovable.
 * </p>
 *
 * @author Ahmed Samy
 * @see io.quarkus.arc.Unremovable
 * @since 1.0.0
 */
@ApplicationScoped
@Unremovable
public class PhoneMaskingCondition implements MaskMeCondition {

    /**
     * User service for validating phone numbers against the database.
     * Injected by Quarkus CDI via constructor.
     */
    private final UserService userService;

    /**
     * Runtime input value to compare against the field value.
     * Set via {@link #setInput(Object)} method by MaskMe library.
     */
    private String input;

    /**
     * Constructor for CDI injection.
     * <p>
     * Quarkus automatically injects the UserService when creating this bean.
     * </p>
     *
     * @param userService the user service to inject
     */
    public PhoneMaskingCondition(UserService userService) {
        this.userService = userService;
    }

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

        return input != null
                && input.equals(maskedFieldValue)
                && anyUserPhoneMatched;
    }

    /**
     * Sets the runtime input value for this condition.
     * <p>
     * This method is called by the MaskMe library before evaluating the condition.
     * The input is typically provided via HTTP headers in REST endpoints.
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
