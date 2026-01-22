package com.javamsdt.javamasking.masking;

import com.javamsdt.javamasking.domain.User;
import com.javamsdt.javamasking.dto.UserDto;
import com.javamsdt.javamasking.mapper.UserMapper;
import com.javamsdt.javamasking.mapper.UserMapperImpl;
import com.javamsdt.javamasking.maskme.condition.PhoneMaskingCondition;
import com.javamsdt.javamasking.service.UserService;
import com.javamsdt.maskme.MaskMeInitializer;
import com.javamsdt.maskme.implementation.condition.MaskMeOnInput;

import java.util.List;

/**
 * Demonstrates various masking scenarios using the MaskMe library in pure Java.
 * <p>
 * This class provides methods that mirror typical REST controller operations
 * but outputs results to console instead of HTTP responses.
 * </p>
 *
 * <p><b>Supported Scenarios:</b></p>
 * <ul>
 *   <li>Unmasked data retrieval</li>
 *   <li>Conditional masking with MaskMeOnInput</li>
 *   <li>Always-masked domain entities</li>
 *   <li>Multiple conditions (MaskMeOnInput + PhoneMaskingCondition)</li>
 * </ul>
 *
 * @author Ahmed Samy
 * @since 1.0.0
 */
@SuppressWarnings("java:S106")
public class UserMasking {

    private final UserService userService;
    private final UserMapper userMapper;

    /**
     * Constructs a UserMasking instance with required dependencies.
     *
     * @param userService the service for retrieving user data
     */
    public UserMasking(UserService userService) {
        this.userService = userService;
        this.userMapper = new UserMapperImpl();
    }

    /**
     * Retrieves and displays a user without any masking applied.
     * <p>
     * This demonstrates the baseline - original data without modifications.
     * </p>
     *
     * @param id the user ID to retrieve
     */
    public void getUserById(Long id) {
        UserDto userDto = userMapper.toDto(userService.findUserById(id));
        System.out.println("\n=== Get User By ID (No Masking) ===");
        System.out.println(userDto);
    }

    /**
     * Retrieves and displays a user with conditional masking applied.
     * <p>
     * Uses {@link MaskMeOnInput} condition - fields are masked only when
     * the input matches the condition's expected value.
     * </p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * userMasking.getMaskedUserById(1L, "maskMe");
     * // Fields with MaskMeOnInput condition will be masked
     * }</pre>
     *
     * @param id        the user ID to retrieve
     * @param maskInput the input value for MaskMeOnInput condition
     */
    public void getMaskedUserById(Long id, String maskInput) {
        UserDto userDto = userMapper.toDto(userService.findUserById(id));
        UserDto masked = MaskMeInitializer.mask(userDto, MaskMeOnInput.class, maskInput);
        System.out.println("\n=== Get Masked User By ID ===");
        System.out.println("Mask Input: " + maskInput);
        System.out.println(masked);
    }

    /**
     * Retrieves and displays a domain entity with always-masked fields.
     * <p>
     * Fields annotated with {@code @MaskMe(conditions = {AlwaysMaskMeCondition.class})}
     * are masked without requiring any input.
     * </p>
     *
     * @param id the user ID to retrieve
     */
    public void getUserEntity(Long id) {
        User user = userService.findUserById(id);
        User masked = MaskMeInitializer.mask(user);
        System.out.println("\n=== Get User Entity (Always Masked) ===");
        System.out.println(masked);
    }

    /**
     * Retrieves and displays all users with multiple masking conditions applied.
     * <p>
     * Demonstrates combining multiple conditions:
     * <ul>
     *   <li>{@link MaskMeOnInput} - masks based on maskInput parameter</li>
     *   <li>{@link PhoneMaskingCondition} - masks specific phone number</li>
     * </ul>
     * </p>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * userMasking.getUsers("maskMe", "01000000000");
     * // Only the phone "01000000000" will be masked, others remain visible
     * }</pre>
     *
     * @param maskInput the input value for MaskMeOnInput condition
     * @param maskPhone the phone number to mask via PhoneMaskingCondition
     */
    public void getUsers(String maskInput, String maskPhone) {
        List<UserDto> users = userService.findUsers().stream()
                .map(user ->
                        MaskMeInitializer.mask(userMapper.toDto(user),
                                getMaskedConditionsForGetAllUsers(maskInput, maskPhone)))
                .toList();
        System.out.println("\n=== Get All Users (Multiple Conditions) ===");
        System.out.println("Mask Input: " + maskInput);
        System.out.println("Mask Phone: " + maskPhone);
        users.forEach(System.out::println);
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
    private static Object[] getMaskedConditionsForGetAllUsers(String maskInput, String maskPhone) {
        return new Object[]{MaskMeOnInput.class, maskInput,
                PhoneMaskingCondition.class, maskPhone};
    }
}
