package lox;


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
                if (stmt instanceof Stmt.Expression) 
                System.out.println(evaluate(((Stmt.Expression)stmt).expression));
                execute(stmt);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private Object evaluate(Expr expr) {
        return null == expr ? null : expr.accept(this);
        // return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (null == object) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    // `operator` just for error logging, not actually used in the logic
    private void checkNumberOperand(Token operator, Object operand) {
        if (!(operand instanceof Double))
            throw new RuntimeError(operator, "operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (!(left instanceof Double))
            throw new RuntimeError(operator, "left operand must be a number.");
        if (!(right instanceof Double))
            throw new RuntimeError(operator, "right operand must be a number.");
    }

    private boolean isEqual(Object a, Object b) {
        /* Java's `==` may work for primitives, but for Strings it will compare pointers,
         * which way work when strings are interned but it's still undesirable. also, we
         * need to check for null before calling .equals, which may be overridden by Object,
         * and for Strings, it actually compares the contents.
         */
        if (null == a) return null == b ? true : false; 
        return a.equals(b);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
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
    public Object visitAssignmentExpr(Expr.Assignment expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        switch(expr.operator.type) {
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);

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
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        System.out.println(evaluate(stmt.expression));
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        environment = new Environment(environment);
        for (Stmt statement : stmt.statements) {
            execute(statement);
        }
        environment = environment.enclosing;
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        // NOTE: evaluating `stmt.initializer` in-place means we're always assigning by value, not reference'
        environment.define(stmt.name, null == stmt.initializer ? null /* for declaration */ : evaluate(stmt.initializer));
        return null;
    }
}
