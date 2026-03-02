/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 */
package com.javamsdt.masking.maskme.config;

import com.javamsdt.masking.maskme.converter.CustomStringConverter;
import io.github.javamsdt.maskme.api.condition.MaskMeConditionFactory;
import io.github.javamsdt.maskme.api.condition.MaskMeFrameworkProvider;
import io.github.javamsdt.maskme.api.converter.MaskMeConverterRegistry;
import io.github.javamsdt.maskme.api.exception.MaskMeException;
import io.github.javamsdt.maskme.implementation.condition.AlwaysMaskMeCondition;
import io.github.javamsdt.maskme.implementation.condition.MaskMeOnInput;
import io.github.javamsdt.maskme.logging.MaskMeLogger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Level;

/**
 * Configuration class for MaskMe library integration with Spring Framework.
 * <p>
 * This class configures MaskMe to work with Spring's ApplicationContext
 * for automatic dependency injection and bean management.
 * </p>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Automatic Spring bean discovery for custom conditions</li>
 *   <li>@Bean methods for built-in conditions (required for a pure Java library)</li>
 *   <li>Custom converter registration</li>
 *   <li>Lifecycle management with @PostConstruct and @PreDestroy</li>
 * </ul>
 *
 * <p><b>Usage:</b> This configuration is automatically picked up by Spring's
 * component scanning. No manual registration is needed.</p>
 *
 * @author Ahmed Samy
 * @since 1.0.0
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MaskMeConfiguration {

    /**
     * Spring ApplicationContext for bean resolution.
     * Injected automatically by Spring via @RequiredArgsConstructor.
     */
    private final ApplicationContext applicationContext;

    /**
     * Initializes MaskMe configuration after bean construction.
     * <p>
     * This method is automatically invoked by Spring after all dependencies are injected.
     * It configures logging, registers the Spring framework provider, and sets up custom converters.
     * </p>
     */
    @PostConstruct
    public void setupMaskMe() {
        // Logger configuration
        MaskMeLogger.enable(Level.INFO);

        // To register Spring as applicationContext so you can leverage DI inside CustomCondition.
        registerMaskConditionProvider();

        // To override the default converters, or set up a new converter based on your need.
        setupCustomConverters();

        // Optional: Configure a custom field reference pattern
        // Default is {fieldName}, you can change to [fieldName] or others
        // MaskMeFieldAccessUtil.setUserPattern(Pattern.compile("\\{([^}]+)}"));
    }

    /**
     * Declares AlwaysMaskMeCondition as a Spring bean.
     * <p>
     * This is REQUIRED because MaskMe is a pure Java library and doesn't
     * automatically register its built-in conditions with Spring.
     * Without this @Bean declaration, Spring won't be able to resolve
     * AlwaysMaskMeCondition when MaskMe requests it.
     * </p>
     *
     * @return a new AlwaysMaskMeCondition instance
     */
    @Bean
    public AlwaysMaskMeCondition alwaysMaskMeCondition() {
        return new AlwaysMaskMeCondition();
    }

    /**
     * Declares MaskMeOnInput as a Spring bean.
     * <p>
     * This is REQUIRED because MaskMe is a pure Java library and doesn't
     * automatically register its built-in conditions with Spring.
     * </p>
     *
     * @return a new MaskMeOnInput instance
     */
    @Bean
    public MaskMeOnInput maskMeOnInput() {
        return new MaskMeOnInput();
    }
    // --- built-in conditions --- //

    /**
     * Registers the Spring ApplicationContext as the framework provider for MaskMe.
     * <p>
     * This provider uses {@code applicationContext.getBean(type)} to resolve
     * condition instances from Spring's container, enabling full dependency injection
     * support for custom conditions.
     * </p>
     */
    private void registerMaskConditionProvider() {
        MaskMeConditionFactory.setFrameworkProvider(new MaskMeFrameworkProvider() {
            @Override
            public <T> T getInstance(Class<T> type) {
                try {
                    return applicationContext.getBean(type);
                } catch (Exception e) {
                    log.warn("Failed to get bean of type {} from the application context", type.getName(), e);
                    throw new MaskMeException("Failed to get bean of type " + type.getName() + " from the application context", e);
                }
            }
        });
    }

    /**
     * Configures custom converters for type conversion during masking.
     * <p>
     * Clears any existing global converters to prevent memory leaks from
     * previous application runs, then registers custom converters that can
     * override default conversion behavior.
     * </p>
     */
    private void setupCustomConverters() {
        // Clear Global
        // to avoid any memory leak from the previous application run.
        // that way you will avoid any custom register to live in memory
        // due to the live reference from this run
        MaskMeConverterRegistry.clearGlobal();

        // Register user's custom converters
        MaskMeConverterRegistry.registerGlobal(new CustomStringConverter());
    }

    /**
     * Cleans up resources on application shutdown.
     * <p>
     * This method is automatically invoked by Spring when the application context
     * is being destroyed. It clears all global converters to prevent memory leaks.
     * </p>
     */
    @PreDestroy
    public void destroy() {
        // to avoid any memory leak from the current application run.
        // that way you will avoid any custom register to live in memory
        // due to the live reference from this run
        MaskMeConverterRegistry.clearGlobal();
    }
}