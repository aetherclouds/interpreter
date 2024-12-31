/* CODE AUTO-GENERATED WITH GenerateAst.java */
package lox;

abstract class Stmt {
    interface Visitor<R> {
        R visitExpressionStmt(ExpressionStmt stmt);
        R visitPrintStmt(PrintStmt stmt);
    }
    abstract <R> R accept(Visitor<R> visitor);

    static class ExpressionStmt extends Stmt {
        final Expr expression;
        ExpressionStmt(Expr expression) {
            this.expression = expression;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
    }

    static class PrintStmt extends Stmt {
        final Expr expression;
        PrintStmt(Expr expression) {
            this.expression = expression;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitPrintStmt(this);
        }
    }
}
