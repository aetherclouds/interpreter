package lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();

    void define(String name, Object value) {values.put(name, value);}
    Object get(String name) {return values.get(name);}
}
