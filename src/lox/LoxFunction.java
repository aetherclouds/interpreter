package lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    final Stmt.Fun declaration;
    final Environment closure;
    final boolean isInitializer;

    LoxFunction(Stmt.Fun declaration, Environment closure, boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); ++i) {
            // parameter may already be defined since environment is persisted across
            // functoin calls (it's created on function definition)
            environment.define(declaration.params.get(i), arguments.get(i));
        }
        try {
            if (declaration.body instanceof Stmt.Block block) {
                /*
                 * executeBlock is separate from execute (which would call visitBlockStmt)
                 * because visitBlockStmt always creates a new environment, but we already do
                 * that
                 * in LoxFunction.call, so we'd be creating an extra environment every time.
                 */
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
        return new LoxFunction(declaration, environment, this.isInitializer);
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
