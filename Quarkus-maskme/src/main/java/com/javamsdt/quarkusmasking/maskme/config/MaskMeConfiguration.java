/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 */
package com.javamsdt.quarkusmasking.maskme.config;

import com.javamsdt.maskme.api.condition.MaskMeConditionFactory;
import com.javamsdt.maskme.api.condition.MaskMeFrameworkProvider;
import com.javamsdt.maskme.api.converter.MaskMeConverterRegistry;
import com.javamsdt.maskme.implementation.condition.AlwaysMaskMeCondition;
import com.javamsdt.maskme.implementation.condition.MaskMeOnInput;
import com.javamsdt.maskme.logging.MaskMeLogger;
import com.javamsdt.quarkusmasking.maskme.converter.CustomStringConverter;
import io.quarkus.arc.Unremovable;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;

import java.util.logging.Level;

/**
 * Configuration class for MaskMe library integration with Quarkus.
 * <p>
 * This class configures MaskMe to work with Quarkus CDI (Contexts and Dependency Injection)
 * by registering a framework provider that resolves condition instances from the CDI container.
 * </p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Automatic CDI bean discovery for custom conditions</li>
 *   <li>Producer methods for built-in conditions with @Unremovable</li>
 *   <li>Custom converter registration</li>
 *   <li>Lifecycle management (startup/shutdown)</li>
 * </ul>
 *
 * <p><b>Important:</b> All condition beans must be annotated with {@code @Unremovable}
 * to prevent Quarkus from removing them during build-time optimization.</p>
 *
 * @author Ahmed Samy
 * @see io.quarkus.arc.Unremovable
 * @since 1.0.0
 */
@ApplicationScoped
public class MaskMeConfiguration {

    /**
     * Initializes MaskMe configuration on application startup.
     * <p>
     * This method is automatically invoked by Quarkus when the application starts.
     * It configures logging, registers the CDI framework provider, and sets up custom converters.
     * </p>
     *
     * @param ev the startup event (provided by Quarkus)
     */
    void onStart(@Observes StartupEvent ev) {
        System.out.println("MaskMeConfiguration startup event triggered");
        MaskMeLogger.enable(Level.FINE);
        registerMaskConditionProvider();
        setupCustomConverters();
    }

    /**
     * Produces an AlwaysMaskMeCondition bean for CDI.
     * <p>
     * The {@code @Unremovable} annotation is CRITICAL - it prevents Quarkus from
     * removing this bean during build-time optimization. Without it, the bean
     * won't be available at runtime for programmatic lookup.
     * </p>
     *
     * @return a new AlwaysMaskMeCondition instance
     */
    @Produces
    @ApplicationScoped
    @Unremovable
    public AlwaysMaskMeCondition alwaysMaskMeCondition() {
        return new AlwaysMaskMeCondition();
    }

    /**
     * Produces a MaskMeOnInput condition bean for CDI.
     * <p>
     * The {@code @Unremovable} annotation is CRITICAL - it prevents Quarkus from
     * removing this bean during build-time optimization.
     * </p>
     *
     * @return a new MaskMeOnInput instance
     */
    @Produces
    @ApplicationScoped
    @Unremovable
    public MaskMeOnInput maskMeOnInput() {
        return new MaskMeOnInput();
    }

//    @Produces
//    @ApplicationScoped
//    @Unremovable
//    public PhoneMaskingCondition phoneMaskingCondition(UserService userService) {
//        return new PhoneMaskingCondition(userService);
//    }

    /**
     * Registers the Quarkus CDI framework provider with MaskMe.
     * <p>
     * This provider uses {@code CDI.current().select(type).get()} to resolve
     * condition instances from the CDI container. If a bean is not found,
     * it returns null to allow MaskMe to fall back to reflection-based instantiation.
     * </p>
     */
    private void registerMaskConditionProvider() {
        MaskMeConditionFactory.setFrameworkProvider(new MaskMeFrameworkProvider() {
            @Override
            public <T> T getInstance(Class<T> type) {
                try {
                    return CDI.current().select(type).get();
                } catch (Exception e) {
                    System.out.println("[DEBUG] Failed to get bean " + type.getName() + ": " + e.getMessage());
                    return null;
                }
            }
        });
    }

    /**
     * Configures custom converters for type conversion during masking.
     * <p>
     * Clears any existing global converters and registers custom ones.
     * This ensures a clean state and allows custom converters to override defaults.
     * </p>
     */
    private void setupCustomConverters() {
        MaskMeConverterRegistry.clearGlobal();
        MaskMeConverterRegistry.registerGlobal(new CustomStringConverter());
    }

    /**
     * Cleans up resources on application shutdown.
     * <p>
     * This method is automatically invoked by Quarkus when the application stops.
     * It clears all global converters to prevent memory leaks.
     * </p>
     *
     * @param ev the shutdown event (provided by Quarkus)
     */
    void onStop(@Observes io.quarkus.runtime.ShutdownEvent ev) {
        MaskMeConverterRegistry.clearGlobal();
    }
}
