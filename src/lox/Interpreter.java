package lox;

import lox.TokenType.*;
import lox.Expr.*;
import lox.Stmt.*;


public class Interpreter implements Expr.Visitor<Object>,
                                    Stmt.Visitor<Void> {
    
    Environment environment = new Environment();
    static class RuntimeError extends RuntimeException {
        Token token;
        RuntimeError(Token token, String message) {
            super(message);
            this.token = token;
        }   
    };

    public void interpret(Iterable<Stmt> statements) {   
        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    public Object evaluate(Expr expr) {
        return null == expr ? null : expr.accept(this);
        // return expr.accept(this);
    }

    public void execute(Stmt stmt) {
        stmt.accept(this);
    }

    boolean isTruthy(Object object) {
        if (null == object) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    // `operator` just for error logging, not actually used in the logic
    void checkNumberOperand(Token operator, Object operand) {
        if (!(operand instanceof Double))
            throw new RuntimeError(operator, "operand must be a number.");
    }

    void checkNumberOperands(Token operator, Object left, Object right) {
        if (!(left instanceof Double))
            throw new RuntimeError(operator, "left operand must be a number.");
        if (!(right instanceof Double))
            throw new RuntimeError(operator, "right operand must be a number.");
    }

    public boolean isEqual(Object a, Object b) {
        /* Java's `==` may work for primitives, but for Strings it will compare pointers,
         * which way work when strings are interned but it's still undesirable. also, we
         * need to check for null before calling .equals, which may be overridden by Object,
         * and for Strings, it actually compares the contents.
         */
        if (null == a) return null == b ? true : false; 
        return a.equals(b);
    }

    @Override
    public Object visitGroupingExpr(GroupingExpr expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitVariableExpr(VariableExpr expr) {
        return environment.get(expr.name.lexeme);
    }

    @Override
    public Object visitLiteralExpr(LiteralExpr expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr expr) {
        Object right = evaluate(expr.right);
        switch(expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                // let java do the work of casting our dynamic object
                checkNumberOperand(expr.operator, right); 
                return -(double)right;
        }
        return null; // TODO: remove this
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch(expr.operator.type) {
            // case QUESTION:
            //     Object test = evaluate(expr.left);
                // we'll n
                // evaluate(isTruthy(test) ? expr.right

            case BANG_EQUAL:
                return !left.equals(right);
            case EQUAL_EQUAL:
                return left.equals(right);

            case GREATER:
                checkNumberOperands(expr.operator, left, right); 
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right); 
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right); 
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right); 
                return (double)left <= (double)right;
            
            case SLASH:
                checkNumberOperands(expr.operator, left, right); 
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right); 
                return (double)left * (double)right;


            case MINUS:
                checkNumberOperands(expr.operator, left, right); 
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof String && right instanceof String)
                    return (String)left + (String)right;
                checkNumberOperands(expr.operator, left, right); 
                return (double)left + (double)right;            
        }
        return null; // TODO: remove this
    }

    @Override
    public Void visitExpressionStmt(ExpressionStmt stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(PrintStmt stmt) {
        System.out.println(evaluate(stmt.expression));
        return null;
    }

    @Override
    public Void visitVarStmt(VarStmt stmt) {
        environment.define(stmt.name.lexeme, evaluate(stmt.initializer));
        return null;
    }
}
