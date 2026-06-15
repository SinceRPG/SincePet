package net.danh.sincePet.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Calculator {
    private static final Pattern TOKEN_PATTERN = Pattern.compile("([a-zA-Z]+)|([0-9]*\\.?[0-9]+)|([+\\-*/%^#(),])");
    private static final Map<String, Integer> PRECEDENCE = new HashMap<>();

    static {
        PRECEDENCE.put("+", 1);
        PRECEDENCE.put("-", 1);
        PRECEDENCE.put("*", 2);
        PRECEDENCE.put("/", 2);
        PRECEDENCE.put("%", 2);
        PRECEDENCE.put("^", 3);
        PRECEDENCE.put("pow", 3);
        PRECEDENCE.put("#", 4);
        PRECEDENCE.put("NEG", 5);
    }

    public static String calculator(String expression, int decimalPlaces) {
        if (expression == null || expression.trim().isEmpty()) return "0";

        try {
            // Legacy support for @{...} -> sqrt(...)
            if (expression.contains("@{")) {
                expression = expression.replace("@{", "sqrt(").replace("}", ")");
            }
            // Remove whitespace
            expression = expression.replaceAll("\\s+", "");

            List<String> tokens = tokenize(expression);
            Queue<String> postfix = shuntingYard(tokens);
            double result = evaluatePostfix(postfix);

            if (Double.isInfinite(result) || Double.isNaN(result)) {
                return "0";
            }

            if (decimalPlaces >= 0) {
                return BigDecimal.valueOf(result)
                        .setScale(decimalPlaces, RoundingMode.HALF_UP)
                        .stripTrailingZeros()
                        .toPlainString();
            } else {
                return BigDecimal.valueOf(result)
                        .stripTrailingZeros()
                        .toPlainString();
            }

        } catch (Exception e) {
            return "0";
        }
    }

    private static List<String> tokenize(String expression) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(expression);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private static Queue<String> shuntingYard(List<String> tokens) {
        Queue<String> outputQueue = new LinkedList<>();
        Stack<String> operatorStack = new Stack<>();
        String previousToken = "";

        for (String token : tokens) {
            if (isNumber(token)) {
                outputQueue.add(token);
            } else if (isFunction(token) || isConstant(token)) {
                operatorStack.push(token);
            } else if (token.equals(",")) {
                while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                    outputQueue.add(operatorStack.pop());
                }
            } else if (isOperator(token)) {
                if (token.equals("-") && (previousToken.isEmpty() || isOperator(previousToken) || previousToken.equals("(") || previousToken.equals(","))) {
                    token = "NEG";
                }
                while (!operatorStack.isEmpty()
                        && !operatorStack.peek().equals("(")
                        && (isFunction(operatorStack.peek())
                        || getPrecedence(operatorStack.peek()) >= getPrecedence(token))) {
                    outputQueue.add(operatorStack.pop());
                }
                operatorStack.push(token);
            } else if (token.equals("(")) {
                operatorStack.push(token);
            } else if (token.equals(")")) {
                while (!operatorStack.isEmpty() && !operatorStack.peek().equals("(")) {
                    outputQueue.add(operatorStack.pop());
                }
                if (!operatorStack.isEmpty()) operatorStack.pop();
                if (!operatorStack.isEmpty() && isFunction(operatorStack.peek())) {
                    outputQueue.add(operatorStack.pop());
                }
            }
            previousToken = token;
        }
        while (!operatorStack.isEmpty()) {
            outputQueue.add(operatorStack.pop());
        }
        return outputQueue;
    }

    private static double evaluatePostfix(Queue<String> postfix) {
        Stack<Double> stack = new Stack<>();

        for (String token : postfix) {
            if (isNumber(token)) {
                stack.push(Double.parseDouble(token));
            } else if (isConstant(token)) {
                if (token.equalsIgnoreCase("pi")) stack.push(Math.PI);
                else if (token.equalsIgnoreCase("e")) stack.push(Math.E);
            } else {
                applyOperatorOrFunction(token, stack);
            }
        }
        return stack.isEmpty() ? 0 : stack.pop();
    }

    private static void applyOperatorOrFunction(String op, Stack<Double> stack) {
        if (op.equals("NEG")) {
            if (!stack.isEmpty()) stack.push(-stack.pop());
            return;
        }
        if (op.equals("#")) {
            if (!stack.isEmpty()) stack.push(stack.pop() / 100.0);
            return;
        }

        if (stack.isEmpty()) return;

        if (isFunction(op) && !op.equalsIgnoreCase("max") && !op.equalsIgnoreCase("min") && !op.equalsIgnoreCase("pow")) {
            double val = stack.pop();
            switch (op.toLowerCase()) {
                case "sin":
                    stack.push(Math.sin(val));
                    break;
                case "cos":
                    stack.push(Math.cos(val));
                    break;
                case "tan":
                    stack.push(Math.tan(val));
                    break;
                case "sqrt":
                    stack.push(Math.sqrt(val));
                    break;
                case "abs":
                    stack.push(Math.abs(val));
                    break;
                case "floor":
                    stack.push(Math.floor(val));
                    break;
                case "ceil":
                    stack.push(Math.ceil(val));
                    break;
                case "round":
                    stack.push((double) Math.round(val));
                    break;
                case "log":
                    stack.push(Math.log10(val));
                    break;
                case "ln":
                    stack.push(Math.log(val));
                    break;
                default:
                    break;
            }
            return;
        }

        if (stack.size() < 2) return;
        double b = stack.pop();
        double a = stack.pop();

        switch (op.toLowerCase()) {
            case "+":
                stack.push(a + b);
                break;
            case "-":
                stack.push(a - b);
                break;
            case "*":
                stack.push(a * b);
                break;
            case "/":
                stack.push(b == 0 ? 0 : a / b);
                break;
            case "%":
                stack.push(a % b);
                break;
            case "^":
            case "pow":
                stack.push(Math.pow(a, b));
                break;
            case "max":
                stack.push(Math.max(a, b));
                break;
            case "min":
                stack.push(Math.min(a, b));
                break;
        }
    }

    private static boolean isNumber(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isFunction(String s) {
        return s.matches("[a-zA-Z]+") && !isConstant(s);
    }

    private static boolean isConstant(String s) {
        return s.equalsIgnoreCase("pi") || s.equalsIgnoreCase("e");
    }

    private static boolean isOperator(String s) {
        return PRECEDENCE.containsKey(s) || s.equals("NEG");
    }

    private static int getPrecedence(String s) {
        return PRECEDENCE.getOrDefault(s, 0);
    }
}
