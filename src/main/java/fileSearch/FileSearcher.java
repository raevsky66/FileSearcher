package fileSearch;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
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

    public static void searchFiles(List<String> directories, String searchText) {
        for (String directory : directories) {
            String formattedPath = directory.replace("\\", "\\\\"); // Коррекция UNC-пути
            processDirectory(formattedPath, searchText);
        }
        executor.shutdown();
    }

    private static void processDirectory(String directory, String searchText) {
        try (Stream<Path> paths = Files.walk(Paths.get(directory))) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".xml") || path.toString().endsWith(".xls") || path.toString().endsWith(".xlsx"))
                 .forEach(path -> executor.submit(() -> processFile(path.toFile(), searchText)));
        } catch (Exception e) {
            System.err.println("Ошибка доступа к папке: " + directory);
        }
    }

    private static void processFile(File file, String searchText) {
        try {
            if (file.getName().endsWith(".xml")) {
                if (searchTextInXml(file, searchText)) {
                    System.out.println("🔍 Найдено в XML: " + file.getAbsolutePath());
                }
            } else if (file.getName().endsWith(".xls") || file.getName().endsWith(".xlsx")) {
                if (searchTextInExcel(file, searchText)) {
                    System.out.println("📊 Найдено в Excel: " + file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки файла: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    private static boolean searchTextInXml(File file, String searchText) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName("*");
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

    private static boolean searchTextInExcel(File file, String searchText) {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.STRING && cell.getStringCellValue().contains(searchText)) {
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
            System.out.println("Использование: java FileSearcher <папка1,папка2,...> <текст>");
            return;
        }

        List<String> directories = Arrays.asList(args[0].split(","));
        String searchText = args[1];

        searchFiles(directories, searchText);
    }
}