package org.example.techsupbot.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.techsupbot.DTO.ClientService;
import org.example.techsupbot.googlesheets.GoogleSheetsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Log4j2
@Component
@RequiredArgsConstructor
public class ScheduleService {
    int count = 0;
    TechSupBot bot;
    private final ClientService clientService;
   private final GoogleSheetsService googleSheetsService;
    public void init(TechSupBot bot) {
        this.bot = bot;
    }


    @Scheduled(cron = "0 0 13 * * ?", zone = "Europe/Moscow")
    private void runDailyTask() {
        googleSheetsService.updateTable(clientService.getAllClients());
        SendMessage message = new SendMessage();
        message.setChatId(889218535L);
        message.setText("Хозяин, я еще жив! Таблицу твою обновил. Дней без перезагрузки - "+count);
        try {
            bot.execute(message);
            count++;
        } catch (TelegramApiException e) {
            log.error("НЕ УДАЛОСЬ ОТПРАВИТЬ СООБЩЕНИЕ В 13.00, ПРИЧИНА - {}", e.getMessage());
        }
    }
}
