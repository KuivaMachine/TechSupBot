package org.example.techsupbot;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.techsupbot.DTO.Client;
import org.example.techsupbot.DTO.ClientService;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageHandler {

    private final Long managerChatId = 889218535L;
    private TechSupBot telegram;
    final ClientService clientService;

    public void registerBot(TechSupBot telegram) {
        this.telegram = telegram;
    }

    public SendMessage processMessage(Long chatId, Message update) {
        String text = update.getText();
        Client currentclient = clientService.findByChatId(chatId);
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(chatId.toString());
        if (text.equals("/start")) {
            return sendWelcomeMessage(message);
        }
        if (text.equals("/delete_me")) {
            clientService.deleteClientByChatId(chatId);
            message.setText("Клиент удален");
            message.setReplyMarkup(new ReplyKeyboardRemove(true));
            return message;
        }
        if (text.equals(ButtonLabels.CANCEL.getLabel())) {
            return cancelProcess(currentclient, message);
        }
        if (text.equals(ButtonLabels.ATTACH_IMAGE.getLabel())) {
            return addImageProcess(currentclient, message);
        }
        if (text.equals(ButtonLabels.ATTACH_SCREEN.getLabel())) {
            return addsScreenProcess(currentclient, message);
        }
        if (text.equals(ButtonLabels.ATTACH_DESCRIPTION.getLabel())) {
            return addDescriptionProcess(currentclient, message);
        }
        if (currentclient.getStatus().equals(ClientStatus.WAITING_DESCRIPTION)) {
            currentclient.setDescription(text);
            clientService.saveClient(currentclient);
            return fillReturnDataProcess(currentclient, message);
        }
        if (currentclient.getStatus().equals(ClientStatus.WAITING_BAD_FEEDBACK)) {
            //TODO: КУДА ТО ОТПРАВИТЬ ФИДБЕК ПО СЕРВИСУ
            message.setText("Спасибо за Ваш отзыв!");
            currentclient.setStatus(ClientStatus.SAVED);
            clientService.saveClient(currentclient);
            return message;
        }
        if (currentclient.getStatus().equals(ClientStatus.WAITING_BAD_FEEDBACK_CONSTRUCTOR)) {
            //TODO: КУДА ТО ОТПРАВИТЬ ФИДБЕК ПО КОНСТРУКТОРУ
            message.setText("Спасибо за Ваш отзыв!");
            currentclient.setStatus(ClientStatus.SAVED);
            clientService.saveClient(currentclient);
            return message;
        }
        if (currentclient.getStatus().equals(ClientStatus.ORDER_QUESTION)) {
            return sendOrderQuestionProcess(update, message, currentclient);
        }
        if (currentclient.getStatus().equals(ClientStatus.WAITING_IMAGE)) {
            message.setText("Чтобы добавить фото товара, воспользуйтесь клавиатурой ниже 👇\nСначала нажмите кнопку \"Прикрепить фото\", а затем отправьте фотографию.");
            return message;
        }
        if (currentclient.getStatus().equals(ClientStatus.WAITING_SCREEN)) {
            message.setText("Чтобы добавить скриншот личного кабинета, воспользуйтесь клавиатурой ниже 👇\nСначала нажмите кнопку \"Прикрепить скрин\", а затем отправьте скриншот.");
            return message;
        }
        if (currentclient.getStatus().equals(ClientStatus.WRONG_ITEM)) {
            message.setText("Чтобы оформить заявку, воспользуйтесь клавиатурой ниже 👇\nСначала нажмите кнопку, а затем введите нужные данные)");
            return message;
        }
        if (text.equals(ButtonLabels.SEND.getLabel()) && currentclient.getStatus().equals(ClientStatus.WAITING_SEND)) {
            return sendDataToManager(currentclient, message);
        }
        if (text.equals(ButtonLabels.MAIN_MENU.getLabel())) {
            return sendWelcomeMessage( message);
        }
        return message;
    }

    public SendMessage processCallback(Long chatId, String data) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        Client currentclient = clientService.findByChatId(chatId);
        return switch (data) {
            case "service_support" -> sendServiceSupportMessage(currentclient, message);
            case "wrong_item" -> sendWrongItemInstructions(currentclient, message);
            case "damaged_item" -> sendDamagedItemInstructions(currentclient, message);
            case "order_questions" -> sendOrderQuestionsMessage(currentclient, message);
            case "create_case" -> sendCreateCaseMessage(currentclient, message);
            case "join_group" -> sendJoinGroupMessage(chatId, message);
            case "promotions" -> sendPromotionsMessage(chatId, message);
            case "help_choice" -> sendHelpChoiceMessage(chatId, message);
            case "5_stars", "4_stars" -> sendGoodAnswer(currentclient, message);
            case "3_stars", "2_stars", "1_stars" -> sendBadAnswer(currentclient, message);
            case "5_stars_constructor", "4_stars_constructor" -> sendGoodAnswerToConstructor(currentclient, message);
            case "3_stars_constructor", "2_stars_constructor", "1_stars_constructor" -> sendBadAnswerToConstructor(currentclient, message);
            default -> {
                message.setText(data);
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

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            sendServiceQualityMessage(currentClient);
        }, 24, TimeUnit.HOURS);
        scheduler.shutdown();

    }

    public void startTimerByCaseConstructor(Client currentClient) {

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
        try {
            telegram.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
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
        try {
            telegram.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private SendMessage sendBadAnswer(Client currentclient, SendMessage message) {
        if (currentclient.getStatus().equals(ClientStatus.WAITING_SERVICE_QUALITY)) {
            message.setText("""
                    🙏 Спасибо за честный отзыв!
                    Нам очень жаль, что мы не оправдали ваших ожиданий.
                    Пожалуйста, напишите, что именно нам стоит улучшить. Ваше мнение поможет нам стать лучше! 🙌
                    """);
            currentclient.setStatus(ClientStatus.WAITING_BAD_FEEDBACK);
            clientService.saveClient(currentclient);
            return message;
        }
        message.setText("Эта кнопка неактивна в данный момент)");
        return message;
    }
    private SendMessage sendBadAnswerToConstructor(Client currentclient, SendMessage message) {
        if (currentclient.getStatus().equals(ClientStatus.WAITING_CONSTRUCTOR_QUALITY)) {
            message.setText("""
                    🙏 Спасибо за честный отзыв!
                    Нам очень жаль, что наш конструктор не оправдал ваших ожиданий.
                    Пожалуйста, напишите, что именно нам стоит улучшить. Ваше мнение поможет нам стать лучше!
                    """);
            currentclient.setStatus(ClientStatus.WAITING_BAD_FEEDBACK_CONSTRUCTOR);
            clientService.saveClient(currentclient);
            return message;
        }
        message.setText("Эта кнопка неактивна в данный момент)");
        return message;
    }
    private SendMessage sendGoodAnswer(Client currentclient, SendMessage message) {
        //TODO:КУДА ТО ОТПРАВИТЬ ЭТИ ЗВЕЗДЫ
        message.setText("""
                🎉 Спасибо за высокую оценку!
                Мы рады, что смогли вам помочь. Будем и дальше стараться радовать вас качественным сервисом!
                """);
        currentclient.setStatus(ClientStatus.SAVED);
        clientService.saveClient(currentclient);
        return message;
    }

    private SendMessage sendGoodAnswerToConstructor(Client currentclient, SendMessage message) {
        //TODO:КУДА ТО ОТПРАВИТЬ ЭТИ ЗВЕЗДЫ
        message.setText("""
                🎉 Спасибо за высокую оценку!
                Мы рады, что вам понравилось создавать чехол с нами.
                Ждем вас снова за новыми уникальными дизайнами!
                """);
        currentclient.setStatus(ClientStatus.SAVED);
        clientService.saveClient(currentclient);
        return message;
    }
    private SendMessage sendOrderQuestionProcess(Message update, SendMessage message, Client currentClient) {
        SendMessage order = new SendMessage();
        String question = String.format("Вопрос по заказу от пользователя @%s:\n", update.getFrom().getUserName());
        order.setChatId(managerChatId);
        order.setText(question + update.getText());
        try {
            telegram.execute(order);
            message.setText("Спасибо! Менеджер свяжется с Вами в ближайшее время для уточнения деталей.");
            message.setReplyMarkup(createReplyKeyboard(List.of(new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.MAIN_MENU.getLabel()))))));
            currentClient.setStatus(ClientStatus.SAVED);
            clientService.saveClient(currentClient);
            if(!currentClient.getGivenServiceFeedback()){
                startTimerByServiceSupport(currentClient);
                currentClient.setGivenServiceFeedback(true);
                clientService.saveClient(currentClient);
            }
            return message;
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
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
        screen.setCaption(currentclient.getDescription());
        media.setMedias(List.of(image, screen));
        media.setChatId(managerChatId);
        try {
            telegram.execute(media);
            message.setText("Мы передали ваш запрос менеджеру. В ближайшее время с вами свяжутся для уточнения деталей.");
            currentclient.setStatus(ClientStatus.SENT);
            clientService.saveClient(currentclient);
            message.setReplyMarkup(createReplyKeyboard(List.of(new KeyboardRow(List.of(new KeyboardButton(ButtonLabels.MAIN_MENU.getLabel()))))));
            startTimerByServiceSupport(currentclient);
            return message;
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private SendMessage addImageProcess(Client currentclient, SendMessage message) {
        currentclient.setStatus(ClientStatus.WAITING_IMAGE);
        clientService.saveClient(currentclient);
        message.setText("""
                Отправьте фото товара:
                """);
        return message;
    }


    private SendMessage sendWelcomeMessage(SendMessage message) {
        message.setText("Главное меню");
        message.setReplyMarkup(new ReplyKeyboardRemove(true));
        try {
            telegram.execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        message.setText("""
                👋 Привет! Добро пожаловать в MustHaveCase!
                Мы рады, что вы с нами! Здесь вы найдете стильные, надежные и уникальные чехлы для вашего телефона. А еще мы всегда готовы помочь с выбором или решить любой вопрос.
                
                Что я могу для вас сделать?
                - 🛠️ Помочь с сервисной поддержкой, если что-то пошло не так.
                - 🎨 Помогу создать индивидуальный чехол.
                - 💬 Пригласить в нашу группу, где вы найдете акции, новинки и общение с другими клиентами.
                - 🛒 Рассказать о текущих акциях и скидках.
                
                Просто выберите нужную кнопку ниже, и я помогу вам! 😊""");
        message.setReplyMarkup(createInlineKeyboard(
                List.of(
                        new Pair<>("🛠️ Сервисная поддержка", "service_support"),
                        new Pair<>("🎨 Создать индивидуальный чехол", "create_case"),
                        new Pair<>("💬 Вступить в группу", "join_group"),
                        new Pair<>("🛒 Акции и скидки", "promotions"),
                        new Pair<>("❓ Помощь в выборе", "help_choice")
                )
        ));
        return message;
    }

    private SendMessage sendServiceSupportMessage(Client currentClient, SendMessage message) {
        currentClient.setStatus(ClientStatus.SERVICE_SUPPORT);
        clientService.saveClient(currentClient);
        message.setText("Спасибо, что обратились к нам! Пожалуйста, опишите вашу проблему, мы постараемся помочь вам как можно быстрее!");
        message.setReplyMarkup(createInlineKeyboard(
                List.of(
                        new Pair<>("Я получил не тот товар или дизайн", "wrong_item"),
                        new Pair<>("Товар поврежден или бракован", "damaged_item"),
                        new Pair<>("Есть вопросы по заказу", "order_questions")
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
        currentClient.setStatus(ClientStatus.ORDER_QUESTION);
        clientService.saveClient(currentClient);
        return message;
    }

    private SendMessage sendCreateCaseMessage(Client currentClient, SendMessage message) {
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
        if(!currentClient.getGivenConstructorFeedback()){
            startTimerByCaseConstructor(currentClient);
            currentClient.setGivenConstructorFeedback(true);
            clientService.saveClient(currentClient);
        }
        return message;
    }

    private SendMessage sendJoinGroupMessage(Long chatId, SendMessage message) {

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
        //TODO: СДЕЛАТЬ ЛОГИКУ ОТВЕТА НА КНОПКУ "АКЦИИ И СКИДКИ"

        message.setChatId(chatId);
        message.setText("СДЕЛАТЬ ЛОГИКУ ОТВЕТА НА КНОПКУ \"АКЦИИ И СКИДКИ\"");
        return message;
    }

    private SendMessage sendHelpChoiceMessage(Long chatId, SendMessage message) {
        //TODO: СДЕЛАТЬ ЛОГИКУ ОТВЕТА НА КНОПКУ "ПОМОЩЬ В ВЫБОРЕ"

        message.setChatId(chatId);
        message.setText("СДЕЛАТЬ ЛОГИКУ ОТВЕТА НА КНОПКУ \"ПОМОЩЬ В ВЫБОРЕ\"");
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

    private InlineKeyboardMarkup createInlineKeyboard(List<Pair<String, String>> buttons) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        for (Pair<String, String> button : buttons) {
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(button.getFirst());
            inlineKeyboardButton.setCallbackData(button.getSecond());
            keyboard.add(List.of(inlineKeyboardButton));
        }

        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }


}
