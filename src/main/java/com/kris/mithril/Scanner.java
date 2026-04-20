package com.kris.mithril;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.kris.mithril.TokenType.*;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int curr = 0;
    private int line = 1;

    private static final HashMap<String, TokenType> keywords = new HashMap<>();

    static {
        keywords.put("rune", RUNE);
        keywords.put("forge", FORGE);
        keywords.put("quest", QUEST);
        keywords.put("bear", BEAR);
        keywords.put("speak", SPEAK);
        keywords.put("should", SHOULD);
        keywords.put("lest", LEST);
        keywords.put("wander", WANDER);
        keywords.put("whilst", WHILST);
        keywords.put("in", IN);
        keywords.put("fellowship", FELLOWSHIP);
        keywords.put("mine", MINE);
        keywords.put("light", LIGHT);
        keywords.put("shadow", SHADOW);
        keywords.put("naught", NAUGHT);
        keywords.put("doom", DOOM);
        keywords.put("ward", WARD);
        keywords.put("and", AND);
        keywords.put("or", OR);
        keywords.put("wise", WISE);
        keywords.put("wizard", WIZARD);
    }

    Scanner(String source){
        this.source = source;
    }

    List<Token> scanTokens(){
        while (!isFinished()){
            start = curr;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken(){
        if (matchEmoji("🧙")){
            addToken(WIZARD, "🧙");
            return;
        }

        char c = advance();
        switch(c){
//          Single
            case '(': addToken(LEFT_PARENTHESIS); break;
            case ')': addToken(RIGHT_PARENTHESIS); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;
            case '/': addToken(SLASH); break;
//          Double
            case '!':
                addToken(matches('=') ? BANG_EQUAL : BANG);
                break;
            case '>':
                addToken(matches('=') ? GREATER_EQUAL : GREATER);
                break;
            case '<':
                addToken(matches('=') ? LESS_EQUAL : LESS);
                break;
            case '=':
                addToken(matches('=') ? EQUAL_EQUAL : EQUAL);
                break;
//          Comments:
//              @ -> oneliners,
//              -- comment -- -> multiple lines
            case '@':
                while (!isFinished() && peek() != '\n'){
                    advance();
                }
                break;
            case '-':
                if (matches('-')){
                    while (!isFinished()){
                        char currChar = advance();
                        if (currChar == '\n') line++;
                        if (currChar == '-' && matches('-')) break;
                    }
                } else {
                    addToken(MINUS);
                }
                break;
//          Meaningless
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                break;

//          Literals:
//              Strings
            case '"':
                while (!isFinished() && peek() != '"'){
                    if (peek() == '\n') line++;
                    advance();
                }

                if (isFinished()){
                    Mithril.error(line,"Unterminated string");
                    return;
                }

                advance();

                String string = source.substring(start + 1, curr - 1);
                addToken(STRING, string);
                break;
//          Unexpected Chars or Number literals
            default:
                if (Character.isDigit(c)){
                    while (Character.isDigit(peek())){
                        advance();
                    }

                    if (peek() == '.' && Character.isDigit(peekNext())){
                        advance();
                        while (Character.isDigit(peek())){
                            advance();
                        }
                    }
                    Double value = Double.parseDouble(source.substring(start, curr));
                    addToken(NUMBER, value);
                } else if (isAlpha(c)) {
                    while(isAlnum(peek())) advance();
                    String text = source.substring(start, curr);
                    System.out.println(text);
                    TokenType type = keywords.get(text);
                    if(type == null) type = IDENTIFIER;
                    addToken(type);
                } else {
                    Mithril.error(line, "Unexpected character.");
                }
                break;
        }
    }

    private char advance(){
        return source.charAt(curr++);
    }

    private char peek(){
        if (isFinished()) return '\0';
        return source.charAt(curr);
    }

    private char peekNext(){
        if (curr + 1 >= source.length()) return '\0';
        return source.charAt(curr + 1);
    }

    private void addToken(TokenType type){
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal){
        String text = source.substring(start, curr);
        tokens.add(new Token(type, text, literal, line));
    }

    private boolean matches(Character character){
        if (isFinished()) return false;
        if (peek() != character) return false;

        curr++;
        return true;
    }

    private boolean isFinished(){
        return curr >= source.length();
    }

    private boolean matchEmoji(String emoji){
        if (curr + emoji.length() > source.length()) return false;

        if(source.startsWith(emoji, curr)){
            curr += emoji.length();
            return true;
        }
        return false;
    }

    private boolean isAlpha(char c){
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlnum(char c){
        return isAlpha(c) || Character.isDigit(c);
    }

}
