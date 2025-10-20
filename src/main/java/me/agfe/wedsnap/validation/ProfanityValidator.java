package me.agfe.wedsnap.validation;

import com.vane.badwordfiltering.BadWordFiltering;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProfanityValidator implements ConstraintValidator<NoProfanity, String> {

    private final BadWordFiltering badWordFiltering;

    public ProfanityValidator() {
        this.badWordFiltering = new BadWordFiltering();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        return !badWordFiltering.blankCheck(value);
    }
}
