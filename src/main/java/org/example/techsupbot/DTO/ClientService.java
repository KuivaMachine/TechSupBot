package org.example.techsupbot.DTO;

import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public interface ClientService {
    void saveClient(Client client);
    Client findByChatId(long chatId);
    void deleteClientByChatId(Long chatId);
    ArrayList<Client> getAllClients();
}
