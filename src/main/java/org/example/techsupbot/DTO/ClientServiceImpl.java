package org.example.techsupbot.DTO;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.example.techsupbot.data.ClientStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Log4j2
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClientServiceImpl implements ClientService {
    ClientRepo clientRepo;

    @Override
    public void saveClient(Client client) {
        clientRepo.save(client);
        log.info(String.format("CLIENT (%d) STATUS - %s", client.getChatId(), client.getStatus()));
    }
    @Override
    public Client findByChatId(long chatId) {
        return clientRepo.findById(chatId).orElse(new Client(chatId,null,null, ClientStatus.SAVED,false,false, (byte) 0,null, (byte) 0,null));
    }

    @Override
    public void deleteClientByChatId(Long chatId) {
        clientRepo.deleteById(chatId);
    }

    @Override
    public ArrayList<Client> getAllClients() {
        return new ArrayList<>(clientRepo.findAll());
    }

}
