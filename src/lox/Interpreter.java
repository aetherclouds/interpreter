package lox;

import lox.Expr.LogicalBinary;
import lox.Stmt.Break;
import lox.Stmt.Continue;
import lox.Stmt.While;

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

    static class ContinueException extends RuntimeException {
        Token token;
        ContinueException(Token token) {this.token = token;}
    };

    static class BreakException extends RuntimeException {
        Token token;
        BreakException(Token token) {this.token = token;}
    };

    public void interpret(Iterable<Stmt> statements) {   
        try {
            for (Stmt stmt : statements) {
                if (
                    stmt instanceof Stmt.Expression
                    // avoid printing `x = y`;
                    && !(((Stmt.Expression)stmt).expression instanceof Expr.Assignment)
                ) System.out.println(evaluate(((Stmt.Expression)stmt).expression));
                else execute(stmt);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        /* not caught by a `while` or `for` loop, which means user is using these statements at global scope.
        * (rather than inside a loop)  */
        } catch (ContinueException e) {
            Lox.runtimeError(new RuntimeError(e.token, "statement may only be used inside a loop"));
        } catch (BreakException e) {
            Lox.runtimeError(new RuntimeError(e.token, "statement may only be used inside a loop"));
        }
    }

    public Object evaluate(Expr expr) {
        return null == expr ? null : expr.accept(this);
        // return expr.accept(this);
    }

    public void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private boolean isTruthy(Object object) {
        if (null == object) return false;
        if (object instanceof Boolean) return (boolean)object;
        if (object instanceof Double) return (Double)object == 0. ? false : true;
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
            default:
                throw new RuntimeError(expr.operator, "undefined operator behavior for unary expression");
        }
    }

    @Override
    public Object visitAssignmentExpr(Expr.Assignment expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitLogicalBinaryExpr(LogicalBinary expr) {
        Object left = evaluate(expr.left);
        switch(expr.operator.type) {
            case AND:
                if (isTruthy(left)) {
                    Object right = evaluate(expr.right);
                    if (isTruthy(right)) return right;
                }
                return left;
            case OR:
                if (isTruthy(left)) return left;
                // let's not even care about determining whether `right` is truthy - we'll return it either way
                return evaluate(expr.right);
            default:
                throw new RuntimeError(expr.operator, "undefined operator behavior for logical binary expression");
        }
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

            default:
                throw new RuntimeError(expr.operator, "undefined operator behavior for binary expression");
        }
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        return (isTruthy(evaluate(expr.condition))) 
        ? evaluate(expr.thenExpr)
        : evaluate(expr.elseExpr);
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

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) 
            execute(stmt.thenBranch);
        else if (null != stmt.elseBranch) execute(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitWhileStmt(While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            // this is probably bad for performance, but so is making an interpreter in Java
            try {
                execute(stmt.body);
            } catch (ContinueException e) {
                continue;
            } catch (BreakException e) {
                break;
            }
        }
        return null;
    }

    @Override
    public Void visitContinueStmt(Continue stmt) {
        throw new ContinueException(stmt.token);
    }

    @Override
    public Void visitBreakStmt(Break stmt) {
        throw new BreakException(stmt.token);
    }
}
