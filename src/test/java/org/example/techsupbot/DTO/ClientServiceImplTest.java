package org.example.techsupbot.DTO;


import org.example.techsupbot.data.ClientStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClientServiceImplTest {

    private static final long chatId = 12345678L;
    @Mock
    private ClientRepo clientRepo;
    @InjectMocks
    private ClientServiceImpl clientService;
    private Client testClient;

    @BeforeEach
    public void setUp() {
        testClient = new Client(chatId, "test_user123", null, "test_image", null, ClientStatus.SAVED, true, true, (byte) 5, "test_service_feedback", (byte) 4, "test_constructor_feedback");
    }

    @Test
    void saveClient_ShouldSaveNotNullClient() {
        clientService.saveClient(testClient);
        verify(clientRepo, times(1)).save(testClient);
    }

    @Test
    void saveClient_ShouldSaveNullClient() {
        when(clientRepo.save(null)).thenThrow(IllegalArgumentException.class);
        verify(clientRepo, never()).save(null);
        assertThrows(IllegalArgumentException.class, () -> clientService.saveClient(null));
    }

    @Test
    void findClient_ShouldReturnClient() {
        // Arrange
        when(clientRepo.findById(chatId)).thenReturn(Optional.of(testClient));
        // Act
        Client result = clientService.findByChatId(chatId);
        // Assert
        assertEquals(testClient, result);
    }

    @Test
    void findClient_ShouldReturnDefaultClient() {
        // Arrange
        long notExistsId = 123L;
        Client defaultClient = new Client(notExistsId, null, null, null, null, ClientStatus.SAVED, false, false, (byte) 0, null, (byte) 0, null);
        when(clientRepo.findById(notExistsId)).thenReturn(Optional.empty());
        // Act
        Client result = clientService.findByChatId(notExistsId);
        // Assert
        assertEquals(defaultClient, result);
        assertNotNull(result);
    }

    @Test
    void deleteClient_ShouldDeleteClient() {
        clientService.deleteClientByChatId(chatId);
        verify(clientRepo, times(1)).deleteById(chatId);
    }

    @Test
    void deleteClient_ShouldThrowException() {
        doThrow(IllegalArgumentException.class).when(clientRepo).deleteById(null);

        assertThrows(IllegalArgumentException.class, () -> clientService.deleteClientByChatId(null));
        verify(clientRepo, times(1)).deleteById(null);
    }


    @Test
    void getAllClients_ShouldReturnAllClients() {
        when(clientRepo.findAll()).thenReturn(List.of(testClient));

        List<Client> result = clientService.getAllClients();

        assertEquals(List.of(testClient), result);
    }

    @Test
    void getAllClients_ShouldReturnEmptyList() {
        when(clientRepo.findAll()).thenReturn(List.of());

        List<Client> result = clientService.getAllClients();

        assertEquals(List.of(), result);
    }
}
