package com.kris;

public class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    public Token(String lexeme, TokenType type, Object literal, int line) {
        this.lexeme = lexeme;
        this.type = type;
        this.literal = literal;
        this.line = line;
    }

    public String toString(){
        return type + " " + lexeme + " " + literal;
    }
}
