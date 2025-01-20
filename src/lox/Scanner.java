package lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lox.TokenType.*;

public class Scanner {
    final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private static final Map<String, TokenType> keywords = new HashMap<>();
    static {
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, null, null, line));
        return tokens;
    }

    // each call will parse a full lexeme 
    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break; 
            case ':': addToken(COLON); break;
            case '?': addToken(QUESTION); break;
            case '!':
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;
            case '>':
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;
            case '=':
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;
            case '<':
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;
            case '/':
                if (match('/')) {
                    while (!isAtEnd() && peek() != '\n') advance();
                } else if (match('*')) {
                    for (;;) {
                        if (isAtEnd()) {Lox.error(line, "unfinished multi-line comment"); break;}
                        
                        if (advance() == '*') {
                            if (advance() == '/') break;
                        }
                    }
                } else addToken(SLASH);
                break;
            case '"': string(); break;
            case '\n':
                ++line;
            case '\r':
            case ' ':
            case '\t':
                break;
            default: 
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else Lox.error(line, "undefined character: "+c);
            }
    }

    private void string() {
        while (peek() != '"') {
            if(isAtEnd()) {
                Lox.error(line, "unterminated string");
                return;
            }
            if (peek() == '\n') ++line;
            advance();
        }
        advance(); // skip over ending "

        addToken(STRING, source.substring(start + 1, current - 1));
    }

    private boolean isDigit(char c) {
        return ('0' <= c && c <= '9');
    }

    private void number() {
        while (isDigit(peek())) advance();
        if (peek() == '.') {
            advance();
            while (isDigit(peek())) advance();
        }
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private boolean isAlpha(char c) {
        return(('A' <= c && c <= 'Z')
            || ('a' <= c && c <= 'z')
            ||  '_' == c);
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String key = source.substring(start, current);
        addToken(keywords.getOrDefault(key, IDENTIFIER));
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (peek() != expected) return false;
        ++current;
        return true;
    }

    private char advance() {
        // TODO: why is `source` a String if we end up consuming characters???
        // returning '\0' avoids throwing StringIndexOutOfBoundsException 
        return isAtEnd() ? '\0' : source.charAt(current++);
    }

    private char peek() {   
        return isAtEnd() ? '\0' : source.charAt(current);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
