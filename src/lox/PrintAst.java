package lox;

import lox.Expr.*;

class PrintAst implements Visitor<String> {
    @Override
    public String visitBinaryExpr(BinaryExpr expr) {
        return parenthesize(
            expr.operator.lexeme,
            expr.left,
            expr.right
        );
    }

    @Override
    public String visitGroupingExpr(GroupingExpr expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(LiteralExpr expr) {
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(UnaryExpr expr) {
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
        Expr expression = new BinaryExpr(
            new UnaryExpr(
                new Token(TokenType.MINUS, "-", null, 0),
                new LiteralExpr(125)
            ),
            new Token(TokenType.STAR, "*", null, 0),
            new GroupingExpr(
                new LiteralExpr(42.6)
            )
        );
        System.out.println(new PrintAst().output(expression));
    }

    @Override
    public String visitVariableExpr(VariableExpr expr) {
        return expr.name.lexeme;
    }
}