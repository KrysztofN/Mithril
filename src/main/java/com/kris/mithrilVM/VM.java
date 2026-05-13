package com.kris.mithrilVM;

import com.kris.mithril.Mithril;

public class VM {
    private static final OpCode[] OPCODES = OpCode.values();
    private static final int STACK_MAX = 256;
    private final Chunk chunk;
    private final int[] code;
    private final Object[] constants;
    private final boolean[] slotMutable;
    private final Object[] variables;
    private int ip = 0;
    private final Object[] stack = new Object[STACK_MAX];
    private int stackTop = 0;

    public VM(Chunk chunk) {
        this.chunk = chunk;
        this.code = chunk.toIntArray();
        this.constants = chunk.constants.toArray();
        this.variables = new Object[chunk.variableCount];
        this.slotMutable = new boolean[chunk.variableCount];

        for (int i = 0; i < chunk.slotMutable.size(); i++)
            slotMutable[i] = chunk.slotMutable.get(i);
    }

    public boolean run() {
        while (true) {
            OpCode op = OPCODES[code[ip++]];
            switch (op) {
                case CONSTANT -> push(constants[code[ip++]]);

                case TRUE -> push(true);

                case FALSE -> push(false);

                case NAUGHT -> push(null);

                case ADD -> {
                    Object right = pop(), left = pop();
                    if (left instanceof Double l && right instanceof Double r) push(l + r);
                    else if (left instanceof String l && right instanceof String r) push(l + r);
                    else {
                        if (runtimeError("Operands must be two numbers or two strings.")) return false;
                    }
                }

                case SUBTRACT -> {
                    Object right = pop(), left = pop();
                    if (!areNumbers(left, right)) {
                        if (runtimeError("Operands must be numbers.")) return false;
                    }
                    else push((double) left - (double) right);
                }

                case MULTIPLY -> {
                    Object right = pop(), left = pop();
                    if (!areNumbers(left, right)) {
                        if (runtimeError("Operands must be numbers.")) return false;
                    }
                    else push((double) left * (double) right);
                }

                case DIVIDE -> {
                    Object right = pop(), left = pop();
                    if (!areNumbers(left, right)) {
                        if (runtimeError("Operands must be numbers.")) return false;
                    }
                    else if ((double) right == 0) {
                        if (runtimeError("Division by zero.")) return false;
                    }
                    else push((double) left / (double) right);
                }

                case NEGATE -> {
                    Object val = pop();
                    if (!(val instanceof Double)) {
                        if (runtimeError("Operand must be a number.")) return false;
                    }
                    else push(-(double) val);
                }

                case NOT -> push(!isTruthy(pop()));

                case EQUAL -> {
                    Object r = pop();
                    push(isEqual(pop(), r));
                }

                case NOT_EQUAL -> {
                    Object r = pop();
                    push(!isEqual(pop(), r));
                }

                case GREATER -> {
                    Object right = pop(), left = pop();
                    if (!areNumbers(left, right)) {
                        if (runtimeError("Operands must be numbers.")) return false;
                    }
                    else push((double) left > (double) right);
                }

                case GREATER_EQUAL -> {
                    Object right = pop(), left = pop();
                    if (!areNumbers(left, right)) {
                        if (runtimeError("Operands must be numbers.")) return false;
                    }
                    else push((double) left >= (double) right);
                }

                case LESS -> {
                    Object right = pop(), left = pop();
                    if (!areNumbers(left, right)) {
                        if (runtimeError("Operands must be numbers.")) return false;
                    }
                    else push((double) left < (double) right);
                }

                case LESS_EQUAL -> {
                    Object right = pop(), left = pop();
                    if (!areNumbers(left, right)) {
                        if (runtimeError("Operands must be numbers.")) return false;
                    }
                    else push((double) left <= (double) right);
                }

                case SPEAK -> System.out.println(stringify(pop()));

                case POP -> pop();

                case DEFINE_VAR -> {
                    int slot = code[ip++];
                    variables[slot] = pop();
                }

                case GET_VAR -> {
                    push(variables[code[ip++]]);
                }

                case SET_VAR -> {
                    int slot = code[ip++];
                    if (!slotMutable[slot]) {
                        if (runtimeError("Cannot reassign rune.")) return false;
                    }
                    variables[slot] = pop();
                }

                case JUMP -> {
                    int o = (code[ip++] << 8) | code[ip++];
                    ip += o;
                }

                case JUMP_IF_FALSE -> {
                    int o = (code[ip++] << 8) | code[ip++];
                    if (!isTruthy(peek())) ip += o;
                }

                case LOOP -> {
                    int o = (code[ip++] << 8) | code[ip++];
                    ip -= o;
                }

                case RETURN -> {
                    return true;
                }
            }
        }
    }

    private void push(Object value) {
        stack[stackTop++] = value;
    }

    private Object pop() {
        return stack[--stackTop];
    }

    private Object peek() {
        return stack[stackTop - 1];
    }

    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private boolean areNumbers(Object a, Object b) {
        return a instanceof Double && b instanceof Double;
    }

    private String stringify(Object value) {
        if (value == null) return "naught";
        if (value instanceof Boolean b) return b ? "light" : "shadow";
        if (value instanceof Double d) {
            if (d == Math.floor(d) && !Double.isInfinite(d)) return String.valueOf(d.longValue());
            return d.toString();
        }
        return value.toString();
    }

    private boolean runtimeError(String message) {
        int line = chunk.lines.get(ip - 1);
        System.err.println("[line " + line + "] Runtime error: " + message);
        Mithril.hadRuntimeError = true;
        return true;
    }
}