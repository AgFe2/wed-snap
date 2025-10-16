package me.agfe.wedsnap.validation;

import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProfanityValidator implements ConstraintValidator<NoProfanity, String> {

    private static final List<String> BANNED_WORDS = List.of(
            "시발", "씨발", "병신", "fuck", "sex", "ㅅㅂ", "ㅂㅅ"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String lower = value.toLowerCase();
        for (String banned : BANNED_WORDS) {
            if (lower.contains(banned)) {
                return false;
            }
        }

        return true;
    }
}
