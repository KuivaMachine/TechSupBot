package org.example.techsupbot.bot;


import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bot")
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TelegramBotConfig {
    String token;
    String url;


}
