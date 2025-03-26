package org.example.techsupbot.DTO;


public interface SheetIdService {
    void saveSheetId(SheetId sheetId);
    SheetId findByTitle(String title);
}
