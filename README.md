interpreter in Java as part of the book [Crafting Interpreters](http://www.craftinginterpreters.com)
## building
run `make bin/lox/Lox.class`
### .jar
run `make bin/lox.jar`
## usage
### running the REPL
run `make run` (or `java -jar bin/lox.jar`)
### executing a file
run `make run ARGS=<FILENAME>`
### adding a new AST definition
1. add new definition under `Main`, in [`src/lox/GenerateAst.java`](src/lox/GenerateAst.java)
2. run `make run-GenerateAst`
3. fill in new visitor methods for `Interpreter` in [`src/lox/Interpreter.java`](src/lox/Interpreter.java)
## TODO:
- parser: add newline token for better error handling, unwinding and synchronizing
- interpreter: cast boolean to double for things like `(a == b) * 4`
- fully implement `cond ? branch1 : branch2` expressions
## ideas
- throw some kind of EndOfExpression exception during parsing so we can skip the upward chain of redundant checks when we know the next token (EOF) won't match anything
- parser with global "level" state, or match error production for extra closing bracket (ex.: `{}}`)