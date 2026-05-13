package com.kris.mithril;

import java.util.List;

public sealed interface Stmt permits Stmt.Expression, Stmt.Speak, Stmt.VarDeclaration, Stmt.Assign, Stmt.Block, Stmt.Should, Stmt.Whilst {

    record Expression(Expr expression) implements Stmt {}

    record Speak(Expr expression) implements Stmt {}

    record VarDeclaration(Token name, Expr initializer, boolean mutable) implements Stmt {}

    record Assign(Token name, Expr value) implements Stmt {}

    record Block(List<Stmt> statements) implements Stmt {}

    record Should(Expr condition, Stmt.Block thenBlock, Stmt.Block elseBlock) implements Stmt {}

    record Whilst(Expr condition, Stmt.Block body) implements Stmt {}
}
