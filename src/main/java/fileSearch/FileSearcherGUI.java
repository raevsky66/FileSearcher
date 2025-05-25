package fileSearch;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

public class FileSearcherGUI {
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

        frame.setVisible(true);
    }
}
