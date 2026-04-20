package com.kris.mithril;

public class Token {
    final TokenType type;
    final String lexeme;
    final Object literal;
    final int line;

    public Token(TokenType type, String lexeme, Object literal, int line) {
        this.lexeme = lexeme;
        this.type = type;
        this.literal = literal;
        this.line = line;
    }

    public String toString(){
        return type + " " + lexeme + " " + literal;
    }
}
