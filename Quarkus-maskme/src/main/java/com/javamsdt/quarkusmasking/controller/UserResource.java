/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.javamsdt.quarkusmasking.controller;

import com.javamsdt.maskme.MaskMeInitializer;
import com.javamsdt.maskme.implementation.condition.MaskMeOnInput;
import com.javamsdt.quarkusmasking.domain.User;
import com.javamsdt.quarkusmasking.dto.UserDto;
import com.javamsdt.quarkusmasking.mapper.UserMapper;
import com.javamsdt.quarkusmasking.maskme.condition.PhoneMaskingCondition;
import com.javamsdt.quarkusmasking.service.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/**
 * JAX-RS REST resource demonstrating MaskMe library usage with Quarkus.
 * <p>
 * This resource provides endpoints showcasing different masking scenarios:
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
@Path("/users")
public class UserResource {

    @Inject
    UserService userService;

    @Inject
    UserMapper userMapper;

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
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserDto getUserById(@PathParam("id") Long id) {
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
    @GET
    @Path("/masked/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public UserDto getMaskedUserById(@PathParam("id") Long id, @HeaderParam("Mask-Input") String maskInput) {
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
    @GET
    @Path("/user/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public User getUserEntity(@PathParam("id") Long id) {
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
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserDto> getUsers(@HeaderParam("Mask-Input") String maskInput, @HeaderParam("Mask-Phone") String maskPhone) {
        return userService.findUsers().stream()
                .map(user -> MaskMeInitializer.mask(userMapper.toDto(user),
                        MaskMeOnInput.class, maskInput,
                        PhoneMaskingCondition.class, maskPhone))
                .toList();
    }
}
