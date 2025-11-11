package lox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private final LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();
    // private final Map<String, LoxFunction> methods = new HashMap<>();

    LoxInstance(LoxClass klass) {
        this.klass = klass;
        // this.methods 
    }

    @Override
    public String toString() {
        return this.klass.name;
    }

    public Object get(Token name) {
        if (fields.containsKey(name.lexeme))
            return fields.get(name.lexeme);
        if (this.klass.methods.containsKey(name.lexeme)) {
            LoxFunction method = this.klass.methods.get(name.lexeme);
            return method.bind(this);
        }
        throw new lox.Interpreter.RuntimeError(name, "undefined field '" + name.lexeme + "'");
    }

    public Object set(Token name, Object value) {
        return fields.put(name.lexeme, value);
    }
}
