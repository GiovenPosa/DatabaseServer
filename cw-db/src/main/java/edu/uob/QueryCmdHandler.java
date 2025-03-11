package edu.uob;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class QueryCmdHandler {
    protected final String destinationPath;
    protected final String selectedFile;
    protected List<String[]> conditionsList;

    public QueryCmdHandler(String storageFolderPath, String selectedFile) {
        this.destinationPath = storageFolderPath + File.separator + selectedFile;
        this.selectedFile = selectedFile;
    }



    protected boolean parseConditionsCommand(String command) {
        List<String[]> conditions = new ArrayList<>();

        Pattern conditionsPattern = Pattern.compile(
                "(?:\\s*(AND|OR)\\s+)?(\\w+)\\s*(==|>|<|>=|<=|!=|LIKE)\\s*('[^']*'|\\S+)?", Pattern.CASE_INSENSITIVE);
        Matcher conditionsMatcher = conditionsPattern.matcher(command);
        while (conditionsMatcher.find()) {
            String operator = conditionsMatcher.group(1);
            String column = conditionsMatcher.group(2);
            String comparator = conditionsMatcher.group(3);
            String value = conditionsMatcher.group(4);
            conditions.add(new String[]{operator, column, comparator, value});
        }
        if (conditions.isEmpty()) {
            return false;
        }
        conditionsList = conditions;
        return true;
    }

    protected List<Integer> findConditionColumns(String[] columns, List<String[]> conditionsList, String tableName) throws Exception {
        List<Integer> conditionColumnIndexes = new ArrayList<>();
        for (String[] condition : conditionsList) {
            String conditionColumn = condition[1];
            int index = -1;
            for (int i = 0; i < columns.length; i++) {
                if (columns[i].equalsIgnoreCase(conditionColumn)) {
                    index = i;
                    break;
                }
            }
            if (index == -1) {
                throw new Exception("[ERROR]: Condition column " + conditionColumn + " not found in table '" + tableName + "'.");
            }
            conditionColumnIndexes.add(index);
        }
        return conditionColumnIndexes;
    }

    protected boolean evaluateCondition(String cellValue, String comparator, String conditionValue) {
        ComparatorType comparatorType;
        try {
            comparatorType = ComparatorType.fromSymbol(comparator);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (comparatorType == ComparatorType.LIKE) {
            return cellValue.contains(conditionValue);
        }

        try {
            double cellNum = Double.parseDouble(cellValue);
            double conditionNum = Double.parseDouble(conditionValue);
            return switch (comparatorType) {
                case EQUAL -> cellNum == conditionNum;
                case NOT_EQUAL -> cellNum != conditionNum;
                case LESS -> cellNum < conditionNum;
                case LESS_OR_EQUAL -> cellNum <= conditionNum;
                case GREATER -> cellNum > conditionNum;
                case GREATER_OR_EQUAL -> cellNum >= conditionNum;
                default -> false;
            };
        } catch (NumberFormatException e) {
            return switch (comparatorType) {
                case EQUAL -> cellValue.equals(conditionValue);
                case NOT_EQUAL -> !cellValue.equals(conditionValue);
                default -> false;
            };
        }
    }

    protected String writeLinesToFile(File file, List<String> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "[ERROR]: Unable to write updates to the database file.";
        }
        return null;
    }

    protected static class TableBlock {
        public List<String> lines;
        public int tableStartIndex;
        public int tableEndIndex;
        public int headerIndex;

        public TableBlock(List<String> lines, int tableStartIndex, int tableEndIndex, int headerIndex) {
            this.lines = lines;
            this.tableStartIndex = tableStartIndex;
            this.tableEndIndex = tableEndIndex;
            this.headerIndex = headerIndex;
        }
    }

    protected TableBlock getTableBlock(String tableName) {
        String fileName = tableName + ".tab";
        File tableFile = new File(destinationPath, fileName);
        if (!tableFile.exists() || !tableFile.isFile()) {
            return null;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(tableFile.toPath());
        } catch (Exception e) {
            return null;
        }

        int tableStartIndex = -1;
        int tableEndIndex = lines.size();
        int headerIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("Table: ")) {
                if (line.equals("Table: " + tableName)) {
                    tableStartIndex = i;
                    headerIndex = i + 1;
                    for (int j = headerIndex + 1; j < lines.size(); j++) {
                        if (lines.get(j).startsWith("Table: ")) {
                            tableEndIndex = j;
                            break;
                        }
                    }
                    break;
                }
            }
        }
        return new TableBlock(lines, tableStartIndex, tableEndIndex, headerIndex);
    }

}
