package fileSearch;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class FileSearcher {
    private static final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public static void searchFiles(List<String> directories, String searchText, String xmlTag, String excelColumn) {
        for (String directory : directories) {
            String formattedPath = directory.replace("\\", "\\\\"); // Коррекция UNC-пути
            processDirectory(formattedPath, searchText, xmlTag, excelColumn);
        }
        executor.shutdown();
    }

    private static void processDirectory(String directory, String searchText, String xmlTag, String excelColumn) {
        try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".xml") || path.toString().endsWith(".xls") || path.toString().endsWith(".xlsx"))
                 .forEach(path -> executor.submit(() -> processFile(path.toFile(), searchText, xmlTag, excelColumn)));
        } catch (Exception e) {
            System.err.println("Ошибка доступа к папке: " + directory);
        }
    }

    private static void processFile(File file, String searchText, String xmlTag, String excelColumn) {
        try {
            if (file.getName().endsWith(".xml")) {
                if (searchTextInXml(file, searchText, xmlTag)) {
                    System.out.println("🔍 Найдено в XML: " + file.getAbsolutePath());
                }
            } else if (file.getName().endsWith(".xls") || file.getName().endsWith(".xlsx")) {
                if (searchTextInExcel(file, searchText, excelColumn)) {
                    System.out.println("📊 Найдено в Excel: " + file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки файла: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    private static boolean searchTextInXml(File file, String searchText, String xmlTag) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList nodes = (xmlTag == null || xmlTag.isEmpty()) ? doc.getElementsByTagName("*") : doc.getElementsByTagName(xmlTag);
            for (int i = 0; i < nodes.getLength(); i++) {
                if (nodes.item(i).getTextContent().contains(searchText)) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки XML: " + file.getAbsolutePath());
        }
        return false;
    }

    private static boolean searchTextInExcel(File file, String searchText, String excelColumn) {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {
            
            for (Sheet sheet : workbook) {
                int columnIndex = (excelColumn != null && !excelColumn.isEmpty()) ? Integer.parseInt(excelColumn) : -1;

                for (Row row : sheet) {
                    if (columnIndex == -1) { // Искать во всех колонках
                        for (Cell cell : row) {
                            if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().contains(searchText)) {
                                return true;
                            }
                        }
                    } else { // Искать только в указанной колонке
                        Cell cell = row.getCell(columnIndex);
                        if (cell != null && cell.getCellType() == CellType.STRING && cell.getStringCellValue().contains(searchText)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки Excel: " + file.getAbsolutePath());
        }
        return false;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Использование: fs <папка1,папка2,...> <текст> [xmlTag - название тэга в котором искать] [excelColumn - индекс колонки, начиная с 0]");
            return;
        }

        List<String> directories = Arrays.asList(args[0].split(","));
        String searchText = args[1];
        String xmlTag = args.length > 2 ? args[2] : null;
        String excelColumn = args.length > 3 ? args[3] : null;
        System.out.println("ищем по тэгу: " + String.valueOf(xmlTag == null || xmlTag.isEmpty() ? "всем" : xmlTag));
        System.out.println("ищем по колонке: " + String.valueOf(excelColumn == null || excelColumn.isEmpty() ? "всем" : excelColumn));
        searchFiles(directories, searchText, xmlTag, excelColumn);
    }
}
