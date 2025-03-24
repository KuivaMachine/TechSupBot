package org.example.techsupbot;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

/*
@RestController
@RequiredArgsConstructor
@FieldDefaults (level = AccessLevel.PRIVATE, makeFinal = true)
public class TelegramRestController {

    MessageHandler handler;

    @PostMapping ("/callback/bot_tech_sup/update")
    public BotApiMethod<?> receiveUpdate(@RequestBody Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            return handler.processMessage(update.getMessage().getChatId(), update.getMessage());
        } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
            return handler.processPhoto(update.getMessage().getChatId(), update.getMessage().getPhoto());
        } else if (update.hasCallbackQuery()) {
            return handler.processCallback(update, update.getCallbackQuery().getData());
        }
        return null;
    }
}
*/
