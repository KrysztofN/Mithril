package com.kris.mithril;

public sealed interface Expr permits Expr.Binary, Expr.Unary, Expr.Grouping, Expr.Literal {

    record Binary(Expr left, Token operator, Expr right) implements Expr {}

    record Unary(Token operator, Expr right) implements Expr {}

    record Grouping(Expr inner) implements Expr {}

    record Literal(Object value) implements Expr {}
}