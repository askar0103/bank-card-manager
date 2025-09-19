package com.example.bankcards.validation.constraint;

import com.example.bankcards.validation.annotation.ValidUsername;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class UsernameValidator implements ConstraintValidator<ValidUsername, String> {

    @Value("${validation.regex.username}")
    private String usernameRegex;

    private Pattern pattern;

    @Override
    public void initialize(ValidUsername constraintAnnotation) {
        this.pattern = Pattern.compile(usernameRegex);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && pattern.matcher(value).matches();
    }
}
