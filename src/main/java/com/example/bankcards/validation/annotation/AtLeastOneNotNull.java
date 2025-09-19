package com.example.bankcards.validation.annotation;

import com.example.bankcards.validation.constraint.AtLeastOneNotNullValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = AtLeastOneNotNullValidator.class)
public @interface AtLeastOneNotNull {

    String message() default "At least one field must be provided";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
