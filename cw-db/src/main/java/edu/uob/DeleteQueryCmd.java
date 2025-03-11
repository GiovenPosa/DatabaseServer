package edu.uob;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeleteQueryCmd extends QueryCmdHandler {
    private String tableName;
    private String fileName;

    public DeleteQueryCmd(String storageFolderPath, String selectedFile) {
        super(storageFolderPath, selectedFile);
    }

    private boolean parseTableName(String command) {
        Pattern pattern = Pattern.compile("^(\\w+)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(command);
        if (!matcher.matches()) {
            return false;
        }
        tableName = matcher.group(1);
        fileName = tableName + ".tab";
        return true;
    }

    public String execute(String command) {

        String whereCmd = command.toUpperCase();
        int getIndex = whereCmd.indexOf(" WHERE ");
        String fromCmd = command.substring(0, getIndex).trim();
        String conditionsCmd = command.substring(getIndex + " WHERE ".length());

        if (!parseTableName(fromCmd)) {
            return "[ERROR]: Invalid SELECT syntax. Use syntax DELETE FROM <tableName>.";
        }

        if (!parseConditionsCommand(conditionsCmd)) {
            return "[ERROR]: No valid conditions found Use syntax <conditionHeader> <comparator> <newValue> [<condition(s)].";
        }

        TableBlock table = getTableBlock(tableName);
        if (table == null) {
            return "[ERROR]: Unable to read table '" + tableName + "'.";
        }
        if (table.tableStartIndex == -1) {
            return "[ERROR]: Table '" + tableName + "' does not exist.";
        }

        String headerLine = table.lines.get(table.headerIndex).trim();
        String[] columns = headerLine.split("\t");

        List<Integer> conditionColumnIndexes;
        try {
            conditionColumnIndexes = findConditionColumns(columns, conditionsList, tableName);
        } catch (Exception e) {
            return e.getMessage();
        }

        List<String> newLines = new ArrayList<>();
        for (int i = 0; i <table.headerIndex; i++) {
            newLines.add(table.lines.get(i));
        }
        newLines.add(table.lines.get(table.headerIndex));

        int deletedRows = 0;
        for (int i = table.headerIndex+1; i < table.tableEndIndex; i++) {
            String row = table.lines.get(i);
            if (row.trim().isEmpty()) {
                continue;
            }
            String[] rowValues = row.split("\t", -1);
            boolean deletedRow = false;
            if (conditionsList != null && !conditionsList.isEmpty()) {
                boolean rowResults = false;
                for (int j = 0; j < conditionsList.size(); j++) {
                    String[] condition = conditionsList.get(j);
                    int columnIndex = conditionColumnIndexes.get(j);
                    if (rowValues.length <= columnIndex) {
                        rowResults = false;
                        break;
                    }
                    String cellValue = rowValues[columnIndex].trim();
                    boolean conditionResult = evaluateCondition(cellValue, condition[2], condition[3]);
                    if (j == 0) {
                        rowResults = conditionResult;
                    } else {
                        if ("AND".equalsIgnoreCase(condition[0])) {
                            rowResults = rowResults && conditionResult;
                        } else if ("OR".equalsIgnoreCase(condition[0])) {
                            rowResults = rowResults || conditionResult;
                        }
                    }
                }
                deletedRow = rowResults;
            }
            if (deletedRow) {
                deletedRows++;
            } else {
                newLines.add(row);
            }
        }

        for (int i = table.tableEndIndex; i < table.lines.size(); i++) {
            newLines.add(table.lines.get(i));
        }

        File tableFile = new File(destinationPath, fileName);
        String writeError = writeLinesToFile(tableFile, newLines);
        if (writeError != null) {
            return writeError;
        }
        return "[OK]: Deleted " + deletedRows + " row(s) in table '" + tableName + "'.";
    }

}
