package com.kris.mithrilVM;

import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public final List<Integer> code = new ArrayList<>();

    public final List<Object> constants = new ArrayList<>();

    public final List<Integer> lines = new ArrayList<>();

    public final List<Boolean> slotMutable = new ArrayList<>();

    public final List<Object> disassembleList = new ArrayList<>();

    public int variableCount = 0;

    public void write(int b, int line, boolean isOpCode) {
        code.add(b);

//      Code array disassembly
        Object value = b;
        if (isOpCode){
            value = OpCode.values()[b].name();
        }
        disassembleList.add(value);

        lines.add(line);
    }

    public int addConstant(Object value) {
        constants.add(value);
        return constants.size() - 1;
    }

    public int currentPosition() {
        return code.size();
    }

    public void patchJump(int jumpEnd) {
        int offset = code.size() - jumpEnd;
        code.set(jumpEnd - 2, (offset >> 8) & 0xff);
        code.set(jumpEnd - 1, offset & 0xff);
    }

    public int[] toIntArray() {
        int[] arr = new int[code.size()];
        for (int i = 0; i < code.size(); i++) arr[i] = code.get(i);
        return arr;
    }
}