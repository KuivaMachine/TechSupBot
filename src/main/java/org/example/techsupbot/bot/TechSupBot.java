package org.example.techsupbot.bot;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


@Log4j2
@Component
public class TechSupBot extends TelegramWebhookBot {

    TelegramBotConfig config;
    ScheduleService scheduleService;
    TelegramRestController controller;

   public TechSupBot(ScheduleService scheduleService,TelegramBotConfig config, TelegramRestController controller) {
        super(config.getToken());
        this.config = config;
        this.controller=controller;
        this.scheduleService = scheduleService;
        scheduleService.init(this);
        try {
            controller.registerBot(this);
            SetWebhook setWebhook = SetWebhook.builder().url(config.getUrl()).build();
            this.setWebhook(setWebhook);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return "MustHaveCase_bot";
    }


    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        return controller.receiveUpdate(update);
    }

    @Override
    public String getBotPath() {
        return "/bot_tech_sup/update";
    }
}


