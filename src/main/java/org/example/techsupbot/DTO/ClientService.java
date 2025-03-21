package org.example.techsupbot.DTO;

import org.springframework.stereotype.Component;

@Component
public interface ClientService {
    void saveClient(Client client);
    Client findByChatId(long chatId);
    void deleteClientByChatId(Long chatId);
}
