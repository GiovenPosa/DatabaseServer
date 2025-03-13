package edu.uob;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DBTableManager {
    private final String storageFolderPath;

    public DBTableManager(String storageFolderPath) {
        this.storageFolderPath = storageFolderPath;
    }

    private String writeLinesToTable(File file, List<String> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            return "[ERROR]: Unable to write updates to the database file.";
        }
        return null;
    }

    public String createTable(String command, String selectedFile) {
        String destinationPath = storageFolderPath + File.separator + selectedFile;

        if (selectedFile == null) {
            return "[ERROR]: No Database selected. Use 'USE' <filename>' first.";
        }

        Pattern pattern = Pattern.compile("(\\w+)(?: \\((.*?)\\))?");
        Matcher matcher = pattern.matcher(command);

        if (!matcher.matches()) {
            return "[ERROR]: Invalid CREATE TABLE syntax. Use: CREATE TABLE <tableName> (col1, col2, ...)";
        }

        String tableName = matcher.group(1);
        String fileName = tableName + ".tab";
        File tableFile = new File(destinationPath, fileName);

        if (tableFile.exists()) {
            return "[ERROR]: Table already Exists";
        }

        String columnString = matcher.group(2);
        String headerLine;

        if (columnString == null || columnString.trim().isEmpty()) {
            headerLine = "id";
        } else {
            String[] columns = columnString.split(",\\s*");
            headerLine = "id\t" + String.join("\t", columns);
        }

        try (BufferedWriter fileWriter = new BufferedWriter(new FileWriter(tableFile, true))) {
            fileWriter.write("Table: " + tableName + "\n");
            fileWriter.write(headerLine + "\n");
        } catch (Exception e) {
            return "[ERROR]: Unable to write updates to the database file." + e.getMessage();
        }

        return "[OK]: Table '" + fileName + "' created successfully in '" + selectedFile + "'.";
    }

    public String dropTable(String command, String selectedFile) {
        String destinationPath = storageFolderPath + File.separator + selectedFile;

        if (selectedFile == null) {
            return "[ERROR]: No Database selected. Use 'USE' <filename>' first.";
        }

        Pattern pattern = Pattern.compile("(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(command);
        if (!matcher.matches()) {
            return "[ERROR]: Invalid DROP TABLE syntax. Use: DROP TABLE <tableName>";
        }

        String tableName = matcher.group(1);
        String fileName = tableName + ".tab";
        File tableFile = new File(destinationPath, fileName);
        if (!tableFile.exists()) {
            return "[ERROR]: file '" + fileName + "' does not exist.";
        }
        try {
            boolean deleted = tableFile.delete();
            if (deleted) {
                return "[OK]: File: '" + fileName + "' deleted successfully.";
            } else {
                return "[ERROR]: File: '" + fileName + "' could not be deleted.";
            }
        } catch (Exception e) {
            return "[ERROR]: " + e.getMessage();
        }
    }

    public String alterTable(String command, String selectedFile) {
        String destinationPath = storageFolderPath + File.separator + selectedFile;

        if (selectedFile == null) {
            return "[ERROR]: No Database selected. Use 'USE' <filename>' first.";
        }

        Pattern pattern = Pattern.compile("(\\w+)\\s+(ADD|DROP)\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(command);
        if (!matcher.matches()) {
            return "[ERROR]: Invalid ALTER TABLE syntax. Use: ALTER TABLE <tableName> ADD/DROP <columnName>";
        }

        String tableName = matcher.group(1);
        String fileName = tableName + ".tab";
        String operator = matcher.group(2).toUpperCase();
        String columnName = matcher.group(3);

        if (operator.equals("DROP") && columnName.equalsIgnoreCase("id")) {
            return "[ERROR]: Cannot drop '" + columnName + "' in '" + tableName + "'.";
        }

        File tableFile = new File(destinationPath, fileName);
        List<String> lines;
        try {
            lines = Files.readAllLines(tableFile.toPath());
        } catch (Exception e) {
            return "[ERROR]: Could not read file: " + fileName + ". " + e.getMessage();
        }

        boolean tableExists = false;
        boolean inDesiredTable = false;
        int dropColumnIndex = -1;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("Table: ")) {
                if (line.equals(("Table: " + tableName))) {
                    tableExists = true;
                    inDesiredTable = true;
                    if (i + 1 < lines.size()) {
                        String headerLine = lines.get(i + 1).trim();
                        List<String> headerColumns = new ArrayList<>(Arrays.asList(headerLine.split("\t")));
                        if (operator.equals("ADD")) {
                            if (headerColumns.stream().anyMatch(col -> col.equalsIgnoreCase(columnName))) {
                                return "[ERROR]: Column '" + columnName + "' already exists in table '" + fileName + "'.";
                            }
                            headerColumns.add(columnName);
                            String newHeaderLine = String.join("\t", headerColumns);
                            lines.set(i + 1, newHeaderLine);
                        } else if (operator.equals("DROP")) {
                            int index = -1;
                            for (int j = 0; j < headerColumns.size(); j++) {
                                if (headerColumns.get(j).equalsIgnoreCase(columnName)) {
                                    index = j;
                                    break;
                                }
                            }
                            if (index == -1) {
                                return "[ERROR]: Column '" + columnName + "' not found in table '" + fileName + "'.";
                            }
                            dropColumnIndex = index;
                            headerColumns.remove(index);
                            String newHeaderLine = String.join("\t", headerColumns);
                            lines.set(i + 1, newHeaderLine);
                        }
                        i++;
                    } else {
                        return "[ERROR]: No header found for table '" + fileName + "'.";
                    }
                    continue;
                } else if (inDesiredTable) {
                    inDesiredTable = false;
                }
            }
            if (inDesiredTable) {
                if (line.trim().isEmpty() || line.startsWith("Table: ")) {
                    continue;
                }
                if (operator.equals("ADD")) {
                    List<String> rowValues = new ArrayList<>(Arrays.asList(line.split("\t", -1)));
                    rowValues.add("");
                    String newRowValue = String.join("\t", rowValues);
                    lines.set(i, newRowValue);
                }
                else if (operator.equals("DROP")) {
                    String[] rowValues = line.split("\t", -1);
                    if (dropColumnIndex < rowValues.length) {
                        List<String> newRowValues = new ArrayList<>();
                        for (int j = 0; j < rowValues.length; j++) {
                            if (j != dropColumnIndex) {
                                newRowValues.add(rowValues[j].trim());
                            }
                        }
                        String newRow = String.join("\t", newRowValues);
                        lines.set(i, newRow);
                    }
                }
            }
        }

        if (!tableExists) {
            return "[ERROR]: Table '" + fileName + "' does not exist.";
        }
        String writeError = writeLinesToTable(tableFile, lines);
        if (writeError != null) {
            return writeError;
        }

        if (operator.equals("ADD")) {
            return "[OK]: Column '" + columnName + "' added successfully to '" + fileName + "' in '" + selectedFile + "'.";
        } else {
            return "[OK]: Column '" + columnName + "' deleted successfully from '" + fileName + "' in '" + selectedFile + "'.";
        }
    }

    public String insertAttribute (String command, String selectedFile) {
        String destinationPath = storageFolderPath + File.separator + selectedFile;

        if (selectedFile == null) {
            return "[ERROR]: No Database selected. Use 'USE' <filename>' first.";
        }

        Pattern pattern = Pattern.compile("^(\\w+)\\s+VALUES\\s+\\((.*)\\)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(command);

        if (!matcher.matches()) {
            return "[ERROR]: Invalid INSERT syntax. Use: INSERT INTO <tableName> VALUES (value1, value2, ...)";
        }

        String tableName = matcher.group(1);
        String fileName = tableName + ".tab";

        String valueGroup = matcher.group(2);
        String[] valueTokens = valueGroup.split(",");
        List<String> valueList = Arrays.asList(valueTokens);
        File tableFile = new File(destinationPath, fileName);
        List<String> lines;
        try {
            lines = Files.readAllLines(tableFile.toPath());
        } catch (Exception e) {
            return "[ERROR]: Could not read table: " + fileName + ". " + e.getMessage();
        }

        int tableStartIndex = -1;
        int headerIndex = -1;
        int tableEndIndex = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("Table: ")) {
                if (line.equals(("Table: " + tableName))) {
                    tableStartIndex = i;
                    headerIndex = i + 1;

                    for (int j = headerIndex + 1; j < lines.size(); j++) {
                        if (lines.get(j). startsWith("Table: ")) {
                            tableEndIndex = j;
                            break;
                        }
                    }
                    break;
                }
            }
        }

        if (tableStartIndex == -1) {
            return "[ERROR]: Table '" + fileName + "' does not exist.";
        }
        if (headerIndex >= lines.size()) {
            return "[ERROR]: No header found for table '" + fileName + "'.";
        }

        String headerLine = lines.get(headerIndex).trim();
        String[] columns = headerLine.split("\t");
        int columnCount = columns.length - 1;
        int newID = 1;

        if (valueList.size() != columnCount) {
            return "[ERROR]: Expected " + columnCount + " columns but got " + valueList.size() + ".";
        }
        for (int i = headerIndex + 1; i < tableEndIndex; i++) {
            String currentRow = lines.get(i).trim();
            if (!currentRow.isEmpty()) {
                String[] rowValues = currentRow.split("\t", -1);
                try {
                    int rowID = Integer.parseInt(rowValues[0]);
                    if (rowID >= newID) {
                        newID = rowID + 1;
                    }
                } catch (NumberFormatException e) {
                    e.getCause();
                }
            }
        }

        StringBuilder newRowValues = new StringBuilder();
        newRowValues.append(newID);
        for (String values : valueList) {
            newRowValues.append("\t").append(values);
        }
        String newRow = newRowValues.toString();

        lines.add(tableEndIndex, newRow);

        String writeError = writeLinesToTable(tableFile, lines);
        if (writeError != null) {
            return writeError;
        }
        return "[OK]: Row inserted successfully into table '" + tableName + "'.";
    }

}
