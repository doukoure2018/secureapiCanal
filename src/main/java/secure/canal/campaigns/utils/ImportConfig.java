package secure.canal.campaigns.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import secure.canal.campaigns.payload.ImportFileDto;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportConfig {

    private int sheetIndex;

    private int headerIndex;

    private int startRow;

    private Class dataClazz;

    private List<CellConfig> cellImportConfigs;

    public static final ImportConfig customerImport;

    static {
        customerImport = new ImportConfig();
        customerImport.setSheetIndex(0);
        customerImport.setHeaderIndex(0);
        customerImport.setStartRow(1);
        customerImport.setDataClazz(ImportFileDto.class);
        List<CellConfig> customerImportCellConfigs = new ArrayList<>();

//        customerImportCellConfigs.add(new CellConfig(0, "nom"));
//        customerImportCellConfigs.add(new CellConfig(1, "prenom"));
//        customerImportCellConfigs.add(new CellConfig(2, "region"));
//        customerImportCellConfigs.add(new CellConfig(3, "prefecture"));
//        customerImportCellConfigs.add(new CellConfig(4, "sousPrefecture"));
//        customerImportCellConfigs.add(new CellConfig(5, "quartierDistrict"));
          customerImportCellConfigs.add(new CellConfig(0, "contact"));
//        customerImportCellConfigs.add(new CellConfig(7, "message"));

        customerImport.setCellImportConfigs(customerImportCellConfigs);
    }
}

