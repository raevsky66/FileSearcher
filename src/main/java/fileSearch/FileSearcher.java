package fileSearch;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class FileSearcher {

    // Синхронизированный список для хранения найденных файлов
    private static final List<String> foundFiles = Collections.synchronizedList(new ArrayList<>());

    /**
     * Ищет файлы с указанными критериями во всех переданных каталогах.
     *
     * @param directories Список каталогов для поиска.
     * @param searchText  Текст для поиска.
     * @param xmlTag      Если не пусто, то в XML-файлах поиск будет ограничен указанным тегом.
     * @param excelColumn Если не пусто, то в Excel-файлах поиск будет произведён только в указанном столбце
     *                    (номер столбца в виде строки, например "0" для первого столбца).
     */
    public static void searchFiles(List<String> directories, String searchText, String xmlTag, String excelColumn) {
        foundFiles.clear();
        // Создаём новый пул потоков для каждого поиска
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            for (String directory : directories) {
                processDirectory(directory, searchText, xmlTag, excelColumn, executor);
            }
        } finally {
            shutdownExecutor(executor);
        }
        // Принудительная сборка мусора (обычно не требуется)
        System.gc();
    }

    /**
     * Обходит заданный каталог и отправляет в пул потоков задачи по обработке файлов.
     */
    private static void processDirectory(String directory, String searchText, String xmlTag, String excelColumn, ExecutorService executor) {
        try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> {
                     String lowerName = path.toString().toLowerCase();
                     return lowerName.endsWith(".xml") || lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx");
                 })
                 .forEach(path -> executor.submit(() -> processFile(path.toFile(), searchText, xmlTag, excelColumn)));
        } catch (AccessDeniedException e) {
            System.err.println("⚠ Ошибка доступа: " + directory);
        } catch (Exception e) {
            System.err.println("Ошибка обработки папки: " + directory);
        }
    }

    /**
     * Обрабатывает отдельный файл – в зависимости от его типа выполняется поиск текста в XML или Excel.
     */
    private static void processFile(File file, String searchText, String xmlTag, String excelColumn) {
        try {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".xml") && searchTextInXml(file, searchText, xmlTag)) {
                foundFiles.add("🔍 Найдено в XML: " + file.getAbsolutePath());
            } else if ((fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) && searchTextInExcel(file, searchText, excelColumn)) {
                foundFiles.add("📊 Найдено в Excel: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки файла: " + file.getAbsolutePath());
        }
    }

    /**
     * Ищет заданный текст в XML-файле.
     */
    private static boolean searchTextInXml(File file, String searchText, String xmlTag) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList nodes = (xmlTag == null || xmlTag.isEmpty()) 
                                ? doc.getElementsByTagName("*") 
                                : doc.getElementsByTagName(xmlTag);
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

    /**
     * Ищет заданный текст в Excel-файле.
     */
    private static boolean searchTextInExcel(File file, String searchText, String excelColumn) {
        FileInputStream fis = null;
        Workbook workbook = null;
        try {
            fis = new FileInputStream(file);
            String lowerName = file.getName().toLowerCase();
            workbook = lowerName.endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis);

            for (Sheet sheet : workbook) {
                int columnIndex = (excelColumn != null && !excelColumn.isEmpty()) ? Integer.parseInt(excelColumn) : -1;
                for (Row row : sheet) {
                    if (columnIndex == -1) {
                        // Поиск по всем ячейкам строки
                        for (Cell cell : row) {
                            if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().contains(searchText)) {
                                return true;
                            }
                        }
                    } else {
                        // Поиск только в указанном столбце
                        Cell cell = row.getCell(columnIndex);
                        if (cell != null && cell.getCellType() == CellType.STRING && cell.getStringCellValue().contains(searchText)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки Excel: " + file.getAbsolutePath());
        } finally {
            try {
                if (workbook != null) {
                    workbook.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (Exception ex) {
                System.err.println("Ошибка закрытия потоков Excel: " + file.getAbsolutePath());
            }
        }
        return false;
    }

    /**
     * Возвращает список найденных файлов.
     */
    public static List<String> getFoundFiles() {
        return foundFiles;
    }

    /**
     * Корректное завершение работы пула потоков.
     */
    private static void shutdownExecutor(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
