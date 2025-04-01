package org.example.techsupbot.bot;

import org.example.techsupbot.DTO.Client;
import org.example.techsupbot.DTO.ClientService;
import org.example.techsupbot.data.ButtonLabels;
import org.example.techsupbot.data.ClientStatus;
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

import java.util.List;


@Component
public class TechSupBot extends TelegramLongPollingBot {
    RedisService redisService;
    TelegramBotConfig config;
    MessageHandler handler;
    ClientService clientService;
    ScheduleService scheduleService;
   // TelegramRestController controller;
   public TechSupBot(ScheduleService scheduleService, ClientService clientService, TelegramBotConfig config, MessageHandler handler, RedisService redisService) {
        super(config.getToken());
        this.handler = handler;
       this.clientService = clientService;
       this.redisService = redisService;
        this.config = config;
        this.scheduleService = scheduleService;
        scheduleService.init(this);
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
            //ЕСЛИ НАЖАЛ КНОПКУ "ВЕРНУТЬСЯ В ГЛАВНОЕ МЕНЮ
            if (update.getMessage().getText().equals("/start") || update.getMessage().getText().equals(ButtonLabels.MAIN_MENU.getLabel())) {
                Client currentClient = clientService.findByChatId(update.getMessage().getChatId());
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(update.getMessage().getChatId());
                lastMessage = executeMessage(sendWelcomeMessage(currentClient, sendMessage));
            } else {
                lastMessage = executeMessage(handler.processMessage(update.getMessage().getChatId(), update.getMessage()));
            }
           if(lastMessage!=null){
               redisService.saveLastMessageId(lastMessage.getChatId(), lastMessage.getMessageId());
           }

        } else if (update.hasMessage() && update.getMessage().hasPhoto()) {
            lastMessage = executeMessage(handler.processPhoto(update.getMessage().getChatId(), update.getMessage().getPhoto()));
            if(lastMessage!=null){
                redisService.saveLastMessageId(lastMessage.getChatId(), lastMessage.getMessageId());
            }
        } else if (update.hasCallbackQuery()) {
            if (update.getCallbackQuery().getData().equals("main_menu")) {
                Client currentClient = clientService.findByChatId(update.getCallbackQuery().getMessage().getChatId());
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
                lastMessage = executeMessage(sendWelcomeMessage(currentClient, sendMessage));
            }else{
                lastMessage= executeMessage(handler.processCallback(update, update.getCallbackQuery().getData()));
            }
            if(lastMessage!=null){
                redisService.saveLastMessageId(lastMessage.getChatId(), lastMessage.getMessageId());
            }
        }
    }

    private SendMessage sendWelcomeMessage(Client currentClient, SendMessage message) {
       deleteLastMessage(currentClient.getChatId());
       message.setText("""
                👋 Привет! Добро пожаловать в MustHaveCase!
                Мы рады, что вы с нами! Здесь вы найдете стильные, надежные и уникальные чехлы для вашего телефона. А еще мы всегда готовы помочь с выбором или решить любой вопрос.
                
                Что я могу для вас сделать?
                - 🛠️ Помочь с сервисной поддержкой, если что-то пошло не так.
                - 🎨 Помогу создать индивидуальный чехол.
                - 💬 Пригласить в нашу группу, где вы найдете акции, новинки и общение с другими клиентами.
                - 🛒 Рассказать о текущих акциях и скидках.
                
                Просто выберите нужную кнопку ниже, и я помогу вам! 😊""");
        message.setReplyMarkup(handler.createInlineKeyboard(
                List.of(
                        new Pair<>("🛠️ Сервисная поддержка", "service_support"),
                        new Pair<>("🎨 Создать индивидуальный чехол", "create_case"),
                        new Pair<>("💬 Вступить в группу", "join_group"),
                        new Pair<>("🛒 Акции и скидки", "promotions"),
                        new Pair<>("\uD83D\uDCBC Сотрудничество", "cooperation")
                )
        ));
        currentClient.setStatus(ClientStatus.SAVED);
        clientService.saveClient(currentClient);

        return message;
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


