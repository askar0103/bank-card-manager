package com.example.bankcards.validation.constraint;

import com.example.bankcards.validation.annotation.ValidId;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IdValidator implements ConstraintValidator<ValidId, Number> {

    @Override
    public boolean isValid(Number id, ConstraintValidatorContext context) {
        return id != null && id.longValue() > 0;
    }
}
