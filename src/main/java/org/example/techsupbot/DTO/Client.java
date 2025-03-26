package org.example.techsupbot.DTO;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.example.techsupbot.data.ClientStatus;

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

    @Column(name = "username")
    String username;

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

    @Column(name = "service_mark")
    Byte serviceMark;

    @Column(name = "service_feedback")
    String serviceFeedback;

    @Column(name = "constructor_mark")
    Byte constructorMark;

    @Column(name = "constructor_feedback")
    String constructorFeedback;

}
