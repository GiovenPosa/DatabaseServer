package edu.uob;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.io.BufferedWriter;
import java.io.FileWriter;

public abstract class QueryCmdHandler {
    protected final String destinationPath;
    protected final String selectedFile;

    public QueryCmdHandler(String storageFolderPath, String selectedFile) {
        this.destinationPath = storageFolderPath + File.separator + selectedFile;
        this.selectedFile = selectedFile;
    }

    protected static boolean evaluateCondition(String cellValue, String comparator, String conditionValue) {
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

    void validateCondition(ConditionExpression expression, String[] header) throws Exception {
        if (expression instanceof ConditionLeaf leaf) {
            boolean isFound = false;
            for (String column : header) {
                if (column.equalsIgnoreCase(leaf.column)) {
                    isFound = true;
                    break;
                }
            }
            if (!isFound) {
                throw new Exception("[ERROR]: Condition column '" + leaf.column + "' not found in table.");
            }
        } else if (expression instanceof ConditionNode node) {
            validateCondition(node.left, header);
            validateCondition(node.right, header);
        }
    }

    protected String writeLinesToFile(File file, List<String> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (Exception e) {
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
