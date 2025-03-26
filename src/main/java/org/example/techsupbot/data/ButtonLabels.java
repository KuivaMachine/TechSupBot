package org.example.techsupbot.data;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ButtonLabels {
    ATTACH_IMAGE("Прикрепить фото"),
    ATTACH_SCREEN("Прикрепить скрин"),
    ATTACH_DESCRIPTION("Добавить описание"),
    SEND("Отправить заявку на возврат или замену"),
    MAIN_MENU("Вернуться в главное меню"), CANCEL("Отмена"),
    CANCEL_ORDER_QUESTION("Я передумал писать");
    String label;


}
