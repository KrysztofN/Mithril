package com.kris.mithrilVM;

import com.kris.mithril.Expr;
import com.kris.mithril.Stmt;

import java.util.ArrayList;
import java.util.List;

public class Compiler {

    private final Chunk chunk = new Chunk();

    private boolean hadError = false;

    private final List<String> variableNames = new ArrayList<>();

    public Chunk compile(List<Stmt> statements) {
        for (Stmt stmt : statements) {
            compileStmt(stmt);
            if (hadError) return null;
        }
        emit(OpCode.RETURN, 0);
        chunk.variableCount = variableNames.size();
        return chunk;
    }

    private int slotOf(String name) {
        int idx = variableNames.indexOf(name);
        if (idx != -1) return idx;
        variableNames.add(name);
        return variableNames.size() - 1;
    }

    private void compileStmt(Stmt stmt) {
        switch (stmt) {
            case Stmt.Expression s -> {
                compileExpr(s.expression());
                emit(OpCode.POP, 0);
            }

            case Stmt.Speak s -> {
                compileExpr(s.expression());
                emit(OpCode.SPEAK, 0);
            }

            case Stmt.VarDeclaration s -> {
                compileExpr(s.initializer());
                int slot = variableNames.size();
                variableNames.add(s.name().lexeme());
                chunk.slotMutable.add(s.mutable());
                emit(OpCode.DEFINE_VAR, s.name().line());
                emitByte(slot);
            }

            case Stmt.Assign s -> {
                compileExpr(s.value());
                emit(OpCode.SET_VAR, s.name().line());
                emitByte(slotOf(s.name().lexeme()));
            }

            case Stmt.Block s -> {
                for (Stmt inner : s.statements()) compileStmt(inner);
            }

            case Stmt.Should s -> compileShouldStmt(s);

            case Stmt.Whilst s -> compileWhilstStmt(s);
        }
    }

    private void compileShouldStmt(Stmt.Should s) {
        compileExpr(s.condition());
        int thenJump = emitJump(OpCode.JUMP_IF_FALSE, 0);
        emit(OpCode.POP, 0);

        for (Stmt inner : s.thenBlock().statements()) compileStmt(inner);

        int elseJump = emitJump(OpCode.JUMP, 0);
        chunk.patchJump(thenJump);
        emit(OpCode.POP, 0);

        if (s.elseBlock() != null)
            for (Stmt inner : s.elseBlock().statements()) compileStmt(inner);

        chunk.patchJump(elseJump);
    }

    private void compileWhilstStmt(Stmt.Whilst s) {
        int loopStart = chunk.currentPosition();
        compileExpr(s.condition());
        int exitJump = emitJump(OpCode.JUMP_IF_FALSE, 0);
        emit(OpCode.POP, 0);

        for (Stmt inner : s.body().statements()) compileStmt(inner);

        emitLoop(loopStart);
        chunk.patchJump(exitJump);
        emit(OpCode.POP, 0);
    }

    private void compileExpr(Expr expr) {
        switch (expr) {
            case Expr.Literal l -> {
                if (l.value() == null)
                    emit(OpCode.NAUGHT, 0);
                else if (l.value() instanceof Boolean b)
                    emit(b ? OpCode.TRUE : OpCode.FALSE, 0);
                else {
                    emit(OpCode.CONSTANT, 0);
                    emitByte(chunk.addConstant(l.value()));
                }
            }

            case Expr.Grouping g -> compileExpr(g.inner());

            case Expr.Variable v -> {
                emit(OpCode.GET_VAR, v.name().line());
                emitByte(slotOf(v.name().lexeme()));
            }

            case Expr.Unary u -> {
                compileExpr(u.right());
                switch (u.operator().type()) {
                    case MINUS -> emit(OpCode.NEGATE, u.operator().line());
                    case BANG -> emit(OpCode.NOT, u.operator().line());
                    default -> error("Unknown unary operator.");
                }
            }

            case Expr.Binary b -> {
                compileExpr(b.left());
                compileExpr(b.right());
                int line = b.operator().line();
                switch (b.operator().type()) {
                    case PLUS -> emit(OpCode.ADD, line);
                    case MINUS -> emit(OpCode.SUBTRACT, line);
                    case STAR -> emit(OpCode.MULTIPLY, line);
                    case SLASH -> emit(OpCode.DIVIDE, line);
                    case EQUAL_EQUAL -> emit(OpCode.EQUAL, line);
                    case BANG_EQUAL -> emit(OpCode.NOT_EQUAL, line);
                    case GREATER -> emit(OpCode.GREATER, line);
                    case GREATER_EQUAL -> emit(OpCode.GREATER_EQUAL, line);
                    case LESS -> emit(OpCode.LESS, line);
                    case LESS_EQUAL -> emit(OpCode.LESS_EQUAL, line);
                    default -> error("Unknown binary operator.");
                }
            }
        }
    }

    private void emit(OpCode op, int line) {
        chunk.write(op.ordinal(), line, true);
    }

    private void emitByte(int b) {
        chunk.write(b, 0, false);
    }

    private int emitJump(OpCode op, int line) {
        emit(op, line);
        emitByte(0xff);
        emitByte(0xff);
        return chunk.currentPosition();
    }

    private void emitLoop(int loopStart) {
        emit(OpCode.LOOP, 0);
        int offset = chunk.currentPosition() - loopStart + 2;
        emitByte((offset >> 8) & 0xff);
        emitByte(offset & 0xff);
    }

    private void error(String msg) {
        System.err.println("[Compiler error] " + msg); hadError = true;
    }
}