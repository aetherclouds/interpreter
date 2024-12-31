package lox;

import java.util.List;

import lox.Interpreter.RuntimeError;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

class Lox {
    static private Path file;
    /* interpreter needs to maintain state, so we can 
    * retain variables and other state across multiple, separate evaulations.
    * (i.e. REPL lines, modules) */
    static private final Interpreter interpreter = new Interpreter();
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    
    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.err.println("usage: java lox <filename>");
            System.exit(-1);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else runPrompt();
    }

    private static void runFile(String filename) throws IOException {
        file = Path.of(filename);
        String bytes = Files.readString(file,  Charset.defaultCharset());
        run(bytes);
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        // inputstream vs outputstream: one exclusively has read methods, the other, write.
        BufferedReader input = new BufferedReader(new InputStreamReader(System. in));
        /* istreamreader vs bufferedreader: stream thinks in characters, i.e. immediate future.
         * that's why it doesn't have a readLine method, whereas bufferedreader does, since it'll
         * probably also reaed every single incoming character, but it has a buffer to store
         * non-newline characters in until then
         */
        for (;;) {
            // return value excludes \r and \n as per java docs
            System.out.print("> ");
            String line = input.readLine();
            if (line == null) break; // EOF ^D
            run(line);
            hadError = false;
        }
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        if (hadError) return;

        Parser parser = new Parser(tokens);
        Iterable<Stmt> statements = parser.parse();
        // if (null == statements) {
        //     System.err.println("could not parse file or REPL input");
        //     return;
        // }
        if (hadError) return;
        
        // System.out.println(new PrintAst().output(expression));
        interpreter.interpret(statements);
        if (hadRuntimeError) return;
    }

    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + " (line "+error.token.line+")");
        hadRuntimeError = true;
    }

    static void error(int line, String message) {
        report(line, "", message);
    }
    
    static void error(Token token, String message) {
        if (TokenType.EOF == token.type) {
            report(token.line, "at end", message);
        } else report(token.line, "at '"+token.lexeme+"'", message);
    }
    
    private static void report(int line, String where, String message) {
        System.err.println(
            (file != null ? file.getFileName().toString() : "<prompt>")  + ":" 
            + line + " error " + where + ": " 
            + message);
        hadError = true;
    }
}