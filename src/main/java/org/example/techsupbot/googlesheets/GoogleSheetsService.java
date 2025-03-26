package org.example.techsupbot.googlesheets;


import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.example.techsupbot.DTO.Client;
import org.example.techsupbot.DTO.SheetId;
import org.example.techsupbot.data.RowColumn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Log4j2
@Component
@RequiredArgsConstructor
public class GoogleSheetsService {


    private SheetId SHEET_ID;
    private final Sheets sheetService;
    @Value("${google.spreadsheet_id}")
    private String SPREADSHEET_ID;

    @PostConstruct
    public void init() {
        this.SHEET_ID = findSheetIdByTitle("Таблица клиентов");
    }


    private SheetId findSheetIdByTitle(String title) {
        try {
            Spreadsheet spreadsheet = sheetService.spreadsheets().get(SPREADSHEET_ID).execute();
            List<Sheet> sheets = spreadsheet.getSheets();
            for (Sheet sheet : sheets) {
                if (title.equals(sheet.getProperties().getTitle())) {
                    log.info(String.format("ТАБЛИЦА С НАЗВАНИЕМ '%s' УЖЕ СУЩЕСТВУЕТ - ID: %d", title, sheet.getProperties().getSheetId()));
                    return new SheetId(sheet.getProperties().getSheetId(), title);
                }
            }
        } catch (IOException e) {
            log.info("ОШИБКА В МЕТОДЕ findSheetIdByTitle() {}", e.getMessage());
        }
        return createNewSheet(title);
    }

    private int generateSheetId() {
        Random random = new Random();
        int length = 3 + random.nextInt(5); // 7 + (0..4) = 7..11
        int min = (int) Math.pow(10, length - 1); // Минимальное значение для длины
        int max = (int) Math.pow(10, length) - 1; // Максимальное значение для длины
        return min + random.nextInt(max - min + 1);
    }

    private SheetId createNewSheet(String sheetTitle) {
        int sheetId = generateSheetId();
        AddSheetRequest addSheetViewRequest = new AddSheetRequest()
                .setProperties(new SheetProperties()
                        .setTitle(sheetTitle)
                        .setSheetId(sheetId)
                        .setGridProperties(new GridProperties()
                                .setColumnCount(40)));
        Request addSheet = new Request().setAddSheet(addSheetViewRequest);
        BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest();
        batchUpdateRequest.setRequests(List.of(addSheet));
        try {
            sheetService.spreadsheets().batchUpdate(SPREADSHEET_ID, batchUpdateRequest).execute();
            log.info(String.format("СОЗДАЛ И СОХРАНИЛ НОВУЮ ТАБЛИЦУ '%s', C ID %d", sheetTitle, sheetId));
        } catch (Exception e) {
            log.error(String.format("ОШИБКА ПРИ СОЗДАНИИ ТАБЛИЦЫ С ID %d - %s", sheetId, e.getMessage()));
        }
        return new SheetId(sheetId, sheetTitle);
    }

    private void cleanTable(SheetId sheetId) {

        ClearValuesRequest clearValuesRequest = new ClearValuesRequest();
        try {
            sheetService.spreadsheets().values()
                    .clear(SPREADSHEET_ID, String.format("%s!A:Z", sheetId.title()), clearValuesRequest)
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Очистка всех форматов
        BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest();
        Request clearFormatsRequest = new Request()
                .setRepeatCell(new RepeatCellRequest()
                        .setRange(new GridRange()
                                .setSheetId(sheetId.sheetId())
                                .setStartRowIndex(0)
                                .setEndRowIndex(100)
                                .setStartColumnIndex(0)
                                .setEndColumnIndex(30))
                        .setCell(new CellData()
                                .setUserEnteredFormat(new CellFormat()))
                        .setFields("userEnteredFormat"));
        batchUpdateRequest.setRequests(Collections.singletonList(clearFormatsRequest));
        try {
            sheetService.spreadsheets().batchUpdate(SPREADSHEET_ID, batchUpdateRequest).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void updateTable(ArrayList<Client> clientsList) {

        int rowCount = 2 + clientsList.size();
        int columnsCount = 1 + 8;
        // Очистка таблицы
        cleanTable(SHEET_ID);
        int sheetId = SHEET_ID.sheetId();
        // Создаем список запросов для пакетного обновления
        List<Request> requests = new ArrayList<>();


        // Установка ширины строк и столбцов
        requests.add(createDimensionUpdateRequest(sheetId, RowColumn.ROWS, 30, 1, 2));
        requests.add(createDimensionUpdateRequest(sheetId, RowColumn.COLUMNS, 200, 1, 2));
        requests.add(createDimensionUpdateRequest(sheetId, RowColumn.COLUMNS, 250, 2, columnsCount));


        // Форматирование всей таблицы
        requests.add(createCellStyleRequest(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(1)
                .setEndRowIndex(rowCount)
                .setStartColumnIndex(1)
                .setEndColumnIndex(columnsCount), getColorByHEX("#ffffff"), "CENTER", 13, false, 1));

        // ОФОРМЛЕНИЕ ОСНОВНЫХ ЯЧЕЕК
        requests.add(createCellStyleRequest(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(2)
                .setEndRowIndex(rowCount)
                .setStartColumnIndex(1)
                .setEndColumnIndex(columnsCount), getColorByHEX("#ffffff"), "CENTER", 13, false, 1));

        // Форматирование шапки
        requests.add(createCellStyleRequest(new GridRange()
                .setSheetId(sheetId)
                .setStartRowIndex(1)
                .setEndRowIndex(2)
                .setStartColumnIndex(1)
                .setEndColumnIndex(columnsCount), getColorByHEX("#ffff00"), "CENTER", 13, true, 2));


        // Создаем запрос для закрепления строк
        requests.add(new Request().setUpdateSheetProperties(new UpdateSheetPropertiesRequest()
                .setProperties(new SheetProperties()
                        .setSheetId(sheetId)
                        .setGridProperties(new GridProperties()
                                        .setFrozenRowCount(2)
                        ))
                .setFields("gridProperties.frozenRowCount")));

        //ПАКЕТНЫЙ ЗАПРОС НА ОБНОВЛЕНИЕ ЯЧЕЕК ТАБЛИЦЫ
        try {
            BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest().setRequests(requests);
            sheetService.spreadsheets().batchUpdate(SPREADSHEET_ID, batchUpdateRequest).execute();
        } catch (IOException e) {
            log.error(String.format("НЕ УДАЛОСЬ ПОСТРОИТЬ ПОЛЯ ТАБЛИЦЫ, ПРИЧИНА: %s", e.getMessage()));
        }


        List<ValueRange> data = new ArrayList<>();
        //ТИТУЛЬНИК
        data.add(new ValueRange()
                .setRange(String.format("%s!B2", SHEET_ID.title()))
                .setValues(List.of(List.of("Клиент", "Оформлял возврат", "Оценил сервис возврата", "Отзыв по сервису возврата", "Причина обращения", "Пользовался конструктором", "Оценил конструктор", "Отзыв по конструктору"))));

        for (int i = 0; i < clientsList.size(); i++) {
            Client client = clientsList.get(i);
            data.add(new ValueRange()
                    .setRange(String.format("%s!B%d", SHEET_ID.title(),i+3))
                    .setValues(List.of(List.of(
                            client.getUsername()!=null?client.getUsername():"-",
                            client.getUsedService()?"Да":"Нет",
                            client.getServiceMark()!=0?client.getServiceMark():"Не ставил оценку",
                            client.getServiceFeedback()!=null?client.getServiceFeedback():"Не оставил отзыв",
                            client.getDescription()!=null?client.getDescription():"Нет данных",
                            client.getUsedConstructor()?"Да":"Нет" ,
                            client.getConstructorMark()!=0?client.getConstructorMark():"Не ставил оценку",
                            client.getConstructorFeedback()!=null?client.getConstructorFeedback():"Не оставил отзыв"))));

        }
        //ЗАПРОС НА ПАКЕТНОЕ ОБНОВЛЕНИЕ ДАННЫХ ТАБЛИЦЫ
        batchUpdateValues(data, SHEET_ID);

    }

    private void batchUpdateValues(List<ValueRange> data, SheetId sheetId) {
        try {
            BatchUpdateValuesRequest batchRequest = new BatchUpdateValuesRequest()
                    .setValueInputOption("USER_ENTERED")
                    .setData(data);
            sheetService.spreadsheets().values()
                    .batchUpdate(SPREADSHEET_ID, batchRequest)
                    .execute();
        } catch (IOException e) {
            log.error(String.format("НЕ УДАЛОСЬ ВЫПОЛНИТЬ ОБНОВЛЕНИЕ ОСНОВНЫХ ТЕКСТОВЫХ ПОЛЕЙ ТАБЛИЦЫ '%s' ПО ПРИЧИНЕ: %s", sheetId.title(), e.getMessage()));
        }
    }

    private Request createDimensionUpdateRequest(int sheetId, RowColumn object, int width, int start, int end) {
        DimensionProperties properties = new DimensionProperties().setPixelSize(width);
        UpdateDimensionPropertiesRequest updateRowProperties = new UpdateDimensionPropertiesRequest()
                .setRange(new DimensionRange()
                        .setSheetId(sheetId)
                        .setDimension(object.getDescription())
                        .setStartIndex(start)
                        .setEndIndex(end))
                .setProperties(properties)
                .setFields("pixelSize");
        return new Request().setUpdateDimensionProperties(updateRowProperties);
    }

    private Request createCellStyleRequest(GridRange range, Color color, String textAlignment, int fontSize, boolean setBold, int borderWidth) {
        Border border = new Border().setStyle("SOLID").setWidth(borderWidth);
        Borders borders = new Borders().setTop(border).setBottom(border).setLeft(border).setRight(border);
        TextFormat textFormat = new TextFormat().setFontSize(fontSize).setBold(setBold);
        CellData cellData = new CellData()
                .setUserEnteredFormat(new CellFormat()
                        .setHorizontalAlignment(textAlignment)
                        .setBackgroundColor(color)
                        .setTextFormat(textFormat)
                        .setBorders(borders));
        RepeatCellRequest repeatCellRequest = new RepeatCellRequest()
                .setRange(range)
                .setFields("*")
                .setCell(cellData);
        return new Request().setRepeatCell(repeatCellRequest);
    }

    private Request createCellBordersRequest(GridRange range) {
        Border border = new Border().setStyle("SOLID").setWidth(2);
        UpdateBordersRequest updateBordersRequest = new UpdateBordersRequest()
                .setRange(range)
                .setBottom(border)
                .setTop(border)
                .setLeft(border)
                .setRight(border);
        return new Request().setUpdateBorders(updateBordersRequest);
    }

    private Color getColorByHEX(String hex) {
        int red = Integer.parseInt(hex.substring(1, 3), 16);
        int green = Integer.parseInt(hex.substring(3, 5), 16);
        int blue = Integer.parseInt(hex.substring(5, 7), 16);
        return new Color().setRed(red / 255f).setGreen(green / 255f).setBlue(blue / 255f);
    }


}
