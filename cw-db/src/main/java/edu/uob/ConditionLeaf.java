package edu.uob;

class ConditionLeaf extends ConditionExpression {
    String column;
    String comparator;
    String value;

    public ConditionLeaf(String column, String comparator, String value) {
        this.column = column;
        this.comparator = comparator;
        this.value = value;
    }

    @Override
    public boolean evaluate(String[] rowValues, String[] header) {
        int index = -1;
        for (int i = 0; i < header.length; i++) {
            if (header[i].equalsIgnoreCase(column)) {
                index = i;
                break;
            }
        }
        if (index == -1 || index >= rowValues.length) {
            return false;
        }
        String cellValue = rowValues[index].trim();
        return QueryCmdHandler.evaluateCondition(cellValue, comparator, value);
    }
}
