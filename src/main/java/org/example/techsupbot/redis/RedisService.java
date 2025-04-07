package org.example.techsupbot.redis;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisDataException;

import java.time.Instant;

@Log4j2
@Controller
public class RedisService {
    @Value("${redis.host}")
    private static  String REDIS_HOST;
    @Value("${redis.port}")
    private static  int REDIS_PORT;
    private static final String LAST_MESSAGE_KEY_PREFIX = "last_message:";

    // Время жизни записи в Redis (48 часов в секундах)
    private static final int MESSAGE_TTL = 48 * 60 * 60;

    private final Jedis jedis;

    public RedisService() {
        this.jedis = new Jedis(REDIS_HOST, REDIS_PORT);
    }

    // Сохраняем ID и время последнего сообщения для чата
    public void saveLastMessageInfo(Long chatId, Integer messageId) {
        try {
            String key = LAST_MESSAGE_KEY_PREFIX + chatId;

            // Используем хэш для хранения нескольких полей
            jedis.hset(key, "messageId", messageId.toString());
            jedis.hset(key, "timestamp", Instant.now().toString());

            // Устанавливаем время жизни записи
            jedis.expire(key, MESSAGE_TTL);
        } catch (JedisDataException e){
            log.error("НЕ УДАЛОСЬ ЗАПИСАТЬ ДАННЫЕ В REDIS (read-only mode). Message: {}", e.getMessage());
        }finally {
            close();
        }
    }

    // Получаем ID последнего сообщения для чата
    public Integer getLastMessageId(Long chatId) {
        try {
            String key = LAST_MESSAGE_KEY_PREFIX + chatId;
            String messageId = jedis.hget(key, "messageId");
            return messageId != null ? Integer.parseInt(messageId) : null;
        } finally {
            close();
        }
    }

    // Получаем время последнего сообщения для чата
    public Instant getLastMessageTime(Long chatId) {
        try {
            String key = LAST_MESSAGE_KEY_PREFIX + chatId;
            String timestamp = jedis.hget(key, "timestamp");
            return timestamp != null ? Instant.parse(timestamp) : null;
        } finally {
            close();
        }
    }


    // Проверяем, можно ли еще удалить сообщение (не прошло 48 часов)
    public boolean isMessageDeletable(Long chatId) {
        Instant messageTime = getLastMessageTime(chatId);
        if (messageTime == null) {
            return false;
        }
        return messageTime.plusSeconds(MESSAGE_TTL).isAfter(Instant.now());
    }

    // Закрываем соединение с Redis
    public void close() {
        jedis.close();
    }

    // Вложенный класс для хранения информации о сообщении
    @Getter
    public static class MessageInfo {
        private final Integer messageId;
        private final Instant timestamp;

        public MessageInfo(Integer messageId, Instant timestamp) {
            this.messageId = messageId;
            this.timestamp = timestamp;
        }

    }
}