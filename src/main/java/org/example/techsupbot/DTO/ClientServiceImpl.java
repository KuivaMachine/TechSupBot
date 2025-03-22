package org.example.techsupbot.DTO;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.example.techsupbot.ClientStatus;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClientServiceImpl implements ClientService {

    ClientRepo clientRepo;
    @Override
    public void saveClient(Client client) {
            clientRepo.save(client);
    }

    @Override
    public Client findByChatId(long chatId) {
        return clientRepo.findById(chatId).orElse(new Client(chatId, null, null, null, ClientStatus.SAVED,false,false));
    }

    @Override
    public void deleteClientByChatId(Long chatId) {
        clientRepo.deleteById(chatId);
    }

}
