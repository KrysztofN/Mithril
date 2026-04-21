package com.kris.mithril;

import java.util.List;

import static com.kris.mithril.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int curr = 0;

    Parser(List<Token> tokens){
        this.tokens = tokens;
    }

    Expr parse(){
        try {
            return expression();
        } catch (ParseError error){
            return null;
        }
    }

    private Expr expression(){
        return equality();
    }

    private Expr equality(){
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)){
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private boolean match(TokenType... types){
        for (TokenType type: types) {
            if (check(type)){
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type){
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance(){
        if (!isAtEnd()) curr++;
        return previous();
    }

    private boolean isAtEnd(){
        return peek().type == EOF;
    }

    private Token peek(){
        return tokens.get(curr);
    }

    private Token previous(){
        return tokens.get(curr - 1);
    }

    private Expr comparison(){
        Expr expr = term();

        while(match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)){
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term(){
        Expr expr = factor();

        while (match(MINUS, PLUS)){
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor(){
        Expr expr = unary();

        while (match(STAR, SLASH)){
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary(){
        if (match(BANG, MINUS)){
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(LIGHT)) return new Expr.Literal(true);
        if (match(SHADOW)) return new Expr.Literal(false);
        if (match(NAUGHT)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PARENTHESIS)) {
            Expr expr = expression();
            consume(RIGHT_PARENTHESIS, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expression expected.");
    }

    private Token consume(TokenType type, String message){
        if(check(type)) return advance();

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message){
        Mithril.error(token, message);
        return new ParseError();
    }

    private void synchronize(){
        advance();

        while (!isAtEnd()){
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case FELLOWSHIP:
                case QUEST:
                case FORGE:
                case WANDER:
                case SHOULD:
                case WHILST:
                case SPEAK:
                case BEAR:
                    return;
            }

            advance();
        }
    }
}
