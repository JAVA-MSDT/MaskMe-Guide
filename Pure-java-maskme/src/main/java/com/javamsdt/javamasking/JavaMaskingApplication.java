package com.javamsdt.javamasking;


import com.javamsdt.javamasking.masking.UserMasking;
import com.javamsdt.javamasking.maskme.config.MaskMeConfiguration;
import com.javamsdt.javamasking.service.UserService;

/**
 * Main application demonstrating MaskMe library usage in pure Java.
 * <p>
 * This application showcases four different masking scenarios without
 * any framework dependencies (Spring, Quarkus, etc.).
 * </p>
 *
 * <p><b>Demonstrated Scenarios:</b></p>
 * <ol>
 *   <li>Unmasked data retrieval</li>
 *   <li>Conditional masking with MaskMeOnInput</li>
 *   <li>Always-masked domain entities with AlwaysMaskMeCondition</li>
 *   <li>Multiple conditions (MaskMeOnInput + PhoneMaskingCondition)</li>
 * </ol>
 *
 * <p><b>Running the Application:</b></p>
 * <pre>{@code
 * mvn clean install
 * java -cp target/Pure-java-maskme-0.0.1-SNAPSHOT.jar com.javamsdt.javamasking.JavaMaskingApplication
 * }</pre>
 *
 * @author Ahmed Samy
 * @since 1.0.0
 */
@SuppressWarnings("java:S106")
public class JavaMaskingApplication {

    /**
     * Application entry point.
     * <p>
     * Initializes services, configures MaskMe library, and demonstrates
     * various masking scenarios.
     * </p>
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        // Initialize services
        UserService userService = new UserService();
        UserMasking userMasking = new UserMasking(userService);

        // Configure MaskMe library
        configureMasking(userService);

        // Test scenarios
        System.out.println("\n" + "=".repeat(60));
        System.out.println("MaskMe Pure Java Integration Demo");
        System.out.println("=".repeat(60));

        // 1. Get user without masking
        userMasking.getUserById(1L);

        // 2. Get user with conditional masking
        userMasking.getMaskedUserById(1L, "maskMe");

        // 3. Get domain entity (always masks certain fields)
        userMasking.getUserEntity(1L);

        // 4. Get all users with phone masking for specific number
        userMasking.getUsers("maskMe", "01000000000");

        System.out.println("\n" + "=".repeat(60));
    }


    /**
     * Configures the MaskMe library with required dependencies.
     * <p>
     * This method sets up:
     * <ul>
     *   <li>Logging configuration</li>
     *   <li>Custom condition registration with dependencies</li>
     *   <li>Framework provider for dependency lookup</li>
     *   <li>Custom converters</li>
     * </ul>
     * </p>
     *
     * @param userService the user service to inject into custom conditions
     */
    public static void configureMasking(UserService userService) {
        MaskMeConfiguration.setupMaskMe(userService);
    }
}
