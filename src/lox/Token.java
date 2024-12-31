package lox;

public class Token {
    final TokenType type;
    final String lexeme; // string inside the source that produced this token
    final Object literal; // a literal interpretation of the token, if there is one
    final int line; // line of the lexeme inside the source

    Token (TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        return type + " '" + lexeme + "' <- " + literal;
    }
}