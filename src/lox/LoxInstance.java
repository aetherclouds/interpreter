package lox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private final LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();
    private final Map<String, LoxFunction> staticMethods;

    // private final Map<String, LoxFunction> methods = new HashMap<>();
    LoxInstance(LoxClass klass) {
        this.klass = klass;
        this.staticMethods = null;
        // this.methods
    }

    // base class
    LoxInstance(Map<String, LoxFunction> staticMethods) {
        this.klass = null;
        this.staticMethods = staticMethods;
    }

    @Override
    public String toString() {
        return this.klass.name;
    }

    public Object get(Token name) {
        if (fields.containsKey(name.lexeme))
            return fields.get(name.lexeme);
        else if (this.klass.methods.containsKey(name.lexeme)) {
            LoxFunction method = this.klass.methods.get(name.lexeme);
            if (!(this.klass instanceof LoxBaseClass))
                method = method.bind(this);
            return method;
        } else if (null == this.klass) {
            if (!staticMethods.containsKey(name.lexeme))
                throw new lox.Interpreter.RuntimeError(name, "undefined static method '" + name.lexeme + "'");
            // this is a static class instance
            return staticMethods.get(name.lexeme);
        }
        throw new lox.Interpreter.RuntimeError(name, "undefined field or method '" + name.lexeme + "'");
    }

    public Object set(Token name, Object value) {
        return fields.put(name.lexeme, value);
    }
}
