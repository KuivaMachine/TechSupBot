package org.example.techsupbot.redis;

import org.springframework.stereotype.Controller;
import redis.clients.jedis.Jedis;

@Controller
public class RedisService {
    private static final String REDIS_HOST = "127.0.0.1";  // Имя контейнера Redis
    private static final int REDIS_PORT = 6379;
    private static final String LAST_MESSAGE_KEY_PREFIX = "last_message_id:";  // Префикс для ключей

    private final Jedis jedis;

    public RedisService() {
        this.jedis = new Jedis(REDIS_HOST, REDIS_PORT);
    }

    // Сохраняем ID последнего сообщения для пользователя
    public void saveLastMessageId(Long chatId, Integer messageId) {
        try {
            String key = LAST_MESSAGE_KEY_PREFIX + chatId;
            jedis.set(key, messageId.toString());
        } finally {
            close();
        }
    }

    // Получаем ID последнего сообщения для пользователя
    public String getLastMessageId(Long chatId) {
        try {
            String key = LAST_MESSAGE_KEY_PREFIX + chatId;
            return jedis.get(key);
        } finally {
            close();
        }
    }

    // Закрываем соединение с Redis
    public void close() {
        jedis.close();
    }
}