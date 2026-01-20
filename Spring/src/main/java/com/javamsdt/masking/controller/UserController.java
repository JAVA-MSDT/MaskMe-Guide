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

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/{id}")
    public UserDto getUserById(@PathVariable final Long id) {
        return userMapper.toDto(userService.findUserById(id));
    }

    @GetMapping("/masked/{id}")
    public UserDto getMaskedUserById(@PathVariable final Long id, @RequestHeader("Mask-Input") String maskInput) {

        UserDto userDto = userMapper.toDto(userService.findUserById(id));
        return MaskMeInitializer.mask(userDto, MaskMeOnInput.class, maskInput);
    }

    @GetMapping("/user/{id}")
    public User getUser(@PathVariable final Long id) {
        return MaskMeInitializer.mask(userService.findUserById(id));
    }

    @GetMapping
    public List<UserDto> getUsers(@RequestHeader("Mask-Input") String maskInput, @RequestHeader("Mask-Phone") String maskPhone) {

        return userService.findUsers().stream()
                .map(user ->
                        MaskMeInitializer.mask(userMapper.toDto(user),
                                getMaskedConditionsForGetAllUsers(maskInput, maskPhone)))
                .toList();
    }

    private static Object @NonNull [] getMaskedConditionsForGetAllUsers(String maskInput, String maskPhone) {
        return new Object[]{MaskMeOnInput.class, maskInput,
                PhoneMaskingCondition.class, maskPhone};
    }
}
