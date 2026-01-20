/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 */
package com.javamsdt.masking.maskme.config;

import com.javamsdt.masking.maskme.converter.CustomStringConverter;
import com.javamsdt.maskme.api.condition.MaskMeConditionFactory;
import com.javamsdt.maskme.api.condition.MaskMeFrameworkProvider;
import com.javamsdt.maskme.api.converter.MaskMeConverterRegistry;
import com.javamsdt.maskme.api.exception.MaskMeException;
import com.javamsdt.maskme.implementation.condition.AlwaysMaskMeCondition;
import com.javamsdt.maskme.implementation.condition.MaskMeOnInput;
import com.javamsdt.maskme.logging.MaskMeLogger;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Level;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MaskMeConfiguration {

    private final ApplicationContext applicationContext;

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

    // --- built-in conditions --- //
    // Declare built-in conditions as beans to avoid NoSuchBeanDefinitionException, because the library is pure java.
    @Bean
    public AlwaysMaskMeCondition alwaysMaskMeCondition() {
        return new AlwaysMaskMeCondition();
    }

    @Bean
    public MaskMeOnInput maskMeOnInput() {
        return new MaskMeOnInput();
    }
    // --- built-in conditions --- //

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

    private void setupCustomConverters() {
        // Clear Global
        // to avoid any memory leak from the previous application run.
        // that way you will avoid any custom register to live in memory
        // due to the live reference from this run
        MaskMeConverterRegistry.clearGlobal();

        // Register user's custom converters
        MaskMeConverterRegistry.registerGlobal(new CustomStringConverter());
    }

    @PreDestroy
    public void destroy() {
        // to avoid any memory leak from the current application run.
        // that way you will avoid any custom register to live in memory
        // due to the live reference from this run
        MaskMeConverterRegistry.clearGlobal();
    }
}