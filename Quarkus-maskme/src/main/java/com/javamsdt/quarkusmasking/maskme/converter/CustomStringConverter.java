/**
 * Copyright (c) 2025: Ahmed Samy, All rights reserved.
 * LinkedIn: https://www.linkedin.com/in/java-msdt/
 * GitHub: https://github.com/JAVA-MSDT
 * Email: serenitydiver@hotmail.com
 */
package com.javamsdt.quarkusmasking.maskme.converter;

import com.javamsdt.maskme.api.converter.MaskMeConverter;
import com.javamsdt.maskme.api.utils.MaskMeFieldAccessUtil;

public class CustomStringConverter implements MaskMeConverter {

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean canConvert(Class<?> type) {
        return type == String.class;
    }

    @Override
    public Object convert(String value, Class<?> targetType, Object originalValue,
                          Object containingObject, String fieldName) {

        String processValue = MaskMeFieldAccessUtil.getMaskedValueFromAnotherFieldOrMaskedValue(value, containingObject);

        if (fieldName.contains("password")) {
            return "************";
        }

        if (fieldName.contains("email")) {
            if (processValue.isEmpty()) {
                return "[EMAIL PROTECTED]";
            }
            return processValue;
        }

        return null;
    }
}
