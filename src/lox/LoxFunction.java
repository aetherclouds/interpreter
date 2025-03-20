package lox;

import java.util.List;

public class LoxFunction implements LoxCallable {
    Stmt.Fun declaration;
    Environment closure;

    LoxFunction(Stmt.Fun declaration, Environment closure) {
        this.closure = closure;
        this.declaration = declaration;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        for (int i = 0; i < declaration.params.size(); ++i) {
            // parameter may already be defined since environment is presisted across
            // functoin calls (it's created on function definition)
            closure.define(declaration.params.get(i), arguments.get(i));
        }
        try {
            if (declaration.body instanceof Stmt.Block block) {
                /*
                 * executeBlock is separate from execute (which would call visitBlockStmt)
                 * because visitBlockStmt always creates a new environment, but we already do
                 * that
                 * in LoxFunction.call, so we'd be creating an extra environment every time.
                 */
                interpreter.executeBlock(block, closure);
            } else {
                interpreter.execute(declaration.body);
            }
        } catch (Interpreter.ReturnException e) {
            return e.obj;
        }
        return null;
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
