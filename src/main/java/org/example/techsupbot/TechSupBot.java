package org.example.techsupbot;

import org.example.techsupbot.redis.RedisService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


@Component
public class TechSupBot extends TelegramLongPollingBot {
    RedisService redisService;
    TelegramBotConfig config;
    MessageHandler handler;
   // TelegramRestController controller;
   public TechSupBot(TelegramBotConfig config, MessageHandler handler, RedisService redisService) {
        super(config.getToken());
        this.handler = handler;
       this.redisService = redisService;
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
        Message lastMessage;
        if (update.hasMessage() && update.getMessage().hasText()) {
           lastMessage = executeMessage(handler.processMessage(update.getMessage().getChatId(), update.getMessage()));
           if(lastMessage!=null){
               redisService.saveLastMessageId(lastMessage.getChatId(), lastMessage.getMessageId());
           }
        } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
            lastMessage = executeMessage(handler.processPhoto(update.getMessage().getChatId(), update.getMessage().getPhoto()));
            if(lastMessage!=null){
                redisService.saveLastMessageId(lastMessage.getChatId(), lastMessage.getMessageId());
            }
        } else if (update.hasCallbackQuery()) {
           lastMessage= executeMessage(handler.processCallback(update, update.getCallbackQuery().getData()));
            if(lastMessage!=null){
                redisService.saveLastMessageId(lastMessage.getChatId(), lastMessage.getMessageId());
            }
        }
    }


    private Message executeMessage(SendMessage message) {
        try {
            return execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteLastMessage(Long chatId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(Integer.parseInt(redisService.getLastMessageId(chatId)));
        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }


    /*@Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        return controller.receiveUpdate(update);
    }

    @Override
    public String getBotPath() {
        return "/bot_tech_sup/update";
    }*/
}


