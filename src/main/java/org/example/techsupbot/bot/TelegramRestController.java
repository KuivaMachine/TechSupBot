package org.example.techsupbot.bot;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.example.techsupbot.DTO.Client;
import org.example.techsupbot.DTO.ClientService;
import org.example.techsupbot.data.ButtonLabels;
import org.example.techsupbot.data.ClientStatus;
import org.example.techsupbot.redis.RedisService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TelegramRestController {

    final MessageHandler handler;
    final ClientService clientService;
    final RedisService redisService;
    private TechSupBot telegramBot;
    private final Map<String, List<Message>> mediaGroups = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        handler.initController(this);
    }
    public void registerBot(TechSupBot techSupBot) {
        this.telegramBot=techSupBot;
    }

    @PostMapping("/callback/bot_tech_sup/update")
    public BotApiMethod<?> receiveUpdate(@RequestBody Update update) {

            Message lastMessage;
            //ЕСЛИ ПРОСТО ТЕКСТ
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
                if (lastMessage != null) {
                    redisService.saveLastMessageInfo(lastMessage.getChatId(), lastMessage.getMessageId());
                }

            } else //ЕСЛИ ПРИСЛАЛИ ФОТО
                if (update.hasMessage() && update.getMessage().hasPhoto()&&update.getMessage().getMediaGroupId() == null) {
                    lastMessage = executeMessage(handler.processPhoto(update.getMessage().getChatId(), update.getMessage()));
                    if (lastMessage != null) {
                        redisService.saveLastMessageInfo(lastMessage.getChatId(), lastMessage.getMessageId());
                    }
                } else //ЕСЛИ НАЖАЛИ НА КНОПКУ
                    if (update.hasCallbackQuery()) {
                        if (update.getCallbackQuery().getData().equals("main_menu")) {
                            Client currentClient = clientService.findByChatId(update.getCallbackQuery().getMessage().getChatId());
                            SendMessage sendMessage = new SendMessage();
                            sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
                            lastMessage = executeMessage(sendWelcomeMessage(currentClient, sendMessage));
                        } else {
                            lastMessage = executeMessage(handler.processCallback(update, update.getCallbackQuery().getData()));
                        }
                        if (lastMessage != null) {
                            redisService.saveLastMessageInfo(lastMessage.getChatId(), lastMessage.getMessageId());
                        }
                    } else //ЕСЛИ ПРИСЛАЛИ ГРУППУ ФОТО
                        if (update.hasMessage() && update.getMessage().getMediaGroupId() != null) {
                            Message message = update.getMessage();

                            String mediaGroupId = message.getMediaGroupId();
                            Long chatId = message.getChatId();

                            // Добавляем сообщение в группу
                            mediaGroups.computeIfAbsent(mediaGroupId, k -> new ArrayList<>()).add(message);

                            // Запускаем обработку с задержкой (чтобы собрать все части)
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    processCompleteMediaGroup(chatId, mediaGroupId);
                                }
                            }, 2000);
                        }
                        if(update.hasMessage()&&update.getMessage().hasDocument()){
                            executeMessage(handler.processFile(update.getMessage().getChatId()));
                        }
                        return null;
        }

        private void processCompleteMediaGroup(Long chatId, String mediaGroupId) {
            List<Message> groupMessages = mediaGroups.remove(mediaGroupId);
            if (groupMessages == null || groupMessages.isEmpty()) return;

            String caption = groupMessages.getFirst().getCaption();

            List<InputMedia> mediaList = new ArrayList<>();

            for (Message msg : groupMessages) {
                if (msg.hasPhoto()) {
                    String fileId = msg.getPhoto().getLast().getFileId();
                    InputMediaPhoto media = new InputMediaPhoto();
                    media.setMedia(fileId);
                    mediaList.add(media);
                }
            }

            executeMessage(handler.processMediaGroup(chatId, caption, mediaList));
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

        Message executeMessage(SendMessage message) {
            try {
                return telegramBot.execute(message);
            } catch (TelegramApiException e) {
                log.error("ОШИБКА ОТПРАВКИ СООБЩЕНИЯ - ", e);
            }
            return null;
        }

        void executeMessage(SendMediaGroup message) {
            try {
                telegramBot.execute(message);
            } catch (TelegramApiException e) {
                log.error("ОШИБКА ОТПРАВКИ ГРУППЫ МЕДИАФАЙЛОВ - ", e);
            }
        }
        void executeMessage(SendPhoto message) {
            try {
                telegramBot.execute(message);
            } catch (TelegramApiException e) {
                log.error("ОШИБКА ОТПРАВКИ ГРУППЫ ФОТО - ", e);
            }
        }
        ChatMember executeMessage(GetChatMember message) {
            try {
                return telegramBot.execute(message);
            } catch (TelegramApiException e) {
                log.error("ОШИБКА ПРИ ПРОВЕРКЕ ПОДПИСКИ НА КАНАЛ @MustHaveCase - {}", e.getMessage());
            }
            return null;
        }

        public void deleteLastMessage(Long chatId) {
            if (redisService.isMessageDeletable(chatId)) {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(chatId);
                deleteMessage.setMessageId(redisService.getLastMessageId(chatId));
                try {
                    telegramBot.execute(deleteMessage);
                } catch (TelegramApiException e) {
                    log.error(String.format("УДАЛИТЬ СООБЩЕНИЕ У ПОЛЬЗОВАТЕЛЯ %s НЕ УДАЛОСЬ, ПО ПРИЧИНЕ: %s", chatId, e.getMessage()));
                }
            }
        }

}
