## TODO:
- add newline token for bether error handling, unwinding and synchronizing
- interpreter: cast boolean to double for things like `(a == b) * 4`
## ideas
- throw some kind of EndOfExpression exception during parsing so we can skip the upward chain of redundant checks when we know the next token (EOF) won't match anything