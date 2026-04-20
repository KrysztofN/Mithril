package com.kris.mithril;

abstract public class Expression {
    static class Binary extends Expression {
        final Expression left;
        final Token operator;
        final Expression right;

        Binary(Expression left, Token operator, Expression right){
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
    }
}
