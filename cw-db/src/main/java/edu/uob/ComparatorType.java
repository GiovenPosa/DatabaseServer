package edu.uob;

public enum ComparatorType {
    EQUAL("=="),
    GREATER(">"),
    LESS("<"),
    GREATER_OR_EQUAL(">="),
    LESS_OR_EQUAL("<="),
    NOT_EQUAL("!="),
    LIKE("LIKE");

    private final String symbol;

    ComparatorType(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static ComparatorType fromSymbol(String symbol) {
        for (ComparatorType operator : values()) {
            if (operator.getSymbol().equals(symbol.trim())) {
                return operator;
            }
        }
        throw new IllegalArgumentException("[ERROR]: " + symbol + " is not a valid comparator type");
    }

}
