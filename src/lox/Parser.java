package lox;

import static lox.TokenType.*;
import static lox.Expr.*;
import static lox.Stmt.*;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    // use RuntimeExceptions when the caller is doing something wrong.
    private static class ParseError extends RuntimeException {}

    // not an Iterable because we need an index accessor method
    private final List<Token> tokens;
    private int current = 0; // current hasn't been accessed yet

    Parser(final List<Token> tokens) {
        this.tokens = tokens;
    }

    private boolean isAtEnd() {return tokens.get(current).type == EOF;}

    Token peek() {return isAtEnd() ? null : tokens.get(current);}

    Token advance() {return isAtEnd() ? null : tokens.get(current++);}

    Token previous() {return tokens.get(current-1);}

    // private boolean matchSequence(TokenType... tokenTypes) {
    //     for (TokenType tokenType : tokenTypes) {
    //         if (tokenType != advance().type) return false;
    //     }
    //     return true;
    // }

    private boolean match(TokenType... tokenTypes) {
        Token match = peek();
        if (null == match) return false;
        for (TokenType tokenType : tokenTypes) {
            if (tokenType == match.type) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (type == peek().type) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                default:
            }
            advance();
        }
    }

    /* for a literal token, every rule before it will be matched/called.
    * this is possible because the parts that really make the grammar rules
    * unique, are optional. so we can "drill down" until we hit literals. */

    /* an interesting edge case is when some character is a valid token (passes scanner)
    * but is not a valid nor invalid/error production in the parser. 
    * for example, `1=` produces the AST `1.0` and no errors (as of last edit). */

    private Stmt statement() {
        if (match(PRINT)) {
            Expr value = expression();
            consume(SEMICOLON, "expected ';' after print statement");
            return new PrintStmt(value);
        };
        // fallthrough case
        Expr value = expression();
        consume(SEMICOLON, "expected ';' after statement");
        return new ExpressionStmt(value);
    }
    
    private Expr expression() {
        Expr expr = equality();
        // if (match(COMMA)) {
        //     expr = new BinaryExpr(expr, previous(), expression());
        // }
        // if (match(QUESTION)) {
        //     Token question = previous();
        //     Expr branch1 = expression();
        //     if (!match(COLON)) error(peek(), "second branch of ternary operator must be defined following a `:`, which is missing");
        //     Token colon = previous();
        //     Expr branch2 = expression();
        //     expr = new BinaryExpr(expr, question, new BinaryExpr(branch1, colon, branch2));
        // } else while(match(COMMA)) {
        //     expr = new BinaryExpr(expr, previous(), equality());
        // }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token middle = previous();
            Expr right = comparison();
            // left-to-right precedence
            expr = new BinaryExpr(expr, middle, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token middle = previous();
            Expr right = term();
            // left-to-right precedence
            expr = new BinaryExpr(expr, middle, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(PLUS, MINUS)) {
            Token middle = previous();
            Expr right = factor();
            // left-to-right precedence
            expr = new BinaryExpr(expr, middle, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token middle = previous();
            Expr right = unary();
            // left-to-right precedence
            expr = new BinaryExpr(expr, middle, right);
        }
        return expr;
    }

    private Expr unary() {
        return match(BANG, MINUS)
        ? new UnaryExpr(previous(), unary())
        : primary();
    }

    private Expr primary() {
        if (match(RIGHT_PAREN)) 
        if (match(RIGHT_PAREN)) 
            throw error(previous(), "closing parenthesis don't match an opening parenthesis");
        if (match(
            BANG, MINUS, SLASH, STAR, PLUS, MINUS, 
            GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, BANG_EQUAL, EQUAL_EQUAL
        )) throw error(previous(), "binary operator not preceded by a complete expression");
        if (match(FALSE)) return new LiteralExpr(false);
        if (match(TRUE)) return new LiteralExpr(true);
        if (match(NIL)) return new LiteralExpr(null);

        if (match(NUMBER, STRING)) return new LiteralExpr(previous().literal);

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "expected ')' after expression");
            return new GroupingExpr(expr);
        }
        throw error(peek(), "expected expression");
    }

    Iterable<Stmt> parse() {
        // try {
            List<Stmt> statements = new ArrayList<>();
            while (!isAtEnd()) {
                statements.add(statement());
            }
            return statements;
        // } catch (ParseError error) {
        //     Lox.error(null, null);
        // }
    }
}
