package optimizer;

import ir.*;
import ir.operand.*;

import java.util.*;

public class ControlFlowGraph {
    
    private String name;
    private List<IRInstruction> instructions;
    private List<ArrayList<Integer>> ins;
    private List<ArrayList<Integer>> outs;
    private List<ArrayList<String>> defs;
    private List<ArrayList<String>> uses;
    private int size;

    public ControlFlowGraph(IRFunction function) {
        this.name = function.name;
        this.instructions = function.instructions;
        this.size = (instructions.get(0).irLineNumber);
        this.ins = new ArrayList<ArrayList<Integer>>(instructions.size());
        this.outs = new ArrayList<ArrayList<Integer>>(instructions.size());
        this.defs = new ArrayList<ArrayList<String>>(instructions.size());
        this.uses = new ArrayList<ArrayList<String>>(instructions.size());
        for (int i = 0; i < instructions.size(); i++) {
            ins.add(i, new ArrayList<Integer>());
            outs.add(i, new ArrayList<Integer>());
            defs.add(i, new ArrayList<String>());
            uses.add(i, new ArrayList<String>());
        }
        buildCFG();
    }

    private void buildCFG() {
        Map<String, Integer> labelLineNums = new HashMap<>();

        for (IRInstruction inst : instructions) {
            if (inst.opCode == IRInstruction.OpCode.LABEL) {
                String label = ((IRLabelOperand) inst.operands[0]).getName();
                labelLineNums.put(label, Integer.valueOf(inst.irLineNumber - size));
            }
        }

        for (int i = 0; i < instructions.size(); i++) {
            IRInstruction inst = instructions.get(i);
            if (inst.opCode == IRInstruction.OpCode.GOTO) {
                String label = ((IRLabelOperand) inst.operands[0]).getName();
                int lineNum = labelLineNums.get(label);
                outs.get(i).add(Integer.valueOf(lineNum));
                ins.get(lineNum).add(Integer.valueOf(i));
            } else {
                if (inst.opCode == IRInstruction.OpCode.BREQ
                || inst.opCode == IRInstruction.OpCode.BRNEQ
                || inst.opCode == IRInstruction.OpCode.BRGEQ
                || inst.opCode == IRInstruction.OpCode.BRLT
                || inst.opCode == IRInstruction.OpCode.BRGT
                || inst.opCode == IRInstruction.OpCode.BRLEQ) {
                    String label = ((IRLabelOperand) inst.operands[0]).getName();
                    int lineNum = labelLineNums.get(label);
                    outs.get(i).add(Integer.valueOf(lineNum));
                    ins.get(lineNum).add(Integer.valueOf(i));
                    for (int j = 1; j < 3; j++) {
                        String variable = filterVariable(inst.operands[j]);
                        if (variable != "") {
                            uses.get(inst.irLineNumber - size).add(variable);
                        }
                    }
                } else if (inst.opCode == IRInstruction.OpCode.ADD
                || inst.opCode == IRInstruction.OpCode.SUB
                || inst.opCode == IRInstruction.OpCode.DIV
                || inst.opCode == IRInstruction.OpCode.MULT
                || inst.opCode == IRInstruction.OpCode.AND
                || inst.opCode == IRInstruction.OpCode.OR) {
                    for (int j = 1; j < 3; j++) {
                        String variable = filterVariable(inst.operands[j]);
                        if (variable != "") {
                            uses.get(inst.irLineNumber - size).add(variable);
                        }
                    }
                    defs.get(inst.irLineNumber - size).add(((IRVariableOperand) inst.operands[0]).getName());
                } else if (inst.opCode == IRInstruction.OpCode.ASSIGN) {
                    if (inst.operands.length != 3) {
                        String variable = filterVariable(inst.operands[1]);
                        if (variable != "") {
                            uses.get(inst.irLineNumber - size).add(variable);
                        }
                        defs.get(inst.irLineNumber - size).add(((IRVariableOperand) inst.operands[0]).getName());
                    }
                } else if (inst.opCode == IRInstruction.OpCode.ARRAY_STORE) {
                    String variable = filterVariable(inst.operands[0]);
                    if (variable != "") {
                        uses.get(inst.irLineNumber - size).add(variable);
                    }
                } else if (inst.opCode == IRInstruction.OpCode.ARRAY_LOAD) {
                    String variable = filterVariable(inst.operands[0]);
                    if (variable != "") {
                        defs.get(inst.irLineNumber - size).add(variable);
                    }
                } else if (inst.opCode == IRInstruction.OpCode.CALL) {
                    for (int j = 0; j < inst.operands.length; j++) {
                        if (inst.operands[j] instanceof IRVariableOperand) {
                            String variable = filterVariable(inst.operands[j]);
                            if (variable != "") {
                                uses.get(inst.irLineNumber - size).add(variable);
                            }
                        }
                    }
                } else if (inst.opCode == IRInstruction.OpCode.CALLR) {
                    for (int j = 1; j < inst.operands.length; j++) {
                        if (inst.operands[j] instanceof IRVariableOperand) {
                            String variable = filterVariable(inst.operands[j]);
                            if (variable != "") {
                                uses.get(inst.irLineNumber - size).add(variable);
                            }
                        }
                    }
                    defs.get(inst.irLineNumber - size).add(((IRVariableOperand) inst.operands[0]).getName());
                }
                if (i + 1 < instructions.size()) {
                    outs.get(i).add(Integer.valueOf(i+1));
                    ins.get(i+1).add(Integer.valueOf(i));
                }
            }
        }
    }

    private String filterVariable(IROperand op) {
        if (op instanceof IRVariableOperand) {
            return ((IRVariableOperand) op).getName();
        } else {
            return "";
        }
    }

    public String getName() {
        return this.name;
    }

    public List<ArrayList<Integer>> getIns() {
        return this.ins;
    }

    public List<ArrayList<Integer>> getOuts() {
        return this.outs;
    }

    public List<IRInstruction> getInstructions() {
        return this.instructions;
    }

    public String toString() {
        StringBuilder strblder = new StringBuilder();
        strblder.append("Function name: " + this.name + "\n");
        for (int i = 0; i < instructions.size(); i++) {
            strblder.append(instructions.get(i).toString() + "; (* ins: " + ins.get(i).toString() + "\touts: " + outs.get(i).toString() + "*)\n");
            strblder.append("(* defs: " + defs.get(i).toString() + "; uses: " + uses.get(i).toString() + " *)\n");
        }
        return strblder.toString();
    }
}