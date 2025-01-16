package lox;

import lox.Expr.*;

class PrintAst implements Visitor<String> {
    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(
            expr.operator.lexeme,
            expr.left,
            expr.right
        );
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    String output(Expr expr) {
        return expr.accept(this);
    }

    String parenthesize(String first, Expr... args) {
        StringBuilder builder = new StringBuilder("(").append(first);
        for (Expr arg : args) {
            builder.append(" ");
            builder.append(arg.accept(this));
        }
        builder.append(")");
        return builder.toString();
    }

    public static void main(String[] args) {
        Expr expression = new Expr.Binary(
            new Expr.Unary(
                new Token(TokenType.MINUS, "-", null, 0),
                new Expr.Literal(125)
            ),
            new Token(TokenType.STAR, "*", null, 0),
            new Expr.Grouping(
                new Expr.Literal(42.6)
            )
        );
        System.out.println(new PrintAst().output(expression));
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }
}