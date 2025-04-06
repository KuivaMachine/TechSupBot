package org.example.techsupbot.bot;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.example.techsupbot.DTO.Client;
import org.example.techsupbot.DTO.ClientService;
import org.example.techsupbot.data.ButtonLabels;
import org.example.techsupbot.data.ClientStatus;
import org.example.techsupbot.googlesheets.GoogleSheetsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageHandler {

    @Value("${bot.manager_id}")
    private Long managerChatId;
    TelegramRestController controller;
    final ClientService clientService;
    final GoogleSheetsService googleSheetsService;


    public void initController(TelegramRestController controller) {
        this.controller = controller;
    }

    public SendMessage processMessage(Long chatId, Message update) {
        String text = update.getText();
        Client currentclient = clientService.findByChatId(chatId);
        currentclient.setUsername(update.getFrom().getUserName());
        SendMessage message = new SendMessage();
        message.setChatId(chatId);

        switch (text) {
            case "/delete_me" -> {
                clientService.deleteClientByChatId(chatId);
                message.setText("Клиент удален");
                message.setReplyMarkup(new ReplyKeyboardRemove(true));
                return message;
            }
            case "/update_table" -> {
                googleSheetsService.updateTable(clientService.getAllClients());
                message.setText("Таблица обновлена");
                return message;
            }
        }
        //ЕСЛИ НАЖАЛИ КНОПКУ ОТМЕНИТЬ
        if (text.equals(ButtonLabels.CANCEL.getLabel())) {
            return cancelProcess(currentclient, message);
        }
        //ЕСЛИ НАЖАЛИ ПРИКРЕПИТЬ ФОТО
        if (text.equals(ButtonLabels.ATTACH_IMAGE.getLabel())) {
            return addImageProcess(currentclient, message);
        }
        //ЕСЛИ НАЖАЛИ ПРИКРЕПИТЬ СКРИН
        if (text.equals(ButtonLabels.ATTACH_SCREEN.getLabel())) {
            return addsScreenProcess(currentclient, message);
        }
        //ЕСЛИ НАЖАЛИ ПРИКРЕПИТЬ ОПИСАНИЕ
        if (text.equals(ButtonLabels.ATTACH_DESCRIPTION.getLabel())) {
            return addDescriptionProcess(currentclient, message);
        }
        //ЕСЛИ ОЖИДАЕТСЯ ОПИСАНИЕ
        if (currentclient.getStatus().equals(ClientStatus.WAITING_DESCRIPTION)) {
            currentclient.setDescription(text);
            clientService.saveClient(currentclient);
            return fillReturnDataProcess(currentclient, message);
        }
        //ЕСЛИ ОЖИДАЕТСЯ ПЛОХОЙ ОТЗЫВ НА СЕРВИС (НАЖАЛИ 1-3 ЗВЕЗДЫ)
        if (currentclient.getStatus().equals(ClientStatus.WAITING_BAD_FEEDBACK)) {
            message.setText("Спасибо за Ваш отзыв!");
            message.setReplyMarkup(createInlineKeyboard(List.of(new Pair<>("Назад в главное меню \uD83D\uDD19","main_menu"))));
            currentclient.setStatus(ClientStatus.SAVED);
            currentclient.setServiceFeedback(text);
            clientService.saveClient(currentclient);
            googleSheetsService.updateTable(clientService.getAllClients());
            return message;
        }
        //ЕСЛИ ОЖИДАЕТСЯ ПЛОХОЙ ОТЗЫВ НА КОНСТРУКТОР (НАЖАЛИ 1-3 ЗВЕЗДЫ)
        if (currentclient.getStatus().equals(ClientStatus.WAITING_BAD_FEEDBACK_CONSTRUCTOR)) {
            message.setText("Спасибо за Ваш отзыв!");
            message.setReplyMarkup(createInlineKeyboard(List.of(new Pair<>("Назад в главное меню \uD83D\uDD19","main_menu"))));
            currentclient.setStatus(ClientStatus.SAVED);
            currentclient.setConstructorFeedback(text);
            clientService.saveClient(currentclient);
            googleSheetsService.updateTable(clientService.getAllClients());
            return message;
        }
        //ЕСЛИ СТАТУС "ВОПРОС ПО ЗАКАЗУ"
        if (currentclient.getStatus().equals(ClientStatus.ORDER_QUESTION)) {
            //ЕСЛИ НАЖАЛИ КНОПКУ Я ПЕРЕДУМАЛ ПИСАТЬ
            if(text.equals(ButtonLabels.CANCEL_ORDER_QUESTION.getLabel())){
                controller.deleteLastMessage(chatId);
                message.setText("Хорошо, я все отменил \uD83D\uDC4D");
                message.setReplyMarkup(createInlineKeyboard(List.of(new Pair<>("Назад в главное меню \uD83D\uDD19","main_menu"))));
                return message;
            }else{
                return sendOrderQuestionProcess(update, message, currentclient);
            }
        }
        //ЕСЛИ КЛИЕНТ ВВОДИТ СООБЩЕНИЕ, ХОТЯ ОЖИДАЕТСЯ ФОТО
        if (currentclient.getStatus().equals(ClientStatus.WAITING_IMAGE)) {
            message.setText("Чтобы добавить фото товара, воспользуйтесь клавиатурой ниже 👇\nСначала нажмите кнопку \"Прикрепить фото\", а затем отправьте фотографию.");
            return message;
        }
        //ЕСЛИ КЛИЕНТ ВВОДИТ СООБЩЕНИЕ, ХОТЯ ОЖИДАЕТСЯ СКРИНШОТ
        if (currentclient.getStatus().equals(ClientStatus.WAITING_SCREEN)) {
            message.setText("Чтобы добавить скриншот личного кабинета, воспользуйтесь клавиатурой ниже 👇\nСначала нажмите кнопку \"Прикрепить скрин\", а затем отправьте скриншот.");
            return message;
        }
        if (currentclient.getStatus().equals(ClientStatus.WRONG_ITEM)) {
            message.setText("Чтобы оформить заявку, воспользуйтесь клавиатурой ниже 👇\nСначала нажмите кнопку, а затем введите нужные данные)");
            return message;
        }
        //ЕСЛИ КЛИЕНТ НАЖАЛ ОТПРАВИТЬ МЕНЕДЖЕРУ И У НЕГО СТАТУС "ОЖИДАЕТ ОТПРАВКИ"
        if (text.equals(ButtonLabels.SEND.getLabel()) && currentclient.getStatus().equals(ClientStatus.WAITING_SEND)) {
            return sendDataToManager(currentclient, message);
        }
        //СООБЩЕНИЕ ПО УМОЛЧАНИЮ, ЕСЛИ НЕ ОДИН ИЗ СЛУЧАЕВ НЕ СРАБОТАЛ
        return setDefaultMessage(message);
    }



    public SendMessage processCallback(Update update, String callback) {
        SendMessage message = new SendMessage();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        message.setChatId(chatId);
        Client currentclient = clientService.findByChatId(chatId);
        currentclient.setUsername(update.getCallbackQuery().getFrom().getUserName());
        return switch (callback) {
            case "service_support" -> sendServiceSupportMessage(currentclient, message);
            case "wrong_item" -> sendWrongItemInstructions(currentclient, message);
            case "damaged_item" -> sendDamagedItemInstructions(currentclient, message);
            case "order_questions" -> sendOrderQuestionsMessage(currentclient, message);
            case "create_case" -> sendCreateCaseMessage(currentclient, message);
            case "join_group" -> sendJoinGroupMessage(chatId, message);
            case "promotions" -> sendPromotionsMessage(chatId, message);
            case "cooperation" -> sendHelpChoiceMessage(chatId, message);
            case "call_to_manager" -> callToManager(update.getCallbackQuery().getFrom().getUserName(), message);
            case "5_stars", "4_stars" -> sendGoodAnswer(currentclient, message,callback);
            case "3_stars", "2_stars", "1_stars" -> sendBadAnswer(currentclient, message,callback);
            case "5_stars_constructor", "4_stars_constructor" -> sendGoodAnswerToConstructor(currentclient, message,callback);
            case "3_stars_constructor", "2_stars_constructor", "1_stars_constructor" -> sendBadAnswerToConstructor(currentclient, message,callback);
            default -> {
                message.setText(callback);
                yield message;
            }
        };
    }




    public SendMessage processPhoto(Long chatId, List<PhotoSize> photos) {
        Client currentclient = clientService.findByChatId(chatId);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("""
                Вы добавили фото, но забыли нажать кнопку "Прикрепить фото" или "Прикрепить скрин"
                """);
        PhotoSize photo = photos.stream().max(Comparator.comparing((x) -> x.getFileSize()
        )).orElse(null);
        if (photo != null) {
            if (currentclient.getStatus().equals(ClientStatus.WAITING_IMAGE)) {
                currentclient.setImage(photo.getFileId());
                currentclient.setStatus(ClientStatus.WRONG_ITEM);
                clientService.saveClient(currentclient);
                return fillReturnDataProcess(currentclient, message);
            }
            if (currentclient.getStatus().equals(ClientStatus.WAITING_SCREEN)) {
                currentclient.setScreenshot(photo.getFileId());
                currentclient.setStatus(ClientStatus.WRONG_ITEM);
                clientService.saveClient(currentclient);
                return fillReturnDataProcess(currentclient, message);
            }
        }
        return message;
    }


    private SendMessage setDefaultMessage(SendMessage message) {
        message.setText("""
                Конечно, я готов помочь! 😊
                Если у тебя остались вопросы, вот что можно сделать:
                
                1️⃣ Связь с поддержкой
                   - Если нужна помощь оператора, я могу подключить нашего специалиста.
                
                2️⃣ Часто задаваемые вопросы (FAQ)
                   - Возможно, ответ уже есть в [нашем разделе FAQ](https://musthavecase.ru/faq)
                
                3️⃣ Написать напрямую директору
                   - Если вопрос важный, пиши на почту: **support@musthavecase.ru**
                   (Письма просматривает лично наш директор.)
                
                👉 Мы всегда рады помочь! Не стесняйся обращаться.
                """);
        message.setReplyMarkup(createInlineKeyboard(List.of(new Pair<>("Вызвать специалиста","call_to_manager"))));
        message.enableMarkdown(true);
        return message;
    }


    private SendMessage callToManager(String user, SendMessage message) {
        SendMessage order = new SendMessage();
        String question = String.format("Клиент @%s нажал кнопку \"Подключить специалиста\"", user);
        order.setChatId(managerChatId);
        order.setText(question);

            controller.executeMessage(order);
            message.setText("Спасибо! Менеджер свяжется с Вами в ближайшее время для уточнения деталей.");
            message.setReplyMarkup(createReplyKeyboard(List.of(new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.MAIN_MENU.getLabel()))))));
            return message;

    }

    private SendMessage cancelProcess(Client currentclient, SendMessage message) {
        message.setText("Заявка на возврат отменена.");
        message.setReplyMarkup(createReplyKeyboard(List.of(new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.MAIN_MENU.getLabel()))))));
        currentclient.setDescription(null);
        currentclient.setImage(null);
        currentclient.setScreenshot(null);
        currentclient.setStatus(ClientStatus.SAVED);
        clientService.saveClient(currentclient);
        return message;
    }
    public void startTimerByServiceSupport(Client currentClient) {
        log.info("ЗАПУСКАЮ ТАЙМЕР ДЛЯ СЕРВИСА");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            sendServiceQualityMessage(currentClient);
        }, 24, TimeUnit.HOURS);
        scheduler.shutdown();

    }

    public void startTimerByCaseConstructor(Client currentClient) {
        log.error("ЗАПУСКАЮ ТАЙМЕР ДЛЯ КОНСТРУКТОРА");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            sendConstructorQualityMessage(currentClient);
        }, 24, TimeUnit.HOURS);
        scheduler.shutdown();

    }

    private void sendConstructorQualityMessage(Client currentClient) {
        SendMessage message = new SendMessage();
        currentClient.setStatus(ClientStatus.WAITING_CONSTRUCTOR_QUALITY);
        clientService.saveClient(currentClient);
        message.setChatId(currentClient.getChatId());
        message.setText("""
                🎨 Понравился ли вам наш конструктор?
               
                Мы стараемся сделать процесс создания чехла максимально удобным и приятным. Пожалуйста, оцените ваш опыт по шкале от 1 до 5, где:
                1 — совсем не понравилось
                5 — все отлично!
                
                Как вам наш конструктор?
                - Удобно ли было выбирать модель телефона и нашли ли свою модель?
                - Легко ли было разобраться с функционалом?
               
                Просто выберите оценку ниже:
                """);
        message.setReplyMarkup(createInlineKeyboard(List.of(
                new Pair<>("⭐️⭐️⭐️⭐️⭐️", "5_stars_constructor"),
                new Pair<>("⭐️⭐️⭐️⭐️", "4_stars_constructor"),
                new Pair<>("⭐️⭐️⭐️", "3_stars_constructor"),
                new Pair<>("⭐️⭐️", "2_stars_constructor"),
                new Pair<>("⭐️", "1_stars_constructor")
        )));
       controller.executeMessage(message);
    }

    private void sendServiceQualityMessage(Client currentClient) {
        SendMessage message = new SendMessage();
        currentClient.setStatus(ClientStatus.WAITING_SERVICE_QUALITY);
        clientService.saveClient(currentClient);
        message.setChatId(currentClient.getChatId());
        message.setText("""
                😊 Мы ценим ваше мнение!
                Пожалуйста, помогите нам стать лучше — оцените качество обслуживания по шкале от 1 до 5, где:
                1 — совсем недоволен
                5 — все отлично!
                
                Как вам наша поддержка?
                - Быстро ли мы ответили на ваш запрос?
                - Удалось ли решить вашу проблему?
                - Были ли наши сотрудники вежливы и внимательны?
                
                Просто выберите оценку ниже:
                """);
        message.setReplyMarkup(createInlineKeyboard(List.of(
                new Pair<>("⭐️⭐️⭐️⭐️⭐️", "5_stars"),
                new Pair<>("⭐️⭐️⭐️⭐️", "4_stars"),
                new Pair<>("⭐️⭐️⭐️", "3_stars"),
                new Pair<>("⭐️⭐️", "2_stars"),
                new Pair<>("⭐️", "1_stars")
        )));
        controller.executeMessage(message);
    }

    private SendMessage sendBadAnswer(Client currentclient, SendMessage message,String callback) {
        if (currentclient.getStatus().equals(ClientStatus.WAITING_SERVICE_QUALITY)) {
            message.setText("""
                    🙏 Спасибо за честный отзыв!
                    Нам очень жаль, что мы не оправдали ваших ожиданий.
                    Пожалуйста, напишите, что именно нам стоит улучшить. Ваше мнение поможет нам стать лучше! 🙌
                    """);
            currentclient.setStatus(ClientStatus.WAITING_BAD_FEEDBACK);
            currentclient.setServiceMark(Byte.parseByte(String.valueOf(callback.charAt(0))));
            clientService.saveClient(currentclient);
            return message;
        }
        message.setText("Эта кнопка неактивна в данный момент)");
        return message;
    }
    private SendMessage sendBadAnswerToConstructor(Client currentclient, SendMessage message,String callback) {
        if (currentclient.getStatus().equals(ClientStatus.WAITING_CONSTRUCTOR_QUALITY)) {
            message.setText("""
                    🙏 Спасибо за честный отзыв!
                    Нам очень жаль, что наш конструктор не оправдал ваших ожиданий.
                    Пожалуйста, напишите, что именно нам стоит улучшить. Ваше мнение поможет нам стать лучше!
                    """);
            currentclient.setStatus(ClientStatus.WAITING_BAD_FEEDBACK_CONSTRUCTOR);
            currentclient.setServiceMark(Byte.parseByte(String.valueOf(callback.charAt(0))));
            clientService.saveClient(currentclient);
            return message;
        }
        message.setText("Эта кнопка неактивна в данный момент)");
        return message;
    }
    private SendMessage sendGoodAnswer(Client currentclient, SendMessage message, String callback) {
        currentclient.setServiceMark(Byte.parseByte(String.valueOf(callback.charAt(0))));
        message.setText("""
                🎉 Спасибо за высокую оценку!
                Мы рады, что смогли вам помочь. Будем и дальше стараться радовать вас качественным сервисом!
                """);
        message.setReplyMarkup(createInlineKeyboard(List.of(new Pair<>("Назад в главное меню \uD83D\uDD19","main_menu"))));
        currentclient.setStatus(ClientStatus.SAVED);
        clientService.saveClient(currentclient);
        googleSheetsService.updateTable(clientService.getAllClients());
        return message;
    }

    private SendMessage sendGoodAnswerToConstructor(Client currentclient, SendMessage message,String callback) {
        currentclient.setConstructorMark(Byte.parseByte(String.valueOf(callback.charAt(0))));
        message.setText("""
                🎉 Спасибо за высокую оценку!
                Мы рады, что вам понравилось создавать чехол с нами.
                Ждем вас снова за новыми уникальными дизайнами!
                """);
        message.setReplyMarkup(createInlineKeyboard(List.of(new Pair<>("Назад в главное меню \uD83D\uDD19","main_menu"))));
        currentclient.setStatus(ClientStatus.SAVED);
        clientService.saveClient(currentclient);
        googleSheetsService.updateTable(clientService.getAllClients());
        return message;
    }
    private SendMessage sendOrderQuestionProcess(Message update, SendMessage message, Client currentClient) {
        SendMessage order = new SendMessage();
        String question = String.format("Вопрос по заказу от пользователя @%s:\n", update.getFrom().getUserName());
        order.setChatId(managerChatId);
        order.setText(question + update.getText());

            controller.executeMessage(order);
            message.setText("Спасибо! Менеджер свяжется с Вами в ближайшее время для уточнения деталей.");
            message.setReplyMarkup(createInlineKeyboard(List.of(new Pair<>("Назад в главное меню \uD83D\uDD19","main_menu"))));
            currentClient.setStatus(ClientStatus.SAVED);
            clientService.saveClient(currentClient);
            if(!currentClient.getUsedService()){
                startTimerByServiceSupport(currentClient);
                currentClient.setUsedService(true);
                clientService.saveClient(currentClient);
            }
            return message;

    }
    private SendMessage fillReturnDataProcess(Client currentclient, SendMessage message) {
        ArrayList<KeyboardRow> keyboard = new ArrayList<>();
        String text = "Отлично! Вам осталось добавить:\n";
        if (currentclient.getScreenshot() == null) {
            text += "- Скриншот из личного кабинета с заказом\n";
            keyboard.add(new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.ATTACH_SCREEN.getLabel()))));
        }
        if (currentclient.getImage() == null) {
            text += "- Фото товара\n";
            keyboard.add(new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.ATTACH_IMAGE.getLabel()))));
        }
        if (currentclient.getDescription() == null) {
            text += "- Описание проблемы\n";
            keyboard.add(new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.ATTACH_DESCRIPTION.getLabel()))));
        }
        if (currentclient.getDescription() != null && currentclient.getImage() != null && currentclient.getScreenshot() != null) {
            message.setText("Отлично! Теперь нажмите \"Отправить заявку на возврат или замену\"");
            currentclient.setStatus(ClientStatus.WAITING_SEND);
            clientService.saveClient(currentclient);
            message.setReplyMarkup(createReplyKeyboard(List.of(new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.SEND.getLabel()))))));
            return message;
        }
        message.setText(text);
        keyboard.add(new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.CANCEL.getLabel()))));
        message.setReplyMarkup(createReplyKeyboard(keyboard));
        return message;
    }


    private SendMessage addDescriptionProcess(Client currentclient, SendMessage message) {
        currentclient.setStatus(ClientStatus.WAITING_DESCRIPTION);
        clientService.saveClient(currentclient);
        message.setText("""
                Опишите, в чем именно заключается проблема:
                """);
        return message;
    }

    private SendMessage addsScreenProcess(Client currentclient, SendMessage message) {
        currentclient.setStatus(ClientStatus.WAITING_SCREEN);
        clientService.saveClient(currentclient);
        message.setText("""
                Отправьте скриншот из личного кабинета с Вашим заказом:
                """);
        return message;
    }

    private SendMessage sendDataToManager(Client currentclient, SendMessage message) {
        SendMediaGroup media = new SendMediaGroup();
        InputMediaPhoto image = new InputMediaPhoto();
        image.setMedia(currentclient.getImage());
        InputMediaPhoto screen = new InputMediaPhoto();
        screen.setMedia(currentclient.getScreenshot());
        screen.setCaption(String.format("Заявка от клиента @%s!\nОписание проблемы:\n%s",currentclient.getUsername(),currentclient.getDescription()));
        media.setMedias(List.of(image, screen));
        media.setChatId(managerChatId);

            controller.executeMessage(media);
            message.setText("Мы передали ваш запрос менеджеру. В ближайшее время с вами свяжутся для уточнения деталей.");
            currentclient.setStatus(ClientStatus.SENT);
            currentclient.setUsedService(true);
            clientService.saveClient(currentclient);
            message.setReplyMarkup(createReplyKeyboard(List.of(new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.MAIN_MENU.getLabel()))))));
            startTimerByServiceSupport(currentclient);
            return message;

    }

    private SendMessage addImageProcess(Client currentclient, SendMessage message) {
        currentclient.setStatus(ClientStatus.WAITING_IMAGE);
        clientService.saveClient(currentclient);
        message.setText("""
                Отправьте 1 фото товара - на нем должна хорошо быть видна суть проблемы:
                """);
        return message;
    }




    private SendMessage sendServiceSupportMessage(Client currentClient, SendMessage message) {
        controller.deleteLastMessage(currentClient.getChatId());
        currentClient.setStatus(ClientStatus.SERVICE_SUPPORT);
        clientService.saveClient(currentClient);
        message.setText("Спасибо, что обратились к нам! Пожалуйста, опишите вашу проблему, мы постараемся помочь вам как можно быстрее!");
        message.setReplyMarkup(createInlineKeyboard(
                List.of(
                        new Pair<>("Я получил не тот товар или дизайн", "wrong_item"),
                        new Pair<>("Товар поврежден или бракован", "damaged_item"),
                        new Pair<>("Есть вопросы по заказу", "order_questions"),
                        new Pair<>("Назад в главное меню \uD83D\uDD19","main_menu")
                )
        ));
        return message;
    }

    private SendMessage sendWrongItemInstructions(Client currentClient, SendMessage message) {
        if (currentClient.getStatus() == ClientStatus.SENT) {
            message.setText("Вы уже отправили заявку на возврат. Наш менеджер скоро свяжется с Вами.");
        } else {
            currentClient.setStatus(ClientStatus.WRONG_ITEM);
            clientService.saveClient(currentClient);
            message.setText("""
                    Мы очень сожалеем, что Вы получили не тот товар или дизайн. Давайте решим эту проблему как можно быстрее!
                    Пожалуйста, выполните следующие шаги:
                    1. 📸 Сфотографируйте товар и пришлите фото нам (это поможет быстрее разобраться в ситуации).
                    2. 📝 Пришлите скрин из личного кабинета с Вашим заказом.
                    3. 📦 Сохраните упаковку и товар в том виде, в котором вы его получили.
                    Как только мы получим эту информацию, мы начнем процесс замены или возврата. Воспользуйтесь клавиатурой ниже 👇
                    """);
            message.setReplyMarkup(createReplyKeyboard(List.of(new KeyboardRow(List.of(
                            new KeyboardButton(ButtonLabels.ATTACH_IMAGE.getLabel()))),
                    new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.ATTACH_SCREEN.getLabel()))),
                    new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.ATTACH_DESCRIPTION.getLabel()))),
                    new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.CANCEL.getLabel())))
            )));
        }
        return message;
    }

    private SendMessage sendDamagedItemInstructions(Client currentClient, SendMessage message) {
        if (currentClient.getStatus() == ClientStatus.SENT) {
            message.setText("Вы уже отправили заявку на возврат. Наш менеджер скоро свяжется с Вами.");
        } else {
            currentClient.setStatus(ClientStatus.WRONG_ITEM);
            clientService.saveClient(currentClient);
            message.setText("""
                    Мы очень сожалеем, что Вы получили бракованный или поврежденный товар. Давайте решим эту проблему как можно быстрее!
                    Пожалуйста, выполните следующие шаги:
                    1. 📸 Сфотографируйте товар и пришлите фото нам (это поможет быстрее разобраться в ситуации.
                    2. 📝 Пришлите скрин из личного кабинета, что б было видно заказ.
                    3. 📦 Сохраните упаковку и товар в том виде, в котором вы его получили.
                    Как только мы получим эту информацию, мы начнем процесс замены или возврата. Воспользуйтесь клавиатурой ниже 👇
                    """);
            message.setReplyMarkup(createReplyKeyboard(List.of(new KeyboardRow(List.of(
                            new KeyboardButton(ButtonLabels.ATTACH_IMAGE.getLabel()))),
                    new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.ATTACH_SCREEN.getLabel()))),
                    new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.ATTACH_DESCRIPTION.getLabel())))
            )));
        }
        return message;
    }

    private SendMessage sendOrderQuestionsMessage(Client currentClient, SendMessage message) {
        message.setText("Спасибо, что обратились к нам! Пожалуйста, опишите ваш вопрос:");
        message.setReplyMarkup(createReplyKeyboard(List.of(new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.CANCEL_ORDER_QUESTION.getLabel()))))));
        currentClient.setStatus(ClientStatus.ORDER_QUESTION);
        clientService.saveClient(currentClient);
        return message;
    }

    private SendMessage sendCreateCaseMessage(Client currentClient, SendMessage message) {
        controller.deleteLastMessage(currentClient.getChatId());
        message.setText("""
                Мы рады, что вы хотите создать что-то уникальное! Нажмите на кнопку ниже, и вы перейдете на наш сайт, где сможете:
                - Выбрать модель телефона.
                - Загрузить свой дизайн или выбрать из готовых вариантов.
                - Добавить текст, изображения или логотипы.
                - Увидеть предварительный результат в режиме реального времени.
                Это просто, быстро и увлекательно!
                👇 Нажмите здесь, чтобы начать:
                [Создать индивидуальный чехол](https://musthavecase.ru/product/cases/konstruktor-chehla)
                
                P.S. Создать можно не только индивидуальный чехол, а еще и обложку на паспорт, футболку, power bank и многое другое)
                Если у вас возникнут вопросы, просто напишите нам — мы всегда готовы помочь! 😊
                """);
        message.setParseMode("Markdown");
        message.setReplyMarkup(createReplyKeyboard(List.of(new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.MAIN_MENU.getLabel()))))));
        if(!currentClient.getUsedConstructor()){
            startTimerByCaseConstructor(currentClient);
            currentClient.setUsedConstructor(true);
            clientService.saveClient(currentClient);
        }
        return message;
    }
    private SendMessage sendJoinGroupMessage(Long chatId, SendMessage message) {
        controller.deleteLastMessage(chatId);
        message.setChatId(chatId.toString());
        message.setText("""
                🎉 Отлично! 🎉
                Спасибо, что решили вступить в нашу группу! Мы рады видеть вас среди наших участников.
                
                👉 Чтобы завершить процесс, просто перейдите по ссылке: https://t.me/MustHaveCase и нажмите кнопку "Подписаться".
                
                Добро пожаловать в нашу дружную команду! 🚀
                """);
        message.setParseMode("Markdown");
        message.setReplyMarkup(createReplyKeyboard(List.of(new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.MAIN_MENU.getLabel()))))));
        return message;
    }

    private SendMessage sendPromotionsMessage(Long chatId, SendMessage message) {
        controller.deleteLastMessage(chatId);
        message.setChatId(chatId);
        message.setText("""
                Привет! 🌟
                Рады, что ты интересуешься нашими акциями и скидками! Вот что у нас сейчас есть:
                
                🎉 Текущие акции и скидки:
                
                1️⃣ Скидка 15% на заказ через сайт при подписке на наши соц. сети
                   - Период действия: до 31 мая 2025 года.
                
                2️⃣ Бесплатная доставка при заказе от 5000 рублей
                   - Акция действует для всех регионов России.
                
                3️⃣ Кэшбек 10% за фото с отметкой в Инстаграм
                
                👉 Чтобы узнать больше, [посетите наш сайт](https://musthavecase.ru)
                """);
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        InlineKeyboardButton telegram_but = new InlineKeyboardButton();
        telegram_but.setText("Подписаться на Телеграмм");
        telegram_but.setUrl("https://t.me/MustHaveCase");
        InlineKeyboardButton wibes_but = new InlineKeyboardButton();
        wibes_but.setText("Подписаться на Wibes");
        wibes_but.setUrl("https://wibes.ru/author/90347");
        InlineKeyboardButton vk_but = new InlineKeyboardButton();
        vk_but.setText("Подписаться на Вконтакте");
        vk_but.setUrl("https://vk.com/musthavecase_ru");
        InlineKeyboardButton instagram_but = new InlineKeyboardButton();
        instagram_but.setText("Подписаться на Инстаграмм");
        instagram_but.setUrl("https://www.instagram.com/musthavecase.ru?igsh=dmE2OTNvdzV5dTVh");
        InlineKeyboardButton main_menu = new InlineKeyboardButton();
        main_menu.setText("Назад в главное меню \uD83D\uDD19");
        main_menu.setCallbackData("main_menu");
        keyboard.add(List.of(telegram_but));
        keyboard.add(List.of(wibes_but));
        keyboard.add(List.of(vk_but));
        keyboard.add(List.of(instagram_but));
        keyboard.add(List.of(main_menu));
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);
        message.enableMarkdown(true);
        return message;
    }

    private SendMessage sendHelpChoiceMessage(Long chatId, SendMessage message) {
        controller.deleteLastMessage(chatId);
        message.setChatId(chatId);
        message.setText("""
                Привет! 🌟
                Мы всегда рады новым партнерствам! Вот направления, по которым мы сотрудничаем:
                
                1️⃣ [Для блогеров и инфлюэнсеров](https://musthavecase.ru/cooperation/)
                   - Условия партнерских программ, рекламные интеграции и специальные предложения.
                
                2️⃣ [Для дизайнеров и иллюстраторов](https://musthavecase.ru/cooperation/)
                   - Коллаборации, создание уникального контента и участие в проектах.
                
                3️⃣ [Для корпоративных клиентов](https://musthavecase.ru/cooperation/)
                   - Индивидуальные решения для бизнеса, корпоративные подарки и спецпредложения.
                
                4️⃣ [Для оптовых клиентов](https://musthavecase.ru/cooperation/)
                   - Выгодные условия закупок, скидки и персональный менеджер.
               
                5️⃣ [Для розничных и интернет-магазинов](https://musthavecase.ru/cooperation/)
                   - Партнерские программы, дропшиппинг и совместные акции.
                
                👉 Выбери подходящий раздел или напиши, если остались вопросы. Мы с радостью обсудим детали!
                """);
        message.enableMarkdown(true);
        message.setReplyMarkup(createReplyKeyboard(List.of(new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.MAIN_MENU.getLabel()))))));
        return message;
    }

    private static ReplyKeyboardMarkup createReplyKeyboard(List<KeyboardRow> list) {
        ArrayList<KeyboardRow> keyboard = new ArrayList<>(list);
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setKeyboard(keyboard);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        return replyKeyboardMarkup;
    }

    InlineKeyboardMarkup createInlineKeyboard(List<Pair<String, String>> buttons) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Pair<String, String> button : buttons) {
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(button.first());
            inlineKeyboardButton.setCallbackData(button.second());
            keyboard.add(List.of(inlineKeyboardButton));
        }

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }


}
