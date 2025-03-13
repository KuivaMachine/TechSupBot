package org.example.techsupbot;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;
import java.util.List;


@Component
public class TechSupBot extends TelegramLongPollingBot {

    TelegramBotConfig config;

    public TechSupBot(TelegramBotConfig config) {
        super(config.getToken());
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getBotUsername() {
        return "MustHaveCase_bot.";
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage().getChatId(), update.getMessage().getText());
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery().getMessage().getChatId(), update.getCallbackQuery().getData());
        }
    }

    private void handleMessage(Long chatId, String text) {
        if ("/start".equals(text)) {
            sendWelcomeMessage(chatId);
        } else {
            sendMessage(chatId, "Пожалуйста, используйте кнопки для взаимодействия.");
        }
    }

    private void handleCallbackQuery(Long chatId, String data) {
        switch (data) {
            case "service_support":
                sendServiceSupportMessage(chatId);
                break;
            case "wrong_item":
                sendWrongItemInstructions(chatId);
                break;
            case "damaged_item":
                sendDamagedItemInstructions(chatId);
                break;
            case "order_questions":
                sendOrderQuestionsMessage(chatId);
                break;
            case "create_case":
                sendCreateCaseMessage(chatId);
                break;
            case "join_group":
                sendJoinGroupMessage(chatId);
                break;
            case "promotions":
                sendPromotionsMessage(chatId);
                break;
            case "help_choice":
                sendHelpChoiceMessage(chatId);
                break;
            default:
                sendMessage(chatId, "Неизвестная команда.");
        }
    }

    private void sendMessage(Long chatId, String s) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(s);
        executeMessage(message);
    }

    private void sendWelcomeMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("👋 Привет! Добро пожаловать в MustHaveCase!\n" +
                "Мы рады, что вы с нами! Здесь вы найдете стильные, надежные и уникальные чехлы для вашего телефона. А еще мы всегда готовы помочь с выбором или решить любой вопрос.\n" +
                "Что я могу для вас сделать?");
        message.setReplyMarkup(createInlineKeyboard(
                List.of(
                        new Pair<>("🛠️ Сервисная поддержка", "service_support"),
                        new Pair<>("🎨 Создать индивидуальный чехол", "create_case"),
                        new Pair<>("💬 Вступить в группу", "join_group"),
                        new Pair<>("🛒 Акции и скидки", "promotions"),
                        new Pair<>("❓ Помощь в выборе", "help_choice")
                )
        ));
        executeMessage(message);
    }

    private void sendServiceSupportMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Спасибо, что обратились к нам! Пожалуйста, опишите вашу проблему, мы постараемся помочь вам как можно быстрее!");
        message.setReplyMarkup(createInlineKeyboard(
                List.of(
                        new Pair<>("Я получил не тот товар или дизайн", "wrong_item"),
                        new Pair<>("Товар поврежден или бракован", "damaged_item"),
                        new Pair<>("Есть вопросы по заказу", "order_questions")
                )
        ));
        executeMessage(message);
    }

    private void sendWrongItemInstructions(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Мы очень сожалеем, что Вы получили не тот товар или дизайн. Давайте решим эту проблему как можно быстрее!\n\n" +
                "Пожалуйста, выполните следующие шаги:\n" +
                "1. 📸 Сфотографируйте товар и пришлите фото нам (это поможет быстрее разобраться в ситуации).\n" +
                "2. 📝 Пришлите скрин из личного кабинета, что б было видно заказ.\n" +
                "3. 📦 Сохраните упаковку и товар в том виде, в котором вы его получили.\n\n" +
                "Как только мы получим эту информацию, мы начнем процесс замены или возврата.");
        executeMessage(message);
    }

    private void sendDamagedItemInstructions(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Мы очень сожалеем, что Вы получили бракованный или поврежденный товар. Давайте решим эту проблему как можно быстрее!\n\n" +
                "Пожалуйста, выполните следующие шаги:\n" +
                "1. 📸 Сфотографируйте товар и пришлите фото нам (это поможет быстрее разобраться в ситуации).\n" +
                "2. 📝 Пришлите скрин из личного кабинета, что б было видно заказ.\n" +
                "3. 📦 Сохраните упаковку и товар в том виде, в котором вы его получили.\n\n" +
                "Как только мы получим эту информацию, мы начнем процесс замены или возврата.");
        executeMessage(message);
    }

    private void sendOrderQuestionsMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Спасибо, что обратились к нам! Пожалуйста, опишите ваш вопрос:");
        executeMessage(message);
    }

    private void sendCreateCaseMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Мы рады, что вы хотите создать что-то уникальное! Нажмите на кнопку ниже, и вы перейдете на наш сайт, где сможете:\n" +
                "- Выбрать модель телефона.\n" +
                "- Загрузить свой дизайн или выбрать из готовых вариантов.\n" +
                "- Добавить текст, изображения или логотипы.\n" +
                "- Увидеть предварительный результат в режиме реального времени.\n\n" +
                "Это просто, быстро и увлекательно!\n\n" +
                "👇 Нажмите здесь, чтобы начать:\n" +
                "[Создать индивидуальный чехол](https://вашсайт.com/конструктор)");
        message.setParseMode("Markdown");
        executeMessage(message);
    }

    private void sendJoinGroupMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Присоединяйтесь к нашей группе, чтобы быть в курсе акций, новинок и общаться с другими клиентами:\n" +
                "[Наша группа](https://t.me/yourgroup)");
        message.setParseMode("Markdown");
        executeMessage(message);
    }

    private void sendPromotionsMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Текущие акции и скидки:\n" +
                "- Скидка 10% на все чехлы до конца месяца!\n" +
                "- Бесплатная доставка при заказе от 2000 рублей.\n" +
                "- Специальные предложения для новых клиентов.");
        executeMessage(message);
    }

    private void sendHelpChoiceMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Если вам нужна помощь в выборе чехла, пожалуйста, опишите, что вы ищете, и мы поможем вам подобрать идеальный вариант!");
        executeMessage(message);
    }

    private InlineKeyboardMarkup createInlineKeyboard(List<Pair<String, String>> buttons) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (Pair<String, String> button : buttons) {
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setText(button.getFirst());
            inlineKeyboardButton.setCallbackData(button.getSecond());
            row.add(inlineKeyboardButton);
        }
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


}

class Pair<K, V> {
    private K first;
    private V second;

    public Pair(K first, V second) {
        this.first = first;
        this.second = second;
    }

    public K getFirst() {
        return first;
    }

    public V getSecond() {
        return second;
    }
}
