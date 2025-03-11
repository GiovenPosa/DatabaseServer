package edu.uob;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SelectQueryCmd extends QueryCmdHandler{
    private String tableName;
    private String attributeList;

    public SelectQueryCmd (String destinationPath, String selectedFile) {
        super(destinationPath, selectedFile);
    }

    private boolean parseAttributeList(String command) {
        Pattern pattern = Pattern.compile("^(\\*|[\\w\\s,]+)\\s+FROM\\s+(\\w+)$", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(command);
        if (!matcher.matches()) {
            return false;
        }

        attributeList = matcher.group(1).trim();
        tableName = matcher.group(2).trim();
        return true;
    }

    public String execute(String command) {

        String whereCmd = command.toUpperCase();
        String attributeCmd;
        String conditionsCmd = null;
        int getIndex = whereCmd.indexOf(" WHERE ");
        if (getIndex != -1) {
            attributeCmd = command.substring(0, getIndex).trim();
            conditionsCmd = command.substring(getIndex + " WHERE ".length()).trim();
        } else {
            attributeCmd = command.trim();
        }

        if(!parseAttributeList(attributeCmd)){
            return "[ERROR]: Invalid SELECT syntax. Use: SELECT <columnName(s)> FROM <tableName>.";
        }

        if (conditionsCmd != null && !conditionsCmd.isEmpty()) {
            if (!parseConditionsCommand(conditionsCmd)) {
                return "[ERROR]: Invalid condition syntax. Use: WHERE <columnName> <comparator> <newValue> [AND/OR <condition>]";
            }
        } else {
            conditionsList = new ArrayList<>();
        }

        TableBlock table = getTableBlock(tableName);
        if (table == null) {
            return "[ERROR]: Unable to read table '" + tableName + "'. File may not exist.";
        }

        String headerLine = table.lines.get(table.headerIndex).trim();
        String[] columns = headerLine.split("\t");

        List<Integer> conditionColumnIndexes = new ArrayList<>();
        if (!conditionsList.isEmpty()) {
            try {
                conditionColumnIndexes = findConditionColumns(columns, conditionsList, tableName);
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        List<Integer> targetColumnsIndexes = new ArrayList<>();
        List<String> targetColumnsNames = new ArrayList<>();
        if (attributeList.equals("*")) {
            for (int i = 0; i < columns.length; i++) {
                targetColumnsIndexes.add(i);
                targetColumnsNames.add(columns[i]);
            }
        } else {
            String[] selectedColumns = attributeList.split(",");
            for (String cols : selectedColumns) {
                String col = cols.trim();
                boolean found = false;
                for (int i = 0; i < columns.length; i++) {
                    if (columns[i].equalsIgnoreCase(col)) {
                        targetColumnsIndexes.add(i);
                        targetColumnsNames.add(columns[i]);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return "[ERROR]: Column " + col + " does not exist in table '" + tableName + "'.";
                }
            }
        }

        StringBuilder selectedRows = new StringBuilder();
        for (int i = 0; i < targetColumnsNames.size(); i++) {
            selectedRows.append(targetColumnsNames.get(i));
            if (i < targetColumnsNames.size() - 1) {
                selectedRows.append("\t");
            }
        }
        selectedRows.append("\n");

        for (int i = table.headerIndex + 1; i < table.tableEndIndex; i++) {
            String rows = table.lines.get(i);
            if (rows.trim().isEmpty()) {
                continue;
            }
            String[] rowValues = rows.split("\t", -1);
            boolean displayRow = true;
            if (conditionsList != null && !conditionsList.isEmpty()) {
                boolean rowResults = false;
                for (int j = 0; j < conditionsList.size(); j++) {
                    String[] condition = conditionsList.get(j);
                    int columnIndex = conditionColumnIndexes.get(j);
                    if (columnIndex == -1 || columnIndex >= rowValues.length) {
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
                displayRow = rowResults;
            }

            if (displayRow) {
                for (int j = 0; j < targetColumnsIndexes.size(); j++) {
                    int columnIndex = targetColumnsIndexes.get(j);
                    if (columnIndex < rowValues.length) {
                        selectedRows.append(rowValues[columnIndex]);
                    }
                    if (j < targetColumnsNames.size() - 1) {
                        selectedRows.append("\t");
                    }
                }
                selectedRows.append("\n");
            }
        }
        return "[OK] \n" + selectedRows;
    }


}
