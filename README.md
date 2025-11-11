interpreter in Java as part of the book [Crafting Interpreters](http://www.craftinginterpreters.com)
## building
run `make bin/lox/Lox.class` (without Make: `javac src/lox/Lox.java -d bin -sourcepath src`)
### .jar
run `make bin/lox.jar`
## usage
### running the REPL
run `make run` (without Make: `java -cp bin lox.Lox`)
### executing a file
run `make run ARGS=<FILENAME>`
### adding a new AST definition
the AST is generated from a string definition and must be regenerated whenever this string is updated.
1. add new definition under `Main`, in [`src/lox/GenerateAst.java`](src/lox/GenerateAst.java)
2. run `make run-GenerateAst`
3. fill in new visitor methods for `Interpreter` in [`src/lox/Interpreter.java`](src/lox/Interpreter.java)
## TODO:
- [ ] increment (PLUSPLUS) unary operator
- [ ] parser: add newline token for better error handling, unwinding and synchronizing
- [x] interpreter: cast double to boolean for things like `1 ?  a : b;`
- [ ] interpreter: cast boolean to double for things like `(a == b) * 4`
- [ ] fully implement ternary expressions (`cond ? branch1 : branch2`)
- [x] parser with global "level" state, or match error production for extra closing bracket (ex.: `{}}`)\
- [ ] different synchronization behavior for ';' character when inside for-loop initializer
## ideas
- [ ] throw some kind of EndOfExpression exception during parsing so we can skip the upward chain of redundant checks when we know the next token (EOF) won't match anything
- [ ] allow identifiers to start with a number - current valid identifier is `/[a-zA-Z][a-zA-Z0-9]*/` (cursed)
- [ ] "escape" to global scope:
    ```
    // start of file
    var x = 123;
    }
    /* x is not accessible here since
    it's not part of global scope */
    fun newGlobalMethod() {...}
    {
    // end of file
    ```
    had this idea because initial environment could inherit from global environment, creating 2 layers of scope from the start, so it would be cool to "climb up" since that is what code blocks do