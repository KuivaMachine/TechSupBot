package org.example.techsupbot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


@Component
public class TechSupBot extends TelegramLongPollingBot {

    TelegramBotConfig config;
    MessageHandler handler;
//    TelegramRestController controller;
    public TechSupBot(TelegramBotConfig config, MessageHandler handler) {
        super(config.getToken());
        this.handler = handler;
        this.config = config;
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
            handler.registerBot(this);
//            SetWebhook setWebhook = SetWebhook.builder().url(config.getUrl()).build();
//            this.setWebhook(setWebhook);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return "MustHaveCase_bot";
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
           executeMessage(handler.processMessage(update.getMessage().getChatId(), update.getMessage()));
        } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
            executeMessage(handler.processPhoto(update.getMessage().getChatId(), update.getMessage().getPhoto()));
        } else if (update.hasCallbackQuery()) {
            executeMessage(handler.processCallback(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getData()));
        }
    }


    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


   /* @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        return controller.receiveUpdate(update);
    }

    @Override
    public String getBotPath() {
        return "/update";
    }*/
}


