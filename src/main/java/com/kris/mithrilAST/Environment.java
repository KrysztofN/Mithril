package com.kris.mithrilAST;

import com.kris.mithril.MithrilResult;
import com.kris.mithril.Token;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Environment {

    private final Environment parent;

    private final Map<String, Object> values = new HashMap<>();

    private final Set<String> immutables = new HashSet<>();

    public Environment() {
        this.parent = null;
    }

    public Environment(Environment parent) {
        this.parent = parent;
    }

    public void defineRune(String name, Object value){
        values.put(name, value);
        immutables.add(name);
    }

    public void defineForge(String name, Object value){
        values.put(name, value);
    }

    public MithrilResult<Object> get(Token name){
        if (values.containsKey(name.lexeme())){
            return MithrilResult.ok(values.get(name.lexeme()));
        }
        if (parent != null) return parent.get(name);

        return MithrilResult.err("Undefined variable '" + name.lexeme() + "'.", name.line());
    }

    public MithrilResult<Object> assign(Token name, Object value) {
        if (values.containsKey(name.lexeme())) {
            if (immutables.contains(name.lexeme())) {
                return MithrilResult.err(
                    "Cannot reassign rune '" + name.lexeme() + "' - it is immutable.", name.line());
            }
            values.put(name.lexeme(), value);
            return MithrilResult.ok(null);
        }
        if (parent != null) return parent.assign(name, value);

        return MithrilResult.err("Undefined variable '" + name.lexeme() + "'.", name.line());
    }
}
