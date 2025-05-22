package fileSearch;

import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class FileSearcher {
    public static void searchFiles(String directory, String searchText) {
        try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".txt") || path.toString().endsWith(".xls") || path.toString().endsWith(".xlsx"))
                 .forEach(path -> {
                     if (containsText(path.toFile(), searchText)) {
                         System.out.println("Найдено в файле: " + path);
                     }
                 });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean containsText(File file, String searchText) {
        try {
            if (file.getName().endsWith(".txt")) {
                return searchTextInTxt(file, searchText);
            } else if (file.getName().endsWith(".xls") || file.getName().endsWith(".xlsx")) {
                return searchTextInExcel(file, searchText);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean searchTextInTxt(File file, String searchText) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(searchText)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean searchTextInExcel(File file, String searchText) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis);

            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().contains(searchText)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        String directoryPath = "C:/Users/Example/Documents";  // Укажи путь к папке
        String searchText = "найти меня";  // Текст для поиска
        searchFiles(directoryPath, searchText);
    }
}