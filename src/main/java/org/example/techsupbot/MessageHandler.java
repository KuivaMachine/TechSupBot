package org.example.techsupbot;


import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Controller
public class MessageHandler {


    public SendMessage processCallback(Long chatId, String data) {

        return switch (data) {
            case "service_support" -> sendServiceSupportMessage(chatId);
            case "wrong_item" -> sendWrongItemInstructions(chatId);
            case "damaged_item" -> sendDamagedItemInstructions(chatId);
            case "order_questions" -> sendOrderQuestionsMessage(chatId);
            case "create_case" -> sendCreateCaseMessage(chatId);
            case "join_group" -> sendJoinGroupMessage(chatId);
            case "promotions" -> sendPromotionsMessage(chatId);
            case "help_choice" -> sendHelpChoiceMessage(chatId);
            default -> {
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(data);
                yield message;
            }
        };
    }

    public SendMessage processMessage(Long chatId, String text) {
        if ("/start".equals(text)) {
            return sendWelcomeMessage(chatId);
        } else {
            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(text);
            return message;
        }
    }



    private SendMessage sendWelcomeMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("""
                👋 Привет! Добро пожаловать в MustHaveCase! \s
                                           
                                           Мы рады, что вы с нами! Здесь вы найдете стильные, надежные и уникальные чехлы для вашего телефона. А еще мы всегда готовы помочь с выбором или решить любой вопрос. \s
                                           
                                           Что я могу для вас сделать? \s
                                           - 🛠️ Помочь с сервисной поддержкой, если что-то пошло не так. \s
                                           - 🎨 Помогу создать индивидуальный чехол. \s
                                           - 💬 Пригласить в нашу группу, где вы найдете акции, новинки и общение с другими клиентами. \s
                                           - 🛒 Рассказать о текущих акциях и скидках. \s
                                           
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

    private SendMessage sendServiceSupportMessage(Long chatId) {
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
        return message;
    }

    private SendMessage sendWrongItemInstructions(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Мы очень сожалеем, что Вы получили не тот товар или дизайн. Давайте решим эту проблему как можно быстрее!\n\n" +
                "Пожалуйста, выполните следующие шаги:\n" +
                "1. 📸 Сфотографируйте товар и пришлите фото нам (это поможет быстрее разобраться в ситуации).\n" +
                "2. 📝 Пришлите скрин из личного кабинета, что б было видно заказ.\n" +
                "3. 📦 Сохраните упаковку и товар в том виде, в котором вы его получили.\n\n" +
                "Как только мы получим эту информацию, мы начнем процесс замены или возврата.");
        return message;
    }

    private SendMessage sendDamagedItemInstructions(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Мы очень сожалеем, что Вы получили бракованный или поврежденный товар. Давайте решим эту проблему как можно быстрее!\n\n" +
                "Пожалуйста, выполните следующие шаги:\n" +
                "1. 📸 Сфотографируйте товар и пришлите фото нам (это поможет быстрее разобраться в ситуации).\n" +
                "2. 📝 Пришлите скрин из личного кабинета, что б было видно заказ.\n" +
                "3. 📦 Сохраните упаковку и товар в том виде, в котором вы его получили.\n\n" +
                "Как только мы получим эту информацию, мы начнем процесс замены или возврата.");
        return message;
    }

    private SendMessage sendOrderQuestionsMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Спасибо, что обратились к нам! Пожалуйста, опишите ваш вопрос:");
        return message;
    }

    private SendMessage sendCreateCaseMessage(Long chatId) {
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
        return message;
    }

    private SendMessage sendJoinGroupMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("""
                🎉 Отлично! 🎉 \s
                Спасибо, что решили вступить в нашу группу! Мы рады видеть вас среди наших участников. \s
                                
                👉 Чтобы завершить процесс, просто перейдите по ссылке: https://t.me/MustHaveCase и нажмите кнопку "Подписаться". \s
                                
                Добро пожаловать в нашу дружную команду! 🚀
                """);
        message.setParseMode("Markdown");

        return message;
    }

    private SendMessage sendPromotionsMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Текущие акции и скидки:\n" +
                "- Скидка 10% на все чехлы до конца месяца!\n" +
                "- Бесплатная доставка при заказе от 2000 рублей.\n" +
                "- Специальные предложения для новых клиентов.");
        return message;
    }

    private SendMessage sendHelpChoiceMessage(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Если вам нужна помощь в выборе чехла, пожалуйста, опишите, что вы ищете, и мы поможем вам подобрать идеальный вариант!");
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
}
