package org.example.techsupbot.bot;

import lombok.extern.log4j.Log4j2;
import org.example.techsupbot.DTO.Client;
import org.example.techsupbot.DTO.ClientService;
import org.example.techsupbot.data.ButtonLabels;
import org.example.techsupbot.data.ClientStatus;
import org.example.techsupbot.redis.RedisService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


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
        return null;
    }

    @Override
    public String getBotPath() {
        return "";
    }
}


