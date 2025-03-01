package com.minenash.customhud.conditionals;

import com.minenash.customhud.ComplexData;
import com.minenash.customhud.HudElements.HudElement;
import com.minenash.customhud.HudElements.StringElement;
import com.minenash.customhud.VariableParser;

import java.util.ArrayList;
import java.util.List;

public class ConditionalParser {

    enum TokenType { START_PREN, END_PREN, FULL_PREN, AND, OR, COMPARISON, NUMBER, STRING, BOOLEAN, VARIABLE }
    enum Conditionals { LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUALS, EQUALS, NOT_EQUALS }

    record Token(TokenType type, Object value) {
        public String toString() {
            return type + (value == null ? "" : " (" + value + ")");
        }
    }

    public static Conditional parseConditional(String input, int debugLine, ComplexData.Enabled enabled) {
        try {
            List<Token> tokens = getTokens(input, debugLine, enabled);
            Conditional c = getConditional(tokens);
            System.out.println("Tree for Conditional on line " + debugLine + ":");
            c.printTree(0);
            System.out.println();
            return c;
        }
        catch (Exception e) {
            System.out.println("Conditional Couldn't Be Parsed: " + e.getMessage());
            return new Conditional.Literal(true);
        }
    }

    private static List<Token> getTokens(String original, int debugLine, ComplexData.Enabled enabled) {

        List<Token> tokens = new ArrayList<>();
        char[] chars = original.toCharArray();

        for (int i = 0; i < chars.length;) {
            char c = chars[i];
            if (c == '(') tokens.add(new Token(TokenType.START_PREN, null));
            else if (c == ')') tokens.add(new Token(TokenType.END_PREN, null));
            else if (c == '|') tokens.add(new Token(TokenType.OR, null));
            else if (c == '&') tokens.add(new Token(TokenType.AND, null));
            else if (c == '=') tokens.add(new Token(TokenType.COMPARISON, Conditionals.EQUALS));
            else if (c == '!') {
                if (i + 1 == chars.length || chars[i + 1] != '=')
                    throw new IllegalStateException();
                tokens.add(new Token(TokenType.COMPARISON, Conditionals.NOT_EQUALS));
                i += 2;
                continue;
            }
            else if (c == '>') {
                boolean hasEqual =  i + 1 != chars.length && chars[i + 1] == '=';
                tokens.add(new Token(TokenType.COMPARISON, hasEqual ? Conditionals.GREATER_THAN_OR_EQUALS : Conditionals.GREATER_THAN));
                i += hasEqual ? 2 : 1;
                continue;
            }
            else if (c == '<') {
                boolean hasEqual =  i + 1 != chars.length && chars[i + 1] == '=';
                tokens.add(new Token(TokenType.COMPARISON, hasEqual ? Conditionals.LESS_THAN_OR_EQUAL : Conditionals.LESS_THAN));
                i += hasEqual ? 2 : 1;
                continue;
            }
            else if (c == 'f' && i + 4 < chars.length && original.startsWith("false", i)) {
                tokens.add(new Token(TokenType.BOOLEAN, false));
                i+=5;
                continue;
            }
            else if (c == 't' && i + 3 < chars.length && original.startsWith("true", i)) {
                tokens.add(new Token(TokenType.BOOLEAN, true));
                i+=4;
                continue;
            }
            else if (c == '"') {
                StringBuilder builder = new StringBuilder();
                i++;
                while (i < chars.length && chars[i] != '"')
                    builder.append(chars[i++]);
                tokens.add(new Token(TokenType.STRING, builder.toString()));
            }
            else if (isNum(c)) {
                StringBuilder builder = new StringBuilder();
                while (i < chars.length && isNum(chars[i]))
                    builder.append(chars[i++]);
                tokens.add(new Token(TokenType.NUMBER, Double.parseDouble(builder.toString())));
                continue;
            }
            else if (isVar(c)) {
                StringBuilder builder = new StringBuilder();
                builder.append('{');
                while (i < chars.length && isVar(chars[i])) {
                    builder.append(chars[i]);
                    i++;
                }
                builder.append('}');
                tokens.add(new Token(TokenType.VARIABLE, VariableParser.parseElement(builder.toString(), debugLine, enabled)));
                continue;
            }
            i++;

        }

//        for (Token token : tokens)
//            System.out.println("[A]" + token);

        int start = -1;
        for (int i = 0; i < tokens.size(); i++) {
            TokenType type = tokens.get(i).type();
            if (type == TokenType.START_PREN) {
                start = i;
            }
            else if (type == TokenType.END_PREN) {
                reduceList(tokens, start, i);
                start = -1;
                i = -1;
            }

        }

//        System.out.println("---------------");
//        for (Token token : tokens)
//            System.out.println("[B]" + token);
        return tokens;

    }

    private static boolean isNum(char c) {
        return c == '.' || (c >= '0' && c <= '9');
    }
    private static boolean isVar(char c) {
        return c == ':' || c == '_' || (c >= 'a' && c <= 'z');
    }
    private static void reduceList(List<Token> original, int start, int end) {
        original.set(start, new Token(TokenType.FULL_PREN, new ArrayList<>(original.subList(start+1, end))));
        for (; end > start; end--)
            original.remove(end);
    }

    private static Conditional getConditional(List<Token> tokens) {
        List<List<Token>> ors = split(tokens, TokenType.OR);
        List<Conditional> conditionals = new ArrayList<>();
        for (var or : ors)
            conditionals.add(getAndConditional(or));

        return conditionals.size() == 1 ? conditionals.get(0) : new Conditional.Or(conditionals);

    }

    private static Conditional getAndConditional(List<Token> tokens) {
        List<List<Token>> ands = split(tokens, TokenType.AND);
        List<Conditional> conditionals = new ArrayList<>();
        for (var and : ands)
            conditionals.add(getComparisonConditional(and));

        return conditionals.size() == 1 ? conditionals.get(0) : new Conditional.And(conditionals);
    }

    @SuppressWarnings("unchecked")
    private static Conditional getComparisonConditional(List<Token> tokens) {

//        System.out.println(tokens.size());
//        System.out.println(tokens);
        if (tokens.size() == 1) {
            Token token = tokens.get(0);
            switch (token.type) {
                case FULL_PREN: return getConditional( (List<Token>) token.value());
                case BOOLEAN: return new Conditional.Literal( (Boolean) token.value());
                case VARIABLE: return new Conditional.BooleanVariable( (HudElement) token.value());
            }
            throw new IllegalStateException("Unexpected value: " + tokens.get(0).type());
        }
        if (tokens.size() != 3 || tokens.get(1).type() != TokenType.COMPARISON)
            throw new IllegalStateException(tokens.size() != 3? "Wrong number of tokens" : "Unexpected value: " + tokens.get(1).type());

        boolean checkBool = false;
        boolean checkNum = false;
        HudElement left = switch (tokens.get(0).type()) {
            case VARIABLE -> (HudElement) tokens.get(0).value();
            case STRING -> new StringElement((String)tokens.get(0).value());
            case NUMBER -> { checkNum = true; yield new SudoHudElements.Num((Number)tokens.get(0).value()); }
            case BOOLEAN -> {checkBool = true; yield new SudoHudElements.Bool((Boolean)tokens.get(0).value());}
            default -> throw new IllegalStateException("Unexpected value: " + tokens.get(0).type());
        };
        HudElement right = switch (tokens.get(2).type()) {
            case VARIABLE -> (HudElement) tokens.get(2).value();
            case STRING -> new StringElement((String)tokens.get(2).value());
            case NUMBER -> { checkNum = true; yield new SudoHudElements.Num((Number)tokens.get(2).value()); }
            case BOOLEAN -> {checkBool = true; yield new SudoHudElements.Bool((Boolean)tokens.get(2).value());}
            default -> throw new IllegalStateException("Unexpected value: " + tokens.get(2).type());
        };

        return new Conditional.Comparison(left, right, (Conditionals) tokens.get(1).value(), checkBool, checkNum);
    }

    private static List<List<Token>> split(List<Token> tokens, TokenType type) {
        List<List<Token>> sections = new ArrayList<>();
        List<Token> current = new ArrayList<>();

        for (Token token : tokens) {
            if (token.type() == type) {
                sections.add(current);
                current = new ArrayList<>();
            }
            else
                current.add(token);
        }
        sections.add(current);
        return sections;
    }



}