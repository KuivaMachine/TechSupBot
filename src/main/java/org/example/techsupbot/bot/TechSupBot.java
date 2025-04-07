package org.example.techsupbot.bot;

import lombok.extern.log4j.Log4j2;
import org.example.techsupbot.DTO.ClientService;
import org.example.techsupbot.redis.RedisService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


@Log4j2
@Component
public class TechSupBot extends TelegramWebhookBot {
    MessageHandler handler;
    ClientService clientService;
    RedisService redisService;
    TelegramBotConfig config;
    ScheduleService scheduleService;
    TelegramRestController controller;

    public TechSupBot( TelegramRestController controller, ClientService clientService, MessageHandler handler , RedisService redisService, ScheduleService scheduleService, TelegramBotConfig config) {
        super(config.getToken());
        this.config = config;
        this.controller = controller;
        this.handler = handler;
        this.clientService = clientService;
        this.redisService = redisService;
        this.scheduleService = scheduleService;
        scheduleService.init(this);
        controller.registerBot(this);
        try {
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


