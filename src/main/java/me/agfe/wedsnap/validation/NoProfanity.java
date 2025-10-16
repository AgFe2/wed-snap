package me.agfe.wedsnap.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ProfanityValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface NoProfanity {
    String message() default "부적절한 단어가 포함되어 있습니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
