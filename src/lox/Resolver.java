package lox;

import static lox.TokenType.THIS;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import lox.Expr.This;

public class Resolver implements Expr.Visitor<Void>,
        Stmt.Visitor<Void> {
    private final Interpreter interpreter;
    private Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private ClassType currentClass = ClassType.NONE;

    private enum FunctionType {
        NONE,
        FUNCTION,
        METHOD,
        STATICMETHOD,
        INITIALIZER,
    }

    private enum ClassType {
        NONE,
        CLASS,
    }

    Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    void resolve(Iterable<Stmt> stmts) {
        for (Stmt stmt : stmts) {
            resolve(stmt);
        }
    }

    void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr) {
        expr.accept(this);
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void declare(Token name) {
        if (scopes.isEmpty())
            return; // special case for global environment; don't do analysis at all
        var scope = scopes.peek();
        if (scope.containsKey(name.lexeme))
            Lox.error(name, "variable name already declared in this scope");
        scope.put(name.lexeme, false);
    }

    private void define(Token name) {
        if (scopes.isEmpty())
            return; // special case for global environment; don't do analysis at all
        scopes.peek().put(name.lexeme, true);
    }

    @Override
    public Void visitAssignmentExpr(Expr.Assignment expr) {
        // declare(expr.name);
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        for (Expr argument : expr.arguments) {
            resolve(argument);
        }
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalBinaryExpr(Expr.LogicalBinary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitTernaryExpr(Expr.Ternary expr) {
        resolve(expr.condition);
        resolve(expr.thenExpr);
        resolve(expr.elseExpr);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (scopes.isEmpty()) // if in global scope
            return null;
        if (expr.name.type == THIS) {
            if (!(this.currentFunction != FunctionType.METHOD))
                Lox.error(expr.name, "using 'this' keyword outside of a class definition");
            resolveLocal(expr, null);
        }
        if (scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            // program will only reach this if facing code like `var a = a;`
            Lox.error(expr.name, "local variable may not initialize with itself: '" + expr.name.lexeme + "'");
        }
        resolveLocal(expr, expr.name);
        return null;
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; --i) {
            if (!scopes.get(i).containsKey(name.lexeme))
                continue;
            interpreter.resolve(expr, scopes.size() - 1 - i);
        }
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        for (Stmt statement : stmt.statements) {
            resolve(statement);
        }
        endScope();
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        return null;
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunStmt(Stmt.Fun stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    private void resolveFunction(Stmt.Fun fun, FunctionType functionType) {
        beginScope();
        for (var param : fun.params) {
            declare(param);
            define(param);
        }
        var previousFunction = this.currentFunction;
        this.currentFunction = functionType;
        if (fun.body instanceof Stmt.Block) {
            // ugly fix
            // because visitBlockStmt will create a scope and that doesn't align with the
            // interpreter's behavior, we'll just run the block's statements right here
            for (Stmt stmt : ((Stmt.Block) fun.body).statements) {
                resolve(stmt);
            }
        } else
            resolve(fun.body);
        this.currentFunction = previousFunction;
        endScope();
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (null != stmt.elseBranch) {
            resolve(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (this.currentFunction == FunctionType.NONE)
            Lox.error(stmt.keyword, "'return' statement not allowed at top-level");
        if (null != stmt.expr) {
            if (this.currentFunction == FunctionType.INITIALIZER)
                Lox.error(stmt.keyword, "'return' statement not allowed at initializer unless empty");
            resolve(stmt.expr);
        }
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        beginScope();
        resolve(stmt.condition);
        resolve(stmt.body);
        endScope();

        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = this.currentClass;
        this.currentClass = ClassType.CLASS;

        declare(stmt.name);
        define(stmt.name);

        beginScope();
        this.scopes.peek().put("this", true);
        for (Stmt.Fun method : stmt.methods) {
            var functionType = method.isStatic ? FunctionType.STATICMETHOD
                    : method.name.lexeme.equals("init") ? FunctionType.INITIALIZER : FunctionType.METHOD;
            resolveFunction(method, functionType);
        }
        endScope();

        this.currentClass = enclosingClass;

        return null;
    }

    @Override
    public Void visitGetExpr(Expr.Get expr) {
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitSetExpr(Expr.Set expr) {
        resolve(expr.value);
        resolve(expr.object);
        return null;
    }

    @Override
    public Void visitThisExpr(This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "'this' can't be used outside of a class");
            return null;
        }
        if (currentFunction == FunctionType.STATICMETHOD) {
            Lox.error(expr.keyword, "'this' can't be used in a static method");
            return null;
        }
        resolveLocal(expr, expr.keyword);
        return null;
    }
}