package com.example.bankcards.validation.constraint;

import com.example.bankcards.validation.annotation.ValidCardNumber;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class CardNumberValidator implements ConstraintValidator<ValidCardNumber, String> {

    @Value("${validation.regex.card-number}")
    private String cardNumberRegex;

    private Pattern pattern;

    @Override
    public void initialize(ValidCardNumber constraintAnnotation) {
        this.pattern = Pattern.compile(cardNumberRegex);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && pattern.matcher(value).matches();
    }
}
