package secure.canal.campaigns.reports;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.InputStreamResource;
import secure.canal.campaigns.entity.SmsMessages;
import secure.canal.campaigns.exception.ApiException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.stream.IntStream.range;
import static org.apache.commons.lang3.time.DateFormatUtils.format;

@Slf4j
public class SmsMessageReport {

    public static final String DATE_FORMATTER = "yyyy-MM-dd hh:mm:ss";
    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private List<SmsMessages> smsMessages;
    private static String[] HEADERS = { "ID", "Messages", "Contact", "Date", "Status" };

    public SmsMessageReport(List<SmsMessages> smsMessages) {
        this.smsMessages = smsMessages;
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("SmsMessages");
        setHeaders();
    }

    private void setHeaders() {
        Row headerRow = sheet.createRow(0);
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight(14);
        style.setFont(font);
        range(0, HEADERS.length).forEach(index -> {
            Cell cell = headerRow.createCell(index);
            cell.setCellValue(HEADERS[index]);
            cell.setCellStyle(style);
        });
    }

    public InputStreamResource export() {
        return generateReport();
    }

    private InputStreamResource generateReport() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle style = workbook.createCellStyle();
            XSSFFont font = workbook.createFont();
            font.setFontHeight(10);
            style.setFont(font);
            int rowIndex = 1;
            for(SmsMessages smsMessages1: smsMessages) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(smsMessages1.getId());
                row.createCell(1).setCellValue(smsMessages1.getMessage());
                row.createCell(2).setCellValue(smsMessages1.getRecipientNumber());
                String formattedDate = smsMessages1.getSentAt().format(DateTimeFormatter.ofPattern(DATE_FORMATTER));
                row.createCell(3).setCellValue(formattedDate);
                row.createCell(4).setCellValue(smsMessages1.getStatus());
            }
            workbook.write(out);
            return new InputStreamResource(new ByteArrayInputStream(out.toByteArray()));
        } catch (Exception exception) {
            log.error(exception.getMessage());
            throw new ApiException("Unable to export report file");
        }
    }






}
