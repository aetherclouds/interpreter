/* CODE AUTO-GENERATED WITH GenerateAst.java */
package lox;

abstract class Expr {
    interface Visitor<R> {
        R visitBinaryExpr(BinaryExpr expr);
        R visitGroupingExpr(GroupingExpr expr);
        R visitLiteralExpr(LiteralExpr expr);
        R visitUnaryExpr(UnaryExpr expr);
    }
    abstract <R> R accept(Visitor<R> visitor);

    static class BinaryExpr extends Expr {
        final Expr left;
        final Token operator;
        final Expr right;
        BinaryExpr(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }

    static class GroupingExpr extends Expr {
        final Expr expression;
        GroupingExpr(Expr expression) {
            this.expression = expression;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }
    }

    static class LiteralExpr extends Expr {
        final Object value;
        LiteralExpr(Object value) {
            this.value = value;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }

    static class UnaryExpr extends Expr {
        final Token operator;
        final Expr right;
        UnaryExpr(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }
}
