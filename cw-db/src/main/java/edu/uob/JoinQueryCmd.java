package edu.uob;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JoinQueryCmd extends QueryCmdHandler {
    private String tableName1;
    private String tableName2;
    private String attributeList1;
    private String attributeList2;

    public JoinQueryCmd(String destinationPath, String selectedFile) {
        super(destinationPath, selectedFile);
    }

    private boolean parseTableNames(String command) {
        Pattern tableNamePattern = Pattern.compile("^(\\w+)\\s+AND\\s+(\\w+)$", Pattern.CASE_INSENSITIVE);
        Matcher tableNameMatcher = tableNamePattern.matcher(command);
        if (!tableNameMatcher.matches()) {
            return false;
        }

        tableName1 = tableNameMatcher.group(1).trim();
        tableName2 = tableNameMatcher.group(2).trim();
        return true;
    }

    private boolean parseAttributeNames(String command) {
        Pattern attributePattern = Pattern.compile("^([\\w\\s,]+)\\s+AND\\s+([\\w\\s,]+)$", Pattern.CASE_INSENSITIVE);
        Matcher attributeMatcher = attributePattern.matcher(command);
        if (!attributeMatcher.matches()) {
            return false;
        }

        attributeList1 = attributeMatcher.group(1).trim();
        attributeList2 = attributeMatcher.group(2).trim();
        return true;

    }

    public String execute(String command) {
        int getIndex = command.indexOf(" ON ");
        if (getIndex == -1) {
            return "[ERROR]: Invalid Join syntax. Use JOIN <tableName1> AND <tableName2> ON <attributeName1> AND <attributeName2>";
        }
        String tableNamesCmd = command.substring(0, getIndex).trim();
        String attributeCmd = command.substring(getIndex + " ON ".length()).trim();

        if (!parseTableNames(tableNamesCmd)) {
            return "[ERROR]: Invalid Join syntax. Use JOIN <tableName1> AND <tableName2> ON <attributeName1> AND <attributeName2>";
        }

        if (!parseAttributeNames(attributeCmd)) {
            return "[ERROR]: Invalid Join syntax. Use JOIN <tableName1> AND <tableName2> ON <attributeName1> AND <attributeName2>";
        }

        TableBlock table1 = getTableBlock(tableName1);
        if (table1 == null) {
            return "[ERROR]: Unable to read table '" + tableName1 + "'. File may not exist.";
        }

        TableBlock table2 = getTableBlock(tableName2);
        if (table2 == null) {
            return "[ERROR]: Unable to read table '" + tableName2 + "'. File may not exist.";
        }

        String headerLine1 = table1.lines.get(table1.headerIndex).trim();
        String headerLine2 = table2.lines.get(table2.headerIndex).trim();
        String[] columns1 = headerLine1.split("\t");
        String[] columns2 = headerLine2.split("\t");

        int joinIndex1 = -1;
        int joinIndex2 = -1;
        for (int i = 0; i < columns1.length; i++) {
            if (columns1[i].trim().equalsIgnoreCase(attributeList1)) {
                joinIndex1 = i;
                break;
            }
        }
        for (int i = 0; i < columns2.length; i++) {
            if (columns2[i].trim().equalsIgnoreCase(attributeList2)) {
                joinIndex2 = i;
                break;
            }
        }
        if (joinIndex1 == -1) {
            return "[ERROR]: Join attribute '" + attributeList1 + "' does not exist in table '" + tableName1 + "'.";
        }
        if (joinIndex2 == -1) {
            return "[ERROR]: Join attribute '" + attributeList2 + "' does not exist in table '" + tableName2 + "'.";
        }

        List<String> newHeader = getHeaders(tableName1, columns1, tableName2, columns2);

        StringBuilder joinedResult = new StringBuilder();
        joinedResult.append((String.join("\t", newHeader)));
        joinedResult.append("\n");

        int newID = 1;
        for (int i = table1.headerIndex + 1; i < table1.tableEndIndex; i++) {
            String row1 = table1.lines.get(i);
            if (row1.trim().isEmpty()) continue;
            String[] rowValues1 = row1.split("\t", -1);
            if (rowValues1.length <= joinIndex1) continue;
            String joinValue1 = rowValues1[joinIndex1].trim();

            for (int j = table2.headerIndex + 1; j < table2.tableEndIndex; j++) {
                String row2 = table2.lines.get(j);
                if (row2.trim().isEmpty()) continue;
                String[] rowValues2 = row2.split("\t", -1);
                if (rowValues2.length <= joinIndex2) continue;
                String joinValue2 = rowValues2[joinIndex2].trim();

                if (joinValue1.equals(joinValue2)) {
                    List<String> joinedRow = new ArrayList<>();
                    joinedRow.add(String.valueOf(newID));
                    for (int k = 0; k < rowValues1.length; k++) {
                        if (columns1[k].equalsIgnoreCase("id")) continue;
                        joinedRow.add(rowValues1[k]);
                    }
                    for (int k = 0; k < rowValues2.length; k++) {
                        if (columns2[k].equalsIgnoreCase("id") || columns2[k].equalsIgnoreCase(attributeList2)) continue;
                        joinedRow.add(rowValues2[k]);
                    }
                    joinedResult.append(String.join("\t", joinedRow));
                    joinedResult.append("\n");
                    newID++;
                }
            }
        }
        return "[OK]: \n" + joinedResult;
    }

    private List<String> getHeaders(String tableName1, String[] columns1, String tableName2, String[] columns2) {
        List<String> newHeader = new ArrayList<>();
        newHeader.add("id");
        for (String column : columns1) {
            if (!column.equalsIgnoreCase("id")) {
                newHeader.add(tableName1 + "." + column);
            }
        }
        for (String column : columns2) {
            if (column.equalsIgnoreCase(attributeList2)) {
                continue;
            }
            if (!column.equalsIgnoreCase("id")) {
                newHeader.add(tableName2 + "." + column);
            }
        }
        return newHeader;
    }
}
