package lox;

import java.util.HashMap;
import java.util.Map;

import lox.Interpreter.RuntimeError;

public class Environment {
    Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment() {
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(Token name, Object value) {
        values.put(name.lexeme, value);
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    void assign(Token name, Object value) {
        if (!values.containsKey(name.lexeme)) {
            if (null != enclosing) {
                enclosing.assign(name, value);
                return;
            }
            throw new RuntimeError(name, "identifier doesn't exist");
        }
        values.put(name.lexeme, value);
    }

    void assign(String name, Object value) {
        if (!values.containsKey(name)) {
            if (null != enclosing) {
                enclosing.assign(name, value);
                return;
            }
            throw new RuntimeException("identifier 'this' doesn't exist");
        }
        values.put(name, value);
    }

    Object get(Token name) {
        if (!values.containsKey(name.lexeme)) {
            // if (null != enclosing) return enclosing.get(name);
            throw new RuntimeError(name, "identifier doesn't exist");
        }
        return values.get(name.lexeme);
    }

    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    Environment ancestor(int depth) {
        Environment ret = this;
        for (int i = 0; i < depth; ++i) {
            ret = ret.enclosing;
        }
        return ret;
    }
}
