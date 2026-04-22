package com.kris.mithril;

public class Interpreter {

    public boolean interpret(Expr expr){
        MithrilResult<Object> result = evaluate(expr);
        switch (result){
            case MithrilResult.Ok<Object> ok -> {
                System.out.println(stringify(ok.value()));
                return true;
            }
            case MithrilResult.Err<Object> err -> {
                System.err.println("[line " + err.line() + "] Runtime error: " + err.message());
                return false;
            }
        }
    }

    private MithrilResult<Object> evaluate(Expr expr){
        return switch (expr){
            case Expr.Literal l -> MithrilResult.ok(l.value());
            case Expr.Grouping g -> evaluate(g.inner());
            case Expr.Unary u -> {
                MithrilResult<Object> rightResult = evaluate(u.right());
                if (rightResult.isErr()) yield rightResult;

                Object right = rightResult.unwrap();
                yield switch (u.operator().type()) {
                    case MINUS -> {
                        if (!(right instanceof Double)){
                            yield MithrilResult.err("Operand must be a number.", u.operator().line());
                        }
                        yield MithrilResult.ok(-(double) right);
                    }

                    case BANG -> MithrilResult.ok(!isTruthy(right));
                    default -> MithrilResult.err("Uknown unary operator.", u.operator().line());
                };
            }

            case Expr.Binary b -> {
                MithrilResult<Object> leftResult = evaluate(b.left());
                if (leftResult.isErr()) yield leftResult;

                MithrilResult<Object> rightResult = evaluate(b.right());
                if (rightResult.isErr()) yield rightResult;

                Object left = leftResult.unwrap();
                Object right = rightResult.unwrap();
                int line = b.operator().line();

                yield switch (b.operator().type()){
                    case MINUS -> {
                        if (!areNumbers(left, right))
                            yield MithrilResult.err("Operands must be numbers.", line);
                        yield MithrilResult.ok((double) left - (double) right);
                    }
                    case STAR -> {
                        if (!areNumbers(left, right))
                            yield MithrilResult.err("Operands must be numbers.", line);
                        yield MithrilResult.ok((double) left * (double) right);
                    }

                    case SLASH -> {
                        if (!areNumbers(left, right))
                            yield MithrilResult.err("Operands must be numbers.", line);
                        if ((double) right == 0)
                            yield MithrilResult.err("Division by zero.", line);
                        yield MithrilResult.ok((double) left / (double) right);
                    }
                    case PLUS -> {
                        if (left instanceof Double l && right instanceof Double r)
                            yield MithrilResult.ok(l + r);
                        if (left instanceof String l && right instanceof String r)
                            yield MithrilResult.ok(l + r);
                        yield MithrilResult.err("Operands must be two numbers or two strings.", line);
                    }

                    case GREATER -> {
                        if (!areNumbers(left, right))
                            yield MithrilResult.err("Operands must be numbers", line);
                        yield MithrilResult.ok((double) left > (double) right);
                    }

                    case GREATER_EQUAL -> {
                        if (!areNumbers(left, right))
                            yield MithrilResult.err("Operands must be numbers", line);
                        yield MithrilResult.ok((double) left >= (double) right);
                    }

                    case LESS -> {
                        if (!areNumbers(left, right))
                            yield MithrilResult.err("Operands must be numbers", line);
                        yield MithrilResult.ok((double) left < (double) right);
                    }

                    case LESS_EQUAL -> {
                        if (!areNumbers(left, right))
                            yield MithrilResult.err("Operands must be numbers", line);
                        yield MithrilResult.ok((double) left <= (double) right);
                    }

                    case EQUAL_EQUAL -> MithrilResult.ok(isEqual(left, right));
                    case BANG_EQUAL -> MithrilResult.ok(!isEqual(left, right));

                    default -> MithrilResult.err("Uknown binary operator.", line);
                };
            }
        };
    }

    private boolean isTruthy(Object value){
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        return true;
    }

    private boolean isEqual(Object a, Object b){
        if(a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private boolean areNumbers(Object a, Object b){
        return a instanceof Double && b instanceof Double;
    }

    private String stringify(Object value){
        if (value == null) return "naught";
        if (value instanceof Boolean b) return b ? "light" : "shadow";
        if (value instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d))
                return String.valueOf(d.longValue());
            return d.toString();
        }
        return value.toString();
    }
}
