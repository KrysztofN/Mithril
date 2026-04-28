package com.kris.mithril;

import java.util.ArrayList;
import java.util.List;

import static com.kris.mithril.TokenType.*;

public class Parser {
    private static class ParseError extends RuntimeException {}
    private final List<Token> tokens;
    private int curr = 0;

    Parser(List<Token> tokens){
        this.tokens = tokens;
    }

    List<Stmt> parseProgram(){
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()){
            Stmt stmt = parseStatement();
            if(stmt != null) statements.add(stmt);
        }
        return statements;
    }

    private Stmt parseStatement(){
        try {
            if (match(SPEAK)) return parseSpeakStmt();
            if (match(RUNE)) return parseVarDeclaration(false);
            if (match(FORGE)) return parseVarDeclaration(true);
            if (match(SHOULD)) return parseShouldStmt();
            if (match(WHILST)) return parseWhilstStmt();
            if (match(LEFT_BRACE)) return parseBlock();

            if (check(IDENTIFIER) && checkNext(EQUAL)) return parseAssign();

            return parseExpressionStmt();
        } catch (ParseError error){
            synchronize();
            return null;
        }
    }

    private Stmt parseShouldStmt() {
        consume(LEFT_PARENTHESIS, "Expected '(' after 'should'.");
        Expr condition = expression();
        consume(RIGHT_PARENTHESIS, "Expected ')' after condition.");

        consume(LEFT_BRACE, "Expected '{' before 'should' body.");
        Stmt.Block thenBlock = parseBlock();

        Stmt.Block elseBlock = null;
        if (match(LEST)) {
            consume(LEFT_BRACE, "Expected '{' before 'lest' body.");
            elseBlock = parseBlock();
        }

        return new Stmt.Should(condition, thenBlock, elseBlock);
    }

    private Stmt parseWhilstStmt() {
        consume(LEFT_PARENTHESIS, "Expected '(' after 'whilst'.");
        Expr condition = expression();
        consume(RIGHT_PARENTHESIS, "Expected ')' after condition.");

        consume(LEFT_BRACE, "Expected '{' before 'whilst' body.");
        Stmt.Block body = parseBlock();

        return new Stmt.Whilst(condition, body);
    }

    private Stmt.Block parseBlock() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            Stmt stmt = parseStatement();
            if (stmt != null) statements.add(stmt);
        }

        consume(RIGHT_BRACE, "Expected '}' after block.");
        return new Stmt.Block(statements);
    }

    private Stmt parseSpeakStmt(){
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' after speak value.");
        return new Stmt.Speak(value);
    }

    private Stmt parseVarDeclaration(boolean mutable) {
        Token name = consume(IDENTIFIER, "Expected variable name.");
        consume(EQUAL, "Expected '=' after variable name.");
        Expr initializer = expression();
        consume(SEMICOLON, "Expected ';' after variable declaration.");
        return new Stmt.VarDeclaration(name, initializer, mutable);
    }

    private Stmt parseAssign() {
        Token name = advance();
        advance();
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' after assignment.");
        return new Stmt.Assign(name, value);
    }

    private Stmt parseExpressionStmt() {
        Expr expr = expression();
        consume(SEMICOLON, "Expected ';' after expression.");
        return new Stmt.Expression(expr);
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
        return peek().type() == type;
    }

    private Token advance(){
        if (!isAtEnd()) curr++;
        return previous();
    }

    private boolean isAtEnd(){
        return peek().type() == EOF;
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
            return new Expr.Literal(previous().literal());
        }

        if (match(IDENTIFIER)) return new Expr.Variable(previous());

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

    private boolean checkNext(TokenType type){
        if (curr + 1 >= tokens.size()) return false;
        return tokens.get(curr + 1).type() == type;
    }

    private ParseError error(Token token, String message){
        Mithril.error(token, message);
        return new ParseError();
    }

    private void synchronize(){
        advance();

        while (!isAtEnd()){
            if (previous().type() == SEMICOLON) return;

            switch (peek().type()) {
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
