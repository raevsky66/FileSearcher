package fileSearch;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;

public class FileSearcherGUI {
    // Имя файла конфигурации
    private static final String CONFIG_FILE = "config.properties";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(FileSearcherGUI::createGUI);
    }

    private static void createGUI() {
        JFrame frame = new JFrame("File Searcher");
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 2));

        JTextField folderField = new JTextField();
        JTextField searchField = new JTextField();
        JTextField xmlTagField = new JTextField();
        JTextField excelColumnField = new JTextField();
        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);

        // Загрузка сохранённых параметров из файла конфигурации
        Properties config = loadConfig();
        folderField.setText(config.getProperty("folder", ""));
        searchField.setText(config.getProperty("search", ""));
        xmlTagField.setText(config.getProperty("xmlTag", ""));
        excelColumnField.setText(config.getProperty("excelColumn", ""));

        panel.add(new JLabel("📂 Папка:"));
        panel.add(folderField);
        panel.add(new JLabel("🔍 Текст поиска:"));
        panel.add(searchField);
        panel.add(new JLabel("📝 XML-тег (опционально):"));
        panel.add(xmlTagField);
        panel.add(new JLabel("📊 Номер колонки Excel (опционально):"));
        panel.add(excelColumnField);

        JButton searchButton = new JButton("🚀 Начать поиск");
        panel.add(searchButton);

        frame.add(panel, BorderLayout.NORTH);
        frame.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                outputArea.setText("🔄 Выполняется поиск...");

                Executors.newSingleThreadExecutor().submit(() -> {
                    FileSearcher.searchFiles(
                        Arrays.asList(folderField.getText().split(",")),
                        searchField.getText(),
                        xmlTagField.getText(),
                        excelColumnField.getText()
                    );

                    List<String> foundFiles = FileSearcher.getFoundFiles();
                    SwingUtilities.invokeLater(() -> {
                        outputArea.setText("✅ Поиск завершён!\n" + String.join("\n", foundFiles));
                    });
                });
            }
        });

        // Сохранение параметров при закрытии окна
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveConfig(folderField.getText(), searchField.getText(), xmlTagField.getText(), excelColumnField.getText());
            }
        });

        frame.setVisible(true);
    }

    /**
     * Загружает параметры из файла конфигурации.
     */
    private static Properties loadConfig() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Ошибка загрузки конфигурации: " + e.getMessage());
        }
        return props;
    }

    /**
     * Сохраняет параметры в файл конфигурации.
     */
    private static void saveConfig(String folder, String search, String xmlTag, String excelColumn) {
        Properties props = new Properties();
        props.setProperty("folder", folder);
        props.setProperty("search", search);
        props.setProperty("xmlTag", xmlTag);
        props.setProperty("excelColumn", excelColumn);
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "FileSearcher Configuration");
        } catch (IOException e) {
            System.err.println("Ошибка сохранения конфигурации: " + e.getMessage());
        }
    }
}
