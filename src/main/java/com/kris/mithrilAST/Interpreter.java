package com.kris.mithril;

import java.util.List;

public class Interpreter {

    private final Environment globalEnv  = new Environment();

    private Environment currentEnv = globalEnv;

    public void interpret(List<Stmt> statements){
        for(Stmt stmt : statements){
            MithrilResult<Object> result = execute(stmt);
            if (result instanceof MithrilResult.Err<Object> err){
                System.err.println("[line " + err.line() + "] Runtime error: " + err.message());
                Mithril.hadRuntimeError = true;
                return;
            }
        }
    }

    private MithrilResult<Object> execute(Stmt stmt) {
        return switch (stmt) {

            case Stmt.Expression s -> {
                MithrilResult<Object> result = evaluate(s.expression());
                yield result.isErr() ? result : MithrilResult.ok(null);
            }

            case Stmt.Speak s -> {
                MithrilResult<Object> result = evaluate(s.expression());
                if (result.isErr()) yield result;
                System.out.println(stringify(result.unwrap()));
                yield MithrilResult.ok(null);
            }

            case Stmt.VarDeclaration s -> {
                MithrilResult<Object> result = evaluate(s.initializer());
                if (result.isErr()) yield result;
                if (s.mutable()) currentEnv.defineForge(s.name().lexeme(), result.unwrap());
                else currentEnv.defineRune(s.name().lexeme(), result.unwrap());
                yield MithrilResult.ok(null);
            }

            case Stmt.Assign s -> {
                MithrilResult<Object> result = evaluate(s.value());
                if (result.isErr()) yield result;
                yield currentEnv.assign(s.name(), result.unwrap());
            }

            case Stmt.Block s -> executeBlock(s, new Environment(currentEnv));

            case Stmt.Should s -> {
                MithrilResult<Object> condResult = evaluate(s.condition());
                if (condResult.isErr()) yield condResult;

                if (isTruthy(condResult.unwrap())) {
                    yield executeBlock(s.thenBlock(), new Environment(currentEnv));
                } else if (s.elseBlock() != null) {
                    yield executeBlock(s.elseBlock(), new Environment(currentEnv));
                }
                yield MithrilResult.ok(null);
            }

            case Stmt.Whilst s -> {
                while (true) {
                    MithrilResult<Object> condResult = evaluate(s.condition());
                    if (condResult.isErr()) yield condResult;
                    if (!isTruthy(condResult.unwrap())) break;

                    MithrilResult<Object> bodyResult = executeBlock(s.body(), new Environment(currentEnv));
                    if (bodyResult.isErr()) yield bodyResult;
                }
                yield MithrilResult.ok(null);
            }
        };
    }

    private MithrilResult<Object> executeBlock(Stmt.Block block, Environment blockEnv) {
        Environment previous = currentEnv;
        try {
            currentEnv = blockEnv;
            for (Stmt stmt : block.statements()) {
                MithrilResult<Object> result = execute(stmt);
                if (result.isErr()) return result;
            }
            return MithrilResult.ok(null);
        } finally {
            currentEnv = previous;
        }
    }

    private MithrilResult<Object> evaluate(Expr expr){
        return switch (expr){
            case Expr.Literal l -> MithrilResult.ok(l.value());
            case Expr.Grouping g -> evaluate(g.inner());
            case Expr.Variable v -> currentEnv.get(v.name());
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
