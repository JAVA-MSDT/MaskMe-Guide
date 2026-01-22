/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.javamsdt.masking.controller;

import com.javamsdt.masking.domain.User;
import com.javamsdt.masking.dto.UserDto;
import com.javamsdt.masking.mapper.UserMapper;
import com.javamsdt.masking.maskme.condition.PhoneMaskingCondition;
import com.javamsdt.masking.service.UserService;
import com.javamsdt.maskme.MaskMeInitializer;
import com.javamsdt.maskme.implementation.condition.MaskMeOnInput;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller demonstrating MaskMe library usage with Spring Framework.
 * <p>
 * This controller provides endpoints showcasing different masking scenarios:
 * <ul>
 *   <li>Unmasked data retrieval</li>
 *   <li>Conditional masking with MaskMeOnInput</li>
 *   <li>Always-masked domain entities</li>
 *   <li>Multiple conditions (MaskMeOnInput + PhoneMaskingCondition)</li>
 * </ul>
 * </p>
 *
 * <p><b>Base URL:</b> {@code /users}</p>
 *
 * @author Ahmed Samy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Retrieves a user by ID without any masking applied.
     * <p>
     * This endpoint returns the original data as-is, demonstrating
     * the baseline before masking is applied.
     * </p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * GET /users/1
     * }</pre>
     *
     * @param id the user ID
     * @return UserDto with original unmasked data
     */
    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable final Long id) {
        return userMapper.toDto(userService.findUserById(id));
    }

    /**
     * Retrieves a user by ID with conditional masking applied.
     * <p>
     * Uses {@link MaskMeOnInput} condition - fields are masked only when
     * the Mask-Input header value matches the condition's expected value.
     * </p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * GET /users/masked/1
     * Headers: Mask-Input: maskMe
     * }</pre>
     *
     * @param id        the user ID
     * @param maskInput the input value for MaskMeOnInput condition (from header)
     * @return UserDto with conditionally masked fields
     */
    @GetMapping("/masked/{id}")
    public UserDto getMaskedUserById(@PathVariable final Long id, @RequestHeader("Mask-Input") String maskInput) {

        UserDto userDto = userMapper.toDto(userService.findUserById(id));
        return MaskMeInitializer.mask(userDto, MaskMeOnInput.class, maskInput);
    }

    /**
     * Retrieves a domain entity with always-masked fields.
     * <p>
     * Fields annotated with {@code @MaskMe(conditions = {AlwaysMaskMeCondition.class})}
     * are masked without requiring any input or headers.
     * </p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * GET /users/user/1
     * }</pre>
     *
     * @param id the user ID
     * @return User domain entity with always-masked fields
     */
    @GetMapping("/user/{id}")
    public User getUserEntity(@PathVariable final Long id) {
        return MaskMeInitializer.mask(userService.findUserById(id));
    }

    /**
     * Retrieves all users with multiple masking conditions applied.
     * <p>
     * Demonstrates combining multiple conditions:
     * <ul>
     *   <li>{@link MaskMeOnInput} - masks based on Mask-Input header</li>
     *   <li>{@link PhoneMaskingCondition} - masks specific phone number from Mask-Phone header</li>
     * </ul>
     * </p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * GET /users
     * Headers:
     *   Mask-Input: maskMe
     *   Mask-Phone: 01000000000
     * }</pre>
     *
     * <p>Only the phone "01000000000" will be masked, others remain visible.</p>
     *
     * @param maskInput the input value for MaskMeOnInput condition
     * @param maskPhone the phone number to mask via PhoneMaskingCondition
     * @return List of UserDto with multiple conditions applied
     */
    @GetMapping
    public List<UserDto> getUsers(@RequestHeader("Mask-Input") String maskInput, @RequestHeader("Mask-Phone") String maskPhone) {

        return userService.findUsers().stream()
                .map(user ->
                        MaskMeInitializer.mask(userMapper.toDto(user),
                                getMaskedConditionsForGetAllUsers(maskInput, maskPhone)))
                .toList();
    }

    /**
     * Builds the condition array for multiple condition masking.
     * <p>
     * Format: [ConditionClass1, input1, ConditionClass2, input2, ...]
     * </p>
     *
     * @param maskInput input for MaskMeOnInput condition
     * @param maskPhone input for PhoneMaskingCondition
     * @return array of conditions and their inputs
     */
    private static Object @NonNull [] getMaskedConditionsForGetAllUsers(String maskInput, String maskPhone) {
        return new Object[]{MaskMeOnInput.class, maskInput,
                PhoneMaskingCondition.class, maskPhone};
    }
}
