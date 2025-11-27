package lox;

import static lox.TokenType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    // use RuntimeExceptions when the caller is doing something wrong.
    private static class ParseError extends RuntimeException {
    }

    // not an Iterable because we need an index accessor method
    private final List<Token> tokens;
    private int current = 0; // current hasn't been accessed yet

    Parser(final List<Token> tokens) {
        this.tokens = tokens;
    }

    private boolean isAtEnd() {
        return check(EOF);
    }

    Token peek() {
        return tokens.get(current);
    }

    Token advance() {
        // if (!isAtEnd())
        // ++current;
        // return previous();
        Token ret = peek();
        if (!isAtEnd())
            ++current;
        return ret;
    }

    Token previous() {
        return tokens.get(current - 1);
    }

    // private boolean check(TokenType tokenType) {
    // return tokenType == peek().type;
    // }

    private boolean match(TokenType... tokenTypes) {
        Token match = peek();
        if (null == match)
            return false;
        for (TokenType tokenType : tokenTypes) {
            if (check(tokenType)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        return type == peek().type;
    }

    private Token consume(TokenType type, String message) {
        if (check(type))
            return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        // if (check(RIGHT_BRACE)) // only time we'll ever find ourselves here is when
        // dealing w/ empty expressions
        // return;
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
                /*
                 * inside a block statement, we match `declaration`s. the grammar to detect the
                 * end of a block
                 * must then be at or below the level of `declaration`. since we have no global
                 * state to track block level,
                 * a `RIGHT_BRACE` match inside `declaration` will always be seen as successful.
                 * so we have to match
                 * an error production at at least a level above.
                 */
                // if (match(RIGHT_BRACE))
                // throw error(previous(), "closing unopened brace");
                statements.add(declaration());
            }
            return statements;
        } catch (ParseError error) {
            return null;
        }
    }

    /*
     * say we want to match a literal token: every rule before it will be
     * matched/called.
     * the parts that really make the grammar rulesunique, are optional.
     * so we can "drill down" until we hit literals.
     */

    /*
     * we want declarations to have a different precedence from statements because,
     * while they are statements,
     * we don't want them coming after if or while statements because the scope can
     * be confusing.
     * ex.: if (a) var b = c;
     * so when `statement()` recursively calls itself after an `if` or `while`,
     * it can't match a declaration, since it's a "level" above.
     */
    private Stmt declaration() {
        try {
            if (match(VAR)) {
                return varDeclaration();
            }
            if (match(FUN)) {
                return function(FunctionKind.FUNCTION);
            }
            if (match(CLASS)) {
                return classDeclaration();
            }
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt.Class classDeclaration() {
        Token name = consume(IDENTIFIER, "expected identifier in class declaration");
        consume(LEFT_BRACE, "expected '{' in class declaration");
        List<Stmt.Fun> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            if (match(CLASS)) {
                methods.add(function(FunctionKind.STATIC));
            } else {
                methods.add(function(FunctionKind.METHOD));
            }
        }
        consume(RIGHT_BRACE, "expected '}' at end of class declaration");
        return new Stmt.Class(name, methods);
    }

    private Stmt.Var varDeclaration() {
        /*
         * since we already matched VAR, we already know what follows MUST be an
         * assignable (Expr.Variable)
         * so instead of "drilling down" the grammar, go straight for `primary` (which
         * can return a Expr.Variable)
         */
        Token name = consume(IDENTIFIER, "invalid variable declaration target following `var`");
        // if (!(name instanceof Expr.Variable)) {
        // throw error(previous(), "invalid variable declaration target following
        // `var`");
        // }
        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = assignment();
        }
        consume(SEMICOLON, "expected ';' instead, after declaration");
        return new Stmt.Var(name, initializer);
    }

    private enum FunctionKind {
        STATIC("static method"), METHOD("method"), FUNCTION("function");

        private final String repr;

        private FunctionKind(String kind) {
            this.repr = kind;
        }
    }

    private Stmt.Fun function(FunctionKind kind) {
        Token name = consume(IDENTIFIER, "invalid " + kind.repr + " declaration target following `fun`");
        List<Token> params = new ArrayList<>();
        boolean isGetter = false;
        if (match(LEFT_PAREN)) {
            if (!match(RIGHT_PAREN)) {
                for (;;) {
                    params.add(consume(IDENTIFIER, "expected parameter on " + kind.repr + " signature"));
                    if (params.size() > 254)
                        throw error(peek(), "exceeded max. " + kind.repr + " parameter count (255)");
                    if (match(RIGHT_PAREN))
                        break;
                    consume(COMMA, "expected ',' after " + kind.repr + " parameter, or ')'");
                }
            }
        } else {
            isGetter = true;
        }
        Stmt body = statement();
        return new Stmt.Fun(name, params, body, kind == FunctionKind.STATIC, isGetter);
    }

    private Stmt statement() {
        if (match(LEFT_BRACE)) {
            return new Stmt.Block(block());
        }
        // TODO: check if inside loop?
        if (match(CONTINUE)) {
            Token stmt = previous();
            consume(SEMICOLON, "expected ';' instead, after continue statement");
            return new Stmt.Continue(stmt);
        }
        if (match(BREAK)) {
            Token stmt = previous();
            consume(SEMICOLON, "expected ';' instead, after break statement");
            return new Stmt.Break(stmt);
        }
        if (match(PRINT)) {
            Expr value = expression();
            consume(SEMICOLON, "expected ';' instead, after print statement");
            return new Stmt.Print(value);
        }
        if (match(IF)) {
            consume(LEFT_PAREN, "expected '(' instead, after if statement");
            Expr condition = expression();
            consume(RIGHT_PAREN, "expected ')' instead, after expression of if statement");
            // declaration not allowed
            Stmt thenBranch = statement();
            Stmt elseBranch = null;
            if (match(ELSE)) {
                elseBranch = statement();
            }
            return new Stmt.If(condition, thenBranch, elseBranch);
        }
        if (match(WHILE)) {
            consume(LEFT_PAREN, "expected '(' instead, after while statement");
            Expr condition = expression();
            consume(RIGHT_PAREN, "expected ')' instead, after expression of while statement");
            Stmt body = statement();
            return new Stmt.While(condition, body);
        }
        if (match(FOR)) {
            consume(LEFT_PAREN, "expected '(' instead, after for statement");

            Stmt initializer = null;
            if (!match(SEMICOLON)) {
                if (match(VAR))
                    initializer = varDeclaration();
                // could also make `initializer` an `Expr` and match a definition expression.
                // not sure why one would want to write anything else here
                else
                    initializer = expressionStatement();
                // expressionStatement consumes SEMICOLON
            }

            Expr condition = null;
            if (!match(SEMICOLON)) {
                condition = expression();
                consume(SEMICOLON, "expected ';' after `for` statement condition");
            }

            Expr increment = null;
            if (!match(RIGHT_PAREN)) {
                increment = expression();
                consume(RIGHT_PAREN, "expected ')' instead, after expression of for statement");
            }

            // TODO: optional body
            // Stmt body = null;
            // if (!match(SEMICOLON)) body = statement();
            Stmt body = statement();

            // "desugarize" `for` statement - build an AST using the `while` node
            if (null == condition)
                condition = new Expr.Literal(true);
            if (null != increment)
                body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
            Stmt stmt = new Stmt.While(condition, body);
            if (null != initializer)
                stmt = new Stmt.Block(Arrays.asList(initializer, stmt));
            return stmt;
        }
        if (match(RETURN)) {
            Token keyword = previous();
            Expr value = null;
            if (!check(SEMICOLON)) {
                value = expression();
            }
            consume(SEMICOLON, "expected ';' instead, after print statement");
            return new Stmt.Return(keyword, value);
        }

        // fallthrough case
        return expressionStatement();
    }

    private Iterable<Stmt> block() {
        Token brace = previous();
        List<Stmt> stmts = new ArrayList<>();
        while (!match(RIGHT_BRACE)) {
            if (isAtEnd())
                throw error(brace, "unclosed brace");
            stmts.add(declaration());
        }
        return stmts;
    }

    private Stmt.Expression expressionStatement() {
        Expr value = expression();
        consume(SEMICOLON, "expected ';' instead, after statement");
        return new Stmt.Expression(value);
    }

    private Expr expression() {
        if (match(VAR))
            throw error(previous(), "declaration not allowed here");
        // if (match(RIGHT_PAREN)) throw error(previous(), "empty expression");

        Expr expr = assignment();

        if (match(QUESTION)) {
            Expr thenBranch = expression();
            consume(COLON, "expected ':' instead, after first branch of ternary operator");
            Expr elseBranch = expression();
            expr = new Expr.Ternary(expr, thenBranch, elseBranch);
        }
        while (match(AND) || match(OR)) {
            Token operator = previous();
            Expr right = assignment();
            expr = new Expr.LogicalBinary(expr, operator, right);
        }

        return expr;
    }

    private Expr assignment() {
        Expr expr = equality();
        if (match(EQUAL)) {
            Token equal = previous();
            // filters out, for ex.: `50 = 49;` `(x) = 5;`
            // doesn't filter out `a == b = c;` for ex. because that is allowed; assignment
            // is an expression
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                return new Expr.Assignment(((Expr.Variable) expr).name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get) expr;
                return new Expr.Set(get.object, get.name, value);
            }

            throw error(equal, "invalid assignment target");
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
                : call();
    }

    private Expr call() {
        Expr expr = primary();
        while (true) {
            if (match(LEFT_PAREN)) {
                // if (!(expr instanceof Expr.Variable || expr instanceof Expr.Call))
                // throw error(previous(), "expression is not callable");
                Token paren = null;
                List<Expr> args = new ArrayList<>();
                if (!match(RIGHT_PAREN)) {
                    for (;;) {
                        args.add(expression());
                        if (args.size() > 254)
                            throw error(peek(), "exceeded max. call argument count (255)");
                        if (match(RIGHT_PAREN))
                            break;
                        consume(COMMA, "call arguments must be separated by a ','");
                    }
                }
                paren = previous();
                expr = new Expr.Call(expr, args, paren/* thesis */);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "expected property name after '.'");
                expr = new Expr.Get(expr, name);
            } else
                break;
        }
        return expr;
    }

    private Expr primary() {
        if (match(IDENTIFIER))
            return new Expr.Variable(previous());
        if (match(THIS))
            return new Expr.This(previous());
        if (match(
                BANG, MINUS, SLASH, STAR, PLUS, MINUS,
                GREATER, GREATER_EQUAL, LESS, LESS_EQUAL, BANG_EQUAL, EQUAL_EQUAL))
            throw error(previous(), "operator not allowed here");
        if (match(FALSE))
            return new Expr.Literal(false);
        if (match(TRUE))
            return new Expr.Literal(true);
        if (match(NIL))
            return new Expr.Literal(null);

        if (match(NUMBER, STRING))
            return new Expr.Literal(previous().literal);

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "expected ')' instead, after expression");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "expected an expression instead");
    }
}