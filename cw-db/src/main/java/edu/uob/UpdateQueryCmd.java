package edu.uob;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateQueryCmd extends QueryCmdHandler{
    private String tableName;
    private String fileName;
    private String assignmentList;

    public UpdateQueryCmd(String destinationPath, String selectedFile) {
        super(destinationPath, selectedFile);
    }

    private boolean parseSetCommand(String command) {
        int getIndex = command.toUpperCase().indexOf(" WHERE ");
        if (getIndex == -1) {
            return false;
        }
        String setCommand = command.substring(0, getIndex).trim();
        Pattern setPattern = Pattern.compile("^(\\w+)\\s+SET\\s+(.*)$", Pattern.CASE_INSENSITIVE);
        Matcher setMatcher = setPattern.matcher(setCommand);
        if (!setMatcher.matches()) {
            return false;
        }
        tableName = setMatcher.group(1).trim();
        fileName = tableName + ".tab";
        assignmentList = setMatcher.group(2).trim();
        return true;
    }

    private Map<String, String> parseAssignmentsList(String assignmentList) {
        Map<String, String> assignments = new HashMap<>();

        String[] assignmentsArray = assignmentList.split(",");
        for (String assignment : assignmentsArray) {
            String[] pair = assignment.split("=", 2);
            if (pair.length != 2) continue;
            String columnName = pair[0].trim();
            String value = pair[1].trim();
            assignments.put(columnName, value);
        }
        return assignments;
    }

    private Map<String, Integer> findTargetColumns(String[] columns, Map<String, String> assignments, String tableName) throws Exception {
        Map<String, Integer> targetColumnIndexes = new HashMap<>();
        for (Map.Entry<String, String> entry : assignments.entrySet()) {
            String targetCol = entry.getKey();
            boolean found = false;
            for (int i = 0; i < columns.length; i++) {
                if (columns[i].equalsIgnoreCase(targetCol)) {
                    targetColumnIndexes.put(targetCol, i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new Exception("[ERROR]: Target column " + targetCol + " not found in table '" + tableName + "'.");
            }
        }
        return targetColumnIndexes;
    }

    public String execute(String command) {

        if (!parseSetCommand(command)) {
            return "[ERROR]: Invalid UPDATE syntax. Use: UPDATE <tableName> SET <updateColumn> = [new_value] WHERE <conditionColumn> <comparator> [condition_value].";
        }

        int getIndex = command.toUpperCase().indexOf(" WHERE ");
        String conditionCmd = command.substring(getIndex + " WHERE ".length()).trim();

        ConditionExpression condition;
        if (!conditionCmd.isEmpty()) {
            ConditionParser conditionParser = new ConditionParser(conditionCmd);
            condition = conditionParser.parseConditionExpression();
        } else {
            return "[ERROR]: No conditions provided in UPDATE query.";
        }

        Map<String, String> assignments = parseAssignmentsList(assignmentList);
        if (assignments.isEmpty()) {
            return "[ERROR]: No valid assignments found.";
        }
        for (String column : assignments.keySet()) {
            if (column.equalsIgnoreCase("id")) {
                return "[ERROR]: Cannot update '" + column + "' in table '" + tableName + "'.";
            }
        }

        TableBlock table = getTableBlock(tableName);
        if (table == null) {
            return "[ERROR]: Unable to read table '" + tableName + "'.";
        }
        if (table.tableStartIndex == -1) {
            return "[ERROR]: Table " + tableName + " does not exist.";
        }

        String headerLine = table.lines.get(table.headerIndex).trim();
        String[] columns = headerLine.split("\t");

        try {
            validateCondition(condition, columns);
        } catch (Exception e) {
            return e.getMessage();
        }

        Map<String, Integer> targetColumnIndexes;
        try {
            targetColumnIndexes = findTargetColumns(columns, assignments, tableName);
        } catch (Exception e) {
            return e.getMessage();
        }

        int updatedRows = 0;
        for (int i = table.headerIndex + 1; i < table.tableEndIndex; i++) {
            String row = table.lines.get(i);
            if (row.trim().isEmpty()) continue;
            String[] rowValues = row.split("\t", - 1);
            boolean rowMatches = condition == null || condition.evaluate(rowValues, columns);
            if (rowMatches) {
                for (Map.Entry<String, String> entry : assignments.entrySet()) {
                    String targetColumn = entry.getKey();
                    String newValue = entry.getValue();
                    int targetIndex = targetColumnIndexes.get(targetColumn);
                    if (rowValues.length > targetIndex) {
                        rowValues[targetIndex] = newValue;
                    }
                }
                updatedRows++;
                StringBuilder stringBuilder = new StringBuilder();
                for (int j = 0; j < rowValues.length; j++) {
                    stringBuilder.append(rowValues[j]);
                    if (j < rowValues.length - 1) {
                        stringBuilder.append("\t");
                    }
                }
                table.lines.set(i, stringBuilder.toString());
            }
        }

        File tableFile = new File(destinationPath, fileName);
        String writeError = writeLinesToFile(tableFile, table.lines);
        if (writeError != null) {
            return writeError;
        }
        return "[OK]: Updated " + updatedRows + "  row(s) in table '" + tableName + "'";
    }
}
