package lox;

import java.util.Map;

public class LoxBaseClass extends LoxClass {
    LoxBaseClass(Map<String, LoxFunction> methods) {
        super("Base", methods, null);
    }
}
