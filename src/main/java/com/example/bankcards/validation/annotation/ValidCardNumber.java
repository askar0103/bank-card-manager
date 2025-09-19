package com.example.bankcards.validation.annotation;

import com.example.bankcards.validation.constraint.CardNumberValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;

@Target({FIELD, PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = CardNumberValidator.class)
public @interface ValidCardNumber {

    String message() default "Invalid card number format";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
