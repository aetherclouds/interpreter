package lox;

import static lox.TokenType.*;

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

    Token peek() {return tokens.get(current);}

    Token advance() {if (!isAtEnd()) ++current; return previous();}

    Token previous() {return tokens.get(current-1);}

    // private boolean check(TokenType tokenType) {
    //     return tokenType == peek().type;
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
        if (peek().type == RIGHT_BRACE) // only time we'll ever find ourselves here is when dealing w/ empty expressions
            return;
        advance(); // don't match current problematic token in `switch` below
        while (!isAtEnd()) {
            switch (previous().type) {
                case SEMICOLON:
                case RIGHT_BRACE:
                    return;
                default:
            }
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

    Iterable<Stmt> parse() {
        try {
            List<Stmt> statements = new ArrayList<>();
            while (!isAtEnd()) {
                // TODO: feels weird doing this here
                /* inside a block statement, we match `declaration`s. the grammar to detect the end of a block
                 * must then be at or below the level of `declaration`. since we have no global state to track block level,
                 * a `RIGHT_BRACE` match inside `declaration` will always be seen as successful. so we have to match
                 * an error production at at least a level above. */
                if (match(RIGHT_BRACE)) throw error(previous(), "closing unopened brace");
                statements.add(declaration());
            }
            return statements;
        } catch (ParseError error) {
            return null;
        }
    }

    private Iterable<Stmt> block() {
        Token brace = previous();
        List<Stmt> stmts = new ArrayList<>();
        do {
            if (isAtEnd()) throw error(brace, "unclosed brace");
            stmts.add(declaration());
        } while (!match(RIGHT_BRACE));
        return stmts;
    }

    /* say we want to match a literal token: every rule before it will be matched/called.
    * the parts that really make the grammar rulesunique, are optional. 
    * so we can "drill down" until we hit literals. */

    /* we want declarations to have a different precedence from statements because, while they are statements, 
    * we don't want them coming after if or while statements because the scope can be confusing.
    * ex.: if (a) var b = c; 
    * so when `statement()` recursively calls itself after an `if` or `while`, 
    * it can't match a declaration, since it's a "level" above.
    * */
    private Stmt declaration() {
        try {
            if (match(VAR)) {
                // Token name = consume(IDENTIFIER, "\"var\" declaration must be a valid identifier, as in /[a-zA-Z][a-zA-Z0-9]*/");
                /* since we already matched VAR, we already know waht follows MUST be an assignable (Expr.Variable) 
                * so instead of "drilling down" the grammar, go straight for `primary` (which can output a Expr.Variable)  */
                Expr name = primary();
                if (name instanceof Expr.Variable) {
                    Expr initializer = null;
                    if (match(EQUAL)) {
                        initializer = assignment();
                    }
                    consume(SEMICOLON, "expected ';' after declaration");
                    return new Stmt.Var(((Expr.Variable)name).name, initializer);
                }
                throw error(previous(), "invalid variable declaration target following `var`");
            }
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt statement() {
        if (match(LEFT_BRACE)) {
            return new Stmt.Block(block());
        }
        if (match(PRINT)) {
            Expr value = expression();
            consume(SEMICOLON, "expected ';' after print statement");
            return new Stmt.Print(value);
        };
        // fallthrough case
        Expr value = expression();
        consume(SEMICOLON, "expected ';' after statement");
        return new Stmt.Expression(value);
    }
    
    private Expr expression() {
        Expr expr = assignment();
        // if (match(COMMA)) {
        //     expr = new Expr.Binary(expr, previous(), expression());
        // }
        // if (match(QUESTION)) {
        //     Token question = previous();
        //     Expr branch1 = expression();
        //     if (!match(COLON)) error(peek(), "second branch of ternary operator must be defined following a `:`, which is missing");
        //     Token colon = previous();
        //     Expr branch2 = expression();
        //     expr = new Expr.Binary(expr, question, new Expr.Binary(branch1, colon, branch2));
        // } else while(match(COMMA)) {
        //     expr = new Expr.Binary(expr, previous(), equality());
        // }
        return expr;
    }

    private Expr assignment() {
        Expr expr = equality();
        if (match(EQUAL)) {
            Token equal = previous();
            Expr value = assignment();

            // filters out, for ex.: `50 = 49;` `(x) = 5;`
            if (!(expr instanceof Expr.Variable)) {
                throw error(equal, "invalid assignment target");
            }
            return new Expr.Assignment(((Expr.Variable)expr).name, value);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token middle = previous();
            Expr right = comparison();
            // left-to-right precedence
            expr = new Expr.Binary(expr, middle, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token middle = previous();
            Expr right = term();
            // left-to-right precedence
            expr = new Expr.Binary(expr, middle, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(PLUS, MINUS)) {
            Token middle = previous();
            Expr right = factor();
            // left-to-right precedence
            expr = new Expr.Binary(expr, middle, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token middle = previous();
            Expr right = unary();
            // left-to-right precedence
            expr = new Expr.Binary(expr, middle, right);
        }
        return expr;
    }

    private Expr unary() {
        return match(BANG, MINUS)
        ? new Expr.Unary(previous(), unary())
        : primary();
    }

    private Expr primary() {
        if (match(IDENTIFIER)) return new Expr.Variable(previous()); // on assigments, this should be the only thing that's returned from the left-side
        if (match(RIGHT_PAREN)) 
            throw error(previous(), "closing parenthesis don't match an opening parenthesis");
        if (match(
            BANG, MINUS, SLASH, STAR, PLUS, MINUS, 
            GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, BANG_EQUAL, EQUAL_EQUAL
        )) throw error(previous(), "binary operator not preceded by a complete expression");
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) return new Expr.Literal(previous().literal);

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "expected ')' after expression");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "empty expression");
    }
}
