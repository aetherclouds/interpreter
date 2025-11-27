package lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    final Stmt.Fun declaration;
    final Environment closure;
    final boolean isInitializer;
    final boolean isStatic;
    final boolean isGetter;

    LoxFunction(Stmt.Fun declaration, Environment closure, boolean isInitializer, boolean isStatic, boolean isGetter) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
        this.isStatic = isStatic;
        this.isGetter = isGetter;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // create new scope. in the interpreter, `visitBlockStmt` does this and wraps
        // `executeBlock`, but because we will call `executeBlock` directly, we must do
        // this manually
        // this lines up with the resolver's scoping
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); ++i) {
            environment.define(declaration.params.get(i), arguments.get(i));
        }
        try {
            if (declaration.body instanceof Stmt.Block block) {
                interpreter.executeBlock(block, environment);
            } else {
                interpreter.execute(declaration.body);
            }
        } catch (Interpreter.ReturnException e) {
            if (isInitializer)
                return closure.getAt(0, "this");
            return e.obj;
        }
        if (isInitializer)
            return closure.getAt(0, "this");
        return null;
    }

    LoxFunction bind(Object parent) {
        Environment environment = new Environment(closure);
        environment.define("this", parent);
        return new LoxFunction(declaration, environment, this.isInitializer, this.isStatic, this.isGetter);
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
