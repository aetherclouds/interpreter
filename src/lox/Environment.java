package lox;

import java.util.HashMap;
import java.util.Map;

import lox.Interpreter.RuntimeError;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();

    void define(Token name, Object value) {values.put(name.lexeme, value);}
    Object get(Token name) {
        if (!values.containsKey(name.lexeme)) throw new RuntimeError(name, "variable doesn't exist: "+name.lexeme);
        return values.get(name.lexeme);
    }
}
