package com.zs.seckill.validator;

import org.apache.commons.lang3.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class MobileValidator implements ConstraintValidator<Mobile, String> {

    private boolean required = false;

    private static final Pattern PATTERN = Pattern.compile("1\\d{10}");

    @Override
    public void initialize(Mobile constraintAnnotation) {
        required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StringUtils.isEmpty(value)) {
            return false;
        }
        return PATTERN.matcher(value).matches();
    }

}
