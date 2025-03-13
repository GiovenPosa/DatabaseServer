package edu.uob;

class ConditionNode extends ConditionExpression {
    ConditionExpression left;
    ConditionExpression right;
    String operator;

    public ConditionNode(ConditionExpression left, ConditionExpression right, String operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public boolean evaluate(String[] rowValues, String[] header) {
        if ("AND".equalsIgnoreCase(operator)) {
            return left.evaluate(rowValues, header) && right.evaluate(rowValues, header);
        } else if ("OR".equalsIgnoreCase(operator)) {
            return left.evaluate(rowValues, header) || right.evaluate(rowValues, header);
        }
        return false;
    }
}
