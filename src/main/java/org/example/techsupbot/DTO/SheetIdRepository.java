package org.example.techsupbot.DTO;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SheetIdRepository extends JpaRepository<SheetId, Long> {
    Optional<SheetId> findByTitle(String title);
}