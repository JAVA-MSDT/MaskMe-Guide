/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.javamsdt.quarkusmasking.controller;

import com.javamsdt.quarkusmasking.dto.UserDto;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
@DisplayName("UserResource Integration Tests - MaskMe Library")
class UserResourceIntegrationTest {

    @Nested
    @DisplayName("Unmasked Endpoints")
    class UnmaskedEndpoints {

        @Test
        @DisplayName("Should return original user data without any masking")
        void shouldReturnOriginalUserData() {
            // Given: User ID 1 exists in the system
            Long userId = 1L;

            // When: Requesting user without masking headers
            UserDto userDto = given()
                    .when()
                    .get("/users/{id}", userId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("id", equalTo(1))
                    .body("name", equalTo("Ahmed Samy"))
                    .body("email", equalTo("one@mail.com"))
                    .body("phone", equalTo("01000000000"))
                    .body("address.city", equalTo("City One"))
                    .extract()
                    .as(UserDto.class);

            // Then: Response contains unmasked data
            assert userDto.email().equals("one@mail.com");
            assert userDto.name().equals("Ahmed Samy");
        }
    }

    @Nested
    @DisplayName("Conditional Masking with MaskMeOnInput")
    class ConditionalMasking {

        @Test
        @DisplayName("Should mask sensitive fields when Mask-Input header is 'maskMe'")
        void shouldMaskSensitiveFieldsWhenConditionMatches() {
            // Given: User ID 1 exists and Mask-Input header is set to 'maskMe'
            Long userId = 1L;
            String maskInput = "maskMe";

            // When: Requesting masked user with matching condition input
            UserDto userDto = given()
                    .header("Mask-Input", maskInput)
                    .when()
                    .get("/users/masked/{id}", userId)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("id", equalTo(1000))
                    .body("password", equalTo("************"))
                    .body("email", equalTo("[EMAIL PROTECTED]"))
                    .body("birthDate", equalTo("1800-01-01"))
                    .body("balance", equalTo(0))
                    .extract()
                    .as(UserDto.class);

            // Then: Sensitive fields are masked and field referencing works
            assert userDto.name().contains("one@mail.com");
            assert userDto.name().contains("-M");
            assert userDto.address().city().equals("****");
            assert userDto.address().zipCode().equals("[ZIP_MASKED]");
            assert userDto.address().geoLocation().latitude().equals(0.0);
        }

        @Test
        @DisplayName("Should not mask fields when Mask-Input header doesn't match condition")
        void shouldNotMaskWhenConditionDoesNotMatch() {
            // Given: User ID 1 exists and Mask-Input header is set to 'doNotMask'
            Long userId = 1L;
            String maskInput = "doNotMask";

            // When: Requesting masked user with non-matching condition input
            given()
                    .header("Mask-Input", maskInput)
                    .when()
                    .get("/users/masked/{id}", userId)
                    .then()
                    .statusCode(200)
                    .body("id", equalTo(1000))
                    .body("name", equalTo("Ahmed Samy"))
                    .body("address.zipCode", equalTo("Zip One"));

            // Then: MaskMeOnInput condition fields remain unmasked, AlwaysMaskMeCondition still applies
        }
    }

    @Nested
    @DisplayName("Custom Condition with Dependency Injection")
    class CustomConditionTests {

        @Test
        @DisplayName("Should mask only matching phone number using PhoneMaskingCondition")
        void shouldMaskOnlyMatchingPhoneNumber() {
            // Given: Multiple users exist and Mask-Phone header targets specific phone
            String maskInput = "maskMe";
            String targetPhone = "01000000000";

            // When: Requesting all users with phone masking condition
            UserDto[] users = given()
                    .header("Mask-Input", maskInput)
                    .header("Mask-Phone", targetPhone)
                    .when()
                    .get("/users")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("[0].phone", equalTo("****"))
                    .body("[1].phone", equalTo("01000000011"))
                    .body("[2].phone", equalTo("01000000022"))
                    .extract()
                    .as(UserDto[].class);

            // Then: Only the matching phone number is masked
            assert users.length == 4;
            assert users[0].phone().equals("****");
            assert users[1].phone().equals("01000000011");
        }
    }

    @Nested
    @DisplayName("AlwaysMaskMeCondition Tests")
    class AlwaysMaskConditionTests {

        @Test
        @DisplayName("Should always mask fields with AlwaysMaskMeCondition on domain entity")
        void shouldAlwaysMaskFieldsOnDomainEntity() {
            // Given: User ID 1 exists
            Long userId = 1L;

            // When: Requesting domain entity (not DTO)
            given()
                    .when()
                    .get("/users/user/{id}", userId)
                    .then()
                    .statusCode(200)
                    .body("email", equalTo("****"))
                    .body("password", equalTo("************"))
                    .body("birthDate", notNullValue())
                    .body("balance", equalTo(0));

            // Then: Fields with AlwaysMaskMeCondition are masked without any input
        }
    }

    @Nested
    @DisplayName("@ExcludeMaskMe Annotation Tests")
    class ExcludeMaskMeTests {

        @Test
        @DisplayName("Should not process nested object marked with @ExcludeMaskMe")
        void shouldNotProcessExcludedNestedObject() {
            // Given: User ID 1 exists with address having @ExcludeMaskMe
            Long userId = 1L;

            // When: Requesting user without masking
            UserDto userDto = given()
                    .when()
                    .get("/users/{id}", userId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(UserDto.class);

            // Then: Nested address object is not processed for masking
            assert userDto.genderName() != null;
            assert userDto.genderName().equals("Male");
        }
    }

    @Nested
    @DisplayName("Field Referencing Tests")
    class FieldReferencingTests {

        @Test
        @DisplayName("Should replace {fieldName} placeholders with actual field values")
        void shouldReplacePlaceholdersWithFieldValues() {
            // Given: User ID 1 exists and name field has maskValue = "{email}-{genderId}"
            Long userId = 1L;
            String maskInput = "maskMe";

            // When: Requesting masked user
            UserDto userDto = given()
                    .header("Mask-Input", maskInput)
                    .when()
                    .get("/users/masked/{id}", userId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(UserDto.class);

            // Then: Field references are resolved correctly
            assert userDto.name().contains("one@mail.com");
            assert userDto.name().contains("-M");
        }
    }

    @Nested
    @DisplayName("Custom Converter Tests")
    class CustomConverterTests {

        @Test
        @DisplayName("Should use custom converter with higher priority over default converters")
        void shouldUseCustomConverterWithHigherPriority() {
            // Given: CustomStringConverter is registered with priority 10
            Long userId = 1L;
            String maskInput = "maskMe";

            // When: Requesting masked user
            UserDto userDto = given()
                    .header("Mask-Input", maskInput)
                    .when()
                    .get("/users/masked/{id}", userId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(UserDto.class);

            // Then: Custom converter logic is applied
            assert userDto.password().equals("************");
            assert userDto.email().equals("[EMAIL PROTECTED]");
        }
    }

    @Nested
    @DisplayName("Type Conversion Tests")
    class TypeConversionTests {

        @Test
        @DisplayName("Should correctly convert mask values to different field types")
        void shouldConvertMaskValuesToCorrectTypes() {
            // Given: User has fields of different types (Long, LocalDate, BigDecimal, Instant)
            Long userId = 1L;
            String maskInput = "maskMe";

            // When: Requesting masked user
            UserDto userDto = given()
                    .header("Mask-Input", maskInput)
                    .when()
                    .get("/users/masked/{id}", userId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(UserDto.class);

            // Then: All types are converted correctly
            assert userDto.id().equals(1000L);
            assert userDto.birthDate().equals(LocalDate.of(1800, 1, 1));
            assert userDto.balance().equals(BigDecimal.ZERO);
            assert userDto.createdAt() != null;
        }
    }

    @Nested
    @DisplayName("Nested Object Masking Tests")
    class NestedObjectMaskingTests {

        @Test
        @DisplayName("Should recursively mask fields in nested objects")
        void shouldRecursivelyMaskNestedObjectFields() {
            // Given: User has nested AddressDto with masked fields
            Long userId = 1L;
            String maskInput = "maskMe";

            // When: Requesting masked user
            UserDto userDto = given()
                    .header("Mask-Input", maskInput)
                    .when()
                    .get("/users/masked/{id}", userId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .as(UserDto.class);

            // Then: Nested object fields are masked correctly
            assert userDto.address().city().equals("****");
            assert userDto.address().zipCode().equals("[ZIP_MASKED]");
            assert userDto.address().geoLocation() != null;
            assert userDto.address().geoLocation().latitude().equals(0.0);
        }
    }
}
