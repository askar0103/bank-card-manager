package com.example.bankcards.validation.constraint;

import com.example.bankcards.validation.annotation.AtLeastOneNotNull;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;

public class AtLeastOneNotNullValidator implements ConstraintValidator<AtLeastOneNotNull, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        try {
            Field[] fields = value.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.get(value) != null) {
                    return true;
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return false;
    }
}
