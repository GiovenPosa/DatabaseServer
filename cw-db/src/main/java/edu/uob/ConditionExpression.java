package edu.uob;

abstract class ConditionExpression {
    public abstract boolean evaluate(String[] rowValues, String[] header);
}
