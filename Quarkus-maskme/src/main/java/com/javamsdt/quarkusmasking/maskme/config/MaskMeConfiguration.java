/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 */
package com.javamsdt.quarkusmasking.maskme.config;

import io.github.javamsdt.maskme.api.condition.MaskMeConditionFactory;
import io.github.javamsdt.maskme.api.condition.MaskMeFrameworkProvider;
import io.github.javamsdt.maskme.api.converter.MaskMeConverterRegistry;
import io.github.javamsdt.maskme.implementation.condition.AlwaysMaskMeCondition;
import io.github.javamsdt.maskme.implementation.condition.MaskMeOnInput;
import io.github.javamsdt.maskme.logging.MaskMeLogger;
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
     * Quarkus automatically invokes this method when the application starts.
     * It configures logging, registers the CDI framework provider, and sets up custom converters.
     * </p>
     *
     * @param ev the startup event (provided by Quarkus)
     */
    void onStart(@Observes StartupEvent ev) {
        // Step 1: (Optional) Enable logging for debugging — disable in production for zero overhead
        MaskMeLogger.enable(Level.FINE);

        // Step 2: Register Quarkus CDI so MaskMe resolves conditions via dependency injection
        registerMaskConditionProvider();

        // Step 3: Clear and register custom converters to override default type conversion
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
    // Step 4: Declare built-in conditions as CDI beans with @Produces + @Unremovable
    // REQUIRED — MaskMe is a pure Java library, Quarkus won't find these without @Produces
    // @Unremovable is CRITICAL — Quarkus removes "unused" beans at build time,
    // but MaskMe looks them up programmatically via CDI.current().select(type).get()
    @Produces
    @ApplicationScoped
    @Unremovable
    public AlwaysMaskMeCondition alwaysMaskMeCondition() {
        return new AlwaysMaskMeCondition();
    }

    @Produces
    @ApplicationScoped
    @Unremovable
    public MaskMeOnInput maskMeOnInput() {
        return new MaskMeOnInput();
    }

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
        // Step 3a: Clear global converters to avoid memory leaks from previous runs
        MaskMeConverterRegistry.clearGlobal();

        // Step 3b: Register your custom converters (priority > 0 to override defaults)
        MaskMeConverterRegistry.registerGlobal(new CustomStringConverter());
    }

    /**
     * Cleans up resources on application shutdown.
     * <p>
     * Quarkus automatically invokes this method when the application stops.
     * It clears all global converters to prevent memory leaks.
     * </p>
     *
     * @param ev the shutdown event (provided by Quarkus)
     */
    // Step 5: Clean up on shutdown — prevents memory leaks from the current run
    void onStop(@Observes io.quarkus.runtime.ShutdownEvent ev) {
        MaskMeConverterRegistry.clearGlobal();
    }
}
