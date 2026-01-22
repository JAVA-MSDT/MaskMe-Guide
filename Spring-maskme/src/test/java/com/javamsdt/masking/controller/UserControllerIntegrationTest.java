/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.javamsdt.masking.controller;

import com.javamsdt.masking.dto.UserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DisplayName("UserController Integration Tests - MaskMe Library")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Unmasked Endpoints")
    class UnmaskedEndpoints {

        @Test
        @DisplayName("Should return original user data without any masking")
        void shouldReturnOriginalUserData() throws Exception {
            // Given: User ID 1 exists in the system
            Long userId = 1L;

            // When: Requesting user without masking headers
            MvcResult result = mockMvc.perform(get("/users/{id}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Ahmed Samy"))
                    .andExpect(jsonPath("$.email").value("one@mail.com"))
                    .andExpect(jsonPath("$.phone").value("01000000000"))
                    .andExpect(jsonPath("$.address.city").value("City One"))
                    .andReturn();

            // Then: Response contains unmasked data
            UserDto userDto = objectMapper.readValue(result.getResponse().getContentAsString(), UserDto.class);
            assertThat(userDto.email()).isEqualTo("one@mail.com");
            assertThat(userDto.name()).isEqualTo("Ahmed Samy");
        }
    }

    @Nested
    @DisplayName("Conditional Masking with MaskMeOnInput")
    class ConditionalMasking {

        @Test
        @DisplayName("Should mask sensitive fields when Mask-Input header is 'maskMe'")
        void shouldMaskSensitiveFieldsWhenConditionMatches() throws Exception {
            // Given: User ID 1 exists and Mask-Input header is set to 'maskMe'
            Long userId = 1L;
            String maskInput = "maskMe";

            // When: Requesting masked user with matching condition input
            MvcResult result = mockMvc.perform(get("/users/masked/{id}", userId)
                            .header("Mask-Input", maskInput))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1000))
                    .andExpect(jsonPath("$.password").value("************"))
                    .andExpect(jsonPath("$.email").value("[EMAIL PROTECTED]"))
                    .andExpect(jsonPath("$.birthDate").value("1800-01-01"))
                    .andExpect(jsonPath("$.balance").value(0))
                    .andReturn();

            // Then: Sensitive fields are masked and field referencing works
            UserDto userDto = objectMapper.readValue(result.getResponse().getContentAsString(), UserDto.class);
            assertThat(userDto.name()).contains("one@mail.com").contains("-M");
            assertThat(userDto.address().city()).isEqualTo("****");
            assertThat(userDto.address().zipCode()).isEqualTo("[ZIP_MASKED]");
            assertThat(userDto.address().geoLocation().latitude()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should not mask fields when Mask-Input header doesn't match condition")
        void shouldNotMaskWhenConditionDoesNotMatch() throws Exception {
            // Given: User ID 1 exists and Mask-Input header is set to 'doNotMask'
            Long userId = 1L;
            String maskInput = "doNotMask";

            // When: Requesting masked user with non-matching condition input
            mockMvc.perform(get("/users/masked/{id}", userId)
                            .header("Mask-Input", maskInput))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1000))
                    .andExpect(jsonPath("$.name").value("Ahmed Samy"))
                    .andExpect(jsonPath("$.address.zipCode").value("Zip One"));
        }
    }

    @Nested
    @DisplayName("Custom Condition with Dependency Injection")
    class CustomConditionTests {

        @Test
        @DisplayName("Should mask only matching phone number using PhoneMaskingCondition")
        void shouldMaskOnlyMatchingPhoneNumber() throws Exception {
            // Given: Multiple users exist and Mask-Phone header targets specific phone
            String maskInput = "maskMe";
            String targetPhone = "01000000000";

            // When: Requesting all users with phone masking condition
            MvcResult result = mockMvc.perform(get("/users")
                            .header("Mask-Input", maskInput)
                            .header("Mask-Phone", targetPhone))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].phone").value("****"))
                    .andExpect(jsonPath("$[1].phone").value("01000000011"))
                    .andExpect(jsonPath("$[2].phone").value("01000000022"))
                    .andReturn();

            // Then: Only the matching phone number is masked
            UserDto[] users = objectMapper.readValue(result.getResponse().getContentAsString(), UserDto[].class);
            assertThat(users).hasSize(4);
            assertThat(users[0].phone()).isEqualTo("****");
            assertThat(users[1].phone()).isNotEqualTo("****");
        }
    }

    @Nested
    @DisplayName("AlwaysMaskMeCondition Tests")
    class AlwaysMaskConditionTests {

        @Test
        @DisplayName("Should always mask fields with AlwaysMaskMeCondition on domain entity")
        void shouldAlwaysMaskFieldsOnDomainEntity() throws Exception {
            // Given: User ID 1 exists
            Long userId = 1L;

            // When: Requesting domain entity (not DTO)
            mockMvc.perform(get("/users/user/{id}", userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("****"))
                    .andExpect(jsonPath("$.password").value("************"))
                    .andExpect(jsonPath("$.birthDate").exists())
                    .andExpect(jsonPath("$.balance").value(0))
                    .andReturn();

            // Then: Fields with AlwaysMaskMeCondition are masked without any input
        }
    }

    @Nested
    @DisplayName("@ExcludeMaskMe Annotation Tests")
    class ExcludeMaskMeTests {

        @Test
        @DisplayName("Should not process nested object marked with @ExcludeMaskMe")
        void shouldNotProcessExcludedNestedObject() throws Exception {
            // Given: User ID 1 exists with address having @ExcludeMaskMe
            Long userId = 1L;

            // When: Requesting user without masking
            MvcResult result = mockMvc.perform(get("/users/{id}", userId))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then: Nested address object is not processed for masking
            UserDto userDto = objectMapper.readValue(result.getResponse().getContentAsString(), UserDto.class);
            assertThat(userDto.genderName()).isNotNull();
            assertThat(userDto.genderName()).isEqualTo("Male");
        }
    }

    @Nested
    @DisplayName("Field Referencing Tests")
    class FieldReferencingTests {

        @Test
        @DisplayName("Should replace {fieldName} placeholders with actual field values")
        void shouldReplacePlaceholdersWithFieldValues() throws Exception {
            // Given: User ID 1 exists and name field has maskValue = "{email}-{genderId}"
            Long userId = 1L;
            String maskInput = "maskMe";

            // When: Requesting masked user
            MvcResult result = mockMvc.perform(get("/users/masked/{id}", userId)
                            .header("Mask-Input", maskInput))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then: Field references are resolved correctly
            UserDto userDto = objectMapper.readValue(result.getResponse().getContentAsString(), UserDto.class);
            assertThat(userDto.name()).contains("one@mail.com");
            assertThat(userDto.name()).contains("-M");
        }
    }

    @Nested
    @DisplayName("Custom Converter Tests")
    class CustomConverterTests {

        @Test
        @DisplayName("Should use custom converter with higher priority over default converters")
        void shouldUseCustomConverterWithHigherPriority() throws Exception {
            // Given: CustomStringConverter is registered with priority 10
            Long userId = 1L;
            String maskInput = "maskMe";

            // When: Requesting masked user
            MvcResult result = mockMvc.perform(get("/users/masked/{id}", userId)
                            .header("Mask-Input", maskInput))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then: Custom converter logic is applied
            UserDto userDto = objectMapper.readValue(result.getResponse().getContentAsString(), UserDto.class);
            assertThat(userDto.password()).isEqualTo("************");
            assertThat(userDto.email()).isEqualTo("[EMAIL PROTECTED]");
        }
    }

    @Nested
    @DisplayName("Type Conversion Tests")
    class TypeConversionTests {

        @Test
        @DisplayName("Should correctly convert mask values to different field types")
        void shouldConvertMaskValuesToCorrectTypes() throws Exception {
            // Given: User has fields of different types (Long, LocalDate, BigDecimal, Instant)
            Long userId = 1L;
            String maskInput = "maskMe";

            // When: Requesting masked user
            MvcResult result = mockMvc.perform(get("/users/masked/{id}", userId)
                            .header("Mask-Input", maskInput))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then: All types are converted correctly
            UserDto userDto = objectMapper.readValue(result.getResponse().getContentAsString(), UserDto.class);
            assertThat(userDto.id()).isEqualTo(1000L);
            assertThat(userDto.birthDate()).isEqualTo(LocalDate.of(1800, 1, 1));
            assertThat(userDto.balance()).isEqualTo(BigDecimal.ZERO);
            assertThat(userDto.createdAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Nested Object Masking Tests")
    class NestedObjectMaskingTests {

        @Test
        @DisplayName("Should recursively mask fields in nested objects")
        void shouldRecursivelyMaskNestedObjectFields() throws Exception {
            // Given: User has nested AddressDto with masked fields
            Long userId = 1L;
            String maskInput = "maskMe";

            // When: Requesting masked user
            MvcResult result = mockMvc.perform(get("/users/masked/{id}", userId)
                            .header("Mask-Input", maskInput))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then: Nested object fields are masked correctly
            UserDto userDto = objectMapper.readValue(result.getResponse().getContentAsString(), UserDto.class);
            assertThat(userDto.address().city()).isEqualTo("****");
            assertThat(userDto.address().zipCode()).isEqualTo("[ZIP_MASKED]");
            assertThat(userDto.address().geoLocation()).isNotNull();
            assertThat(userDto.address().geoLocation().latitude()).isEqualTo(0.0);
        }
    }
}
