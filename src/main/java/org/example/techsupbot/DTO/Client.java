package org.example.techsupbot.DTO;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.techsupbot.ClientStatus;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Client {

    @Id
    @Column(name = "chat_id")
    Long chatId;

    @Column(name = "screenshot")
    String screenshot;

    @Column(name = "image")
    String image;

    @Column(name="description")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    ClientStatus status;

    @Column(name="usedConstructor")
    Boolean usedConstructor;

    @Column(name="usedService")
    Boolean usedService;


}
