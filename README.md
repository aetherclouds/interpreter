interpreter in Java as part of the book [Crafting Interpreters](http://www.craftinginterpreters.com)
## TODO:
- parser: add newline token for better error handling, unwinding and synchronizing
- interpreter: cast boolean to double for things like `(a == b) * 4`
- fully implement `cond ? branch1 : branch2` expressions
## ideas
- throw some kind of EndOfExpression exception during parsing so we can skip the upward chain of redundant checks when we know the next token (EOF) won't match anything