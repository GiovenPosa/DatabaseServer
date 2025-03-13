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
        String conditionCmd = command.substring(getIndex + " WHERE ".length());

        if (!parseTableName(fromCmd)) {
            return "[ERROR]: Invalid SELECT syntax. Use syntax DELETE FROM <tableName>.";
        }

        if (conditionCmd.isEmpty()) {
            return "[ERROR]: No conditions provided in DELETE query.";
        }

        ConditionParser conditionParser = new ConditionParser(conditionCmd);
        ConditionExpression condition = conditionParser.parseConditionExpression();
        if (condition == null) {
            return "[ERROR]: Failed to parse condition syntax.";
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

        try {
            validateCondition(condition, columns);
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
            boolean deletedRow = condition.evaluate(rowValues, columns);
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
