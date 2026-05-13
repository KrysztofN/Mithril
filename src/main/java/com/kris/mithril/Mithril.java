package com.kris.mithril;

import com.kris.mithrilAST.Interpreter;
import com.kris.mithrilVM.Chunk;
import com.kris.mithrilVM.Compiler;
import com.kris.mithrilVM.OpCode;
import com.kris.mithrilVM.VM;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Mithril {
    static boolean hadError = false;
    public static boolean hadRuntimeError = false;

    private static final Interpreter interpreter = new Interpreter();

    static void main(String[] args) throws IOException {
        if(args.length > 1){
            System.out.println("Usage: mithril [script]");
            System.exit(64);
        } else if (args.length == 1){
            runFile(args[0]);
        } else {
            runPrompt();
        }

    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));
        if(hadError) System.exit(65);
        if(hadRuntimeError) System.exit(70);
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;){
            System.out.print("-> ");
            String line = reader.readLine();
            if(line == null) break;
            run(line);
            hadError = false;
            hadRuntimeError = false;
        }
    }

    private static void run(String source){
        System.out.print("SOURCE CODE: " + source);
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();

//        Printing every token
//        int curr_line = 1;
//        for (Token token : tokens){
//            if (token.line() > curr_line){
//                curr_line = token.line();
//                System.out.println();
//            }
//            System.out.print(token + " | ");
//        }

//        Printing Token list
        List<TokenType> tokenTypeList = tokens.stream().map(Token::type).toList();
        System.out.println("SCANNER OUTPUT: " + tokenTypeList);

        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parseProgram();

        if (hadError) return;

//      [AST]
//        interpreter.interpret(statements);


//      [VM]
        Chunk chunk = new Compiler().compile(statements);
        System.out.println("Constants: " + chunk.constants);
        System.out.println("Code: " + chunk.disassembleList);
        System.out.println("Variable count: " + chunk.variableCount);
        new VM(chunk).run();
    }

    static void error(int line, String message){
        report(line, "", message);
    }

    private static void report(int line, String where, String message){
        System.err.println("[line "+ line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(Token token, String message){
        if(token.type() == TokenType.EOF){
            report(token.line(), " at end", message);
        } else {
            report(token.line(), " at '" + token.lexeme() + "'", message);
        }
    }
}
