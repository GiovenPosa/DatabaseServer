package edu.uob;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ConditionParser {
    private final List<String> conditions;
    private int currentIndex = 0;

    public ConditionParser (String command) {
        conditions = parseConditions(command);
    }

    private List<String> parseConditions (String command) {
        List<String> conditions = new ArrayList<>();
        Matcher conditionMatcher = Pattern.compile("\\(|\\)|AND|OR|==|>=|<=|!=|>|<|'[^']*'|\\w+").matcher(command);
        while (conditionMatcher.find()) {
            conditions.add(conditionMatcher.group());
        }
        return conditions;
    }

    //Class 'ConditionExpression' is exposed outside its defined visibility scope

    private ConditionExpression parsePrimary () {
        String condition = conditions.get(currentIndex);
        if ("(".equals(condition)) {
            currentIndex++;
            ConditionExpression expression = parseConditionExpression();
            currentIndex++;
            return expression;
        } else {
            String column = conditions.get(currentIndex++);
            String comparator = conditions.get(currentIndex++);
            String value = conditions.get(currentIndex++);
            return new ConditionLeaf(column, comparator, value);
        }
    }

    public ConditionExpression parseConditionExpression () {
        return parseOrExpression();
    }

    private ConditionExpression parseOrExpression () {
        ConditionExpression expression = parseAndExpression();
        while (currentIndex < conditions.size() && conditions.get(currentIndex).equalsIgnoreCase("OR")) {
            String operator = conditions.get(currentIndex++);
            ConditionExpression right = parseAndExpression();
            expression = new ConditionNode(expression, right, operator);
        }
        return expression;
    }

    private ConditionExpression parseAndExpression () {
        ConditionExpression expression = parsePrimary();
        while (currentIndex < conditions.size() && conditions.get(currentIndex).equalsIgnoreCase("AND")) {
            String operator = conditions.get(currentIndex++);
            ConditionExpression right = parsePrimary();
            expression = new ConditionNode(expression, right, operator);
        }
        return expression;
    }
}
