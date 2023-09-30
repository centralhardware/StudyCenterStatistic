package me.centralhardware.znatoki.telegram.statistic.eav.types;

import io.vavr.control.Validation;
import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.meta.api.objects.Update;

public non-sealed class Integer implements Type {
    @Override
    public String format(String name, Boolean isOptional) {
        return STR."Введите \{name} (число). \{isOptional? optionalText: ""}";
    }

    @Override
    public Validation<String, Void> __validate(Update update, String...variants) {
        return update.hasMessage() && StringUtils.isNumeric(update.getMessage().getText()) ?
                Validation.valid(null) :
                Validation.invalid("Введите число");
    }

    @Override
    public String extract(Update update) {
        return update.getMessage().getText();
    }

}
