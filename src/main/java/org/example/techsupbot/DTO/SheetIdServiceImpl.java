package org.example.techsupbot.DTO;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Log4j2
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SheetIdServiceImpl implements SheetIdService {


    SheetIdRepository sheetIdRepository;

    @Override
    public void saveSheetId(SheetId sheetId) {
        sheetIdRepository.save(sheetId);
        log.info("SAVED SHEET ID " + sheetId);
    }

    @Override
    public SheetId findByTitle(String title) {
        Optional<SheetId> sheetId = sheetIdRepository.findByTitle(title);
        if (sheetId.isPresent()) {
            return sheetId.get();
        }else{
            log.error("SHEET ID DID NOT FOUND by title " + title);
            return null;
        }
    }

}
