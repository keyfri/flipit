package cz.agents.gtlibrary.experimental.imperfectrecall.blseqformlp.bnb.oldimpl;

import cz.agents.gtlibrary.algorithms.sequenceform.gensum.MILPTable;
import cz.agents.gtlibrary.algorithms.sequenceform.refinements.LPData;
import cz.agents.gtlibrary.algorithms.sequenceform.refinements.LPTable;
import cz.agents.gtlibrary.experimental.imperfectrecall.blseqformlp.SequenceFormIRInformationSet;
import cz.agents.gtlibrary.interfaces.Action;
import cz.agents.gtlibrary.interfaces.Sequence;
import cz.agents.gtlibrary.utils.Pair;
import ilog.concert.*;
import ilog.cplex.IloCplex;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Deprecated
public class BilinearTable extends MILPTable {
    private Map<Object, Pair<Object, Object>> bilinearVars;
    private Map<Object, Integer> bilinearPrecision; // infromation set -> precision
    private Map<Object, IloNumVar[][]> wVariables; // information set -> binary variables for the bilinear terms for that IS
    private Map<Object, IloNumVar[][]> rHatVariables; // sequence -> rHat variables for sequence
    private Map<Object, IloNumVar> deltaBehavioralVariables; // action -> delta x variable
    private Map<Object, IloNumVar> deltaSequenceVariables; // sequence -> delta x variable
    private Map<Object, IloRange> outgoingBilinearConstraints; // information set -> all constraints for the bilinear terms for that IS
    private Map<Object, IloRange> behavioralBilinearConstraints; // information set -> all constraints for the bilinear terms for that IS
    private Map<Object, double[][]> wValues;
    private final int INITIAL_MDT_PRECISION = 2;
    public static boolean fixPreviousDigits = false;
    final int digits = 10;

    public BilinearTable() {
        outgoingBilinearConstraints = new HashMap<>();
        behavioralBilinearConstraints = new HashMap<>();
        bilinearVars = new HashMap<>();
        bilinearPrecision = new HashMap<>();
        wVariables = new HashMap<>();
        rHatVariables = new HashMap<>();
        deltaBehavioralVariables = new HashMap<>();
        deltaSequenceVariables = new HashMap<>();
    }

    public void markAsBilinear(Object bilinearVarKey, Object cause1Key, Object cause2Key) {
        bilinearVars.put(bilinearVarKey, new Pair<>(cause1Key, cause2Key));
    }

    @Override
    public LPData toCplex() throws IloException {
        LPData data = super.toCplex();

        for (Map.Entry<Object,Pair<Object,Object>> entry : bilinearVars.entrySet()) {
            addBilinearConstraint(data,entry.getKey(),entry.getValue().getLeft(),entry.getValue().getRight(),INITIAL_MDT_PRECISION);
        }
        return data;
    }

    public void addBilinearConstraint(LPData data, Object product, Object sequence, Object behavioral, int precision) throws IloException {
        if (outgoingBilinearConstraints.containsKey(product))
            data.getSolver().delete(outgoingBilinearConstraints.get(product));

        if (behavioralBilinearConstraints.containsKey(behavioral))
            data.getSolver().delete(behavioralBilinearConstraints.get(behavioral));

        IloRange[] newConstraints = addMDTConstraints(data, product, sequence, behavioral, precision);
        behavioralBilinearConstraints.put(behavioral,newConstraints[0]);
        outgoingBilinearConstraints.put(product, newConstraints[1]);
        bilinearPrecision.put(product, precision);
    }

    public IloRange[] addMDTConstraints(LPData data, Object product, Object sequence, Object behavioral, int precision) throws IloException{
        IloCplex cplex = data.getSolver();
        IloNumVar rSequenceToIS = data.getVariables()[getVariableIndex(sequence)];
        IloNumVar xBehStrategy = data.getVariables()[getVariableIndex(behavioral)];
        IloNumVar rSequenceFromIS = data.getVariables()[getVariableIndex(product)];

        IloNumVar xBehDelta = deltaBehavioralVariables.get(behavioral);
        if (xBehDelta == null) {
            xBehDelta = cplex.numVar(0,1,IloNumVarType.Float, "DELTA_" + behavioral.toString());
            deltaBehavioralVariables.put(behavioral, xBehDelta);
//            cplex.addMaximize(cplex.diff(cplex.getObjective().getExpr(), xBehDelta));
        }

        IloNumVar xSeqDelta = deltaSequenceVariables.get(product);
        if (xSeqDelta == null) {
            xSeqDelta = cplex.numVar(0,1,IloNumVarType.Float, "DELTA_" + product.toString());
            deltaSequenceVariables.put(product, xSeqDelta);
//            cplex.getObjective().setExpr(cplex.diff(cplex.getObjective().getExpr(), cplex.prod(1,xSeqDelta)));
        }

        IloRange[] result = new IloRange[2];
        IloNumVar[][] w;

        if (wVariables.containsKey(behavioral)) {
            IloNumVar[][] existingWs = wVariables.get(behavioral);
            precision = Math.max(precision, existingWs[0].length);
            w = new IloNumVar[digits][precision];
            for (int d=0; d < digits; d++)
                for (int existingPrecision = 0; existingPrecision < existingWs[d].length; existingPrecision++) {
                    w[d][existingPrecision] = existingWs[d][existingPrecision];
                }
        } else {
            w = new IloNumVar[digits][precision];
        }

        IloNumVar[][] rHat = new IloNumVar[digits][precision];

        if (rHatVariables.containsKey(product)) {
            IloNumVar[][] existingRHat = rHatVariables.get(product);
            for (int d=0; d < digits; d++)
                for (int existingPrecision = 0; existingPrecision < existingRHat[d].length; existingPrecision++) {
                    rHat[d][existingPrecision] = existingRHat[d][existingPrecision];
                }
        }

        IloNumExpr productSum = xSeqDelta;
        IloNumExpr approxSum = xBehDelta;
//        IloNumExpr productSum = cplex.numExpr();
//        IloNumExpr approxSum = cplex.numExpr();
        for (int l=0; l<precision; l++) {
            IloNumExpr xSum = cplex.numExpr();
            IloNumExpr wSum = cplex.numExpr();
            boolean thisPrecisionExists = false;
            for (int k = 0; k < digits; k++) {
                if ((l == 0) && (k > 1)) continue;
                if (w[k][l] == null)
                    w[k][l] = cplex.numVar(0,1, IloNumVarType.Bool,"W_" + behavioral.toString() + "_"+ k + "_"+ l );
                    wSum = cplex.sum(wSum,w[k][l]);
                if (rHat[k][l] == null) {
                    rHat[k][l] = cplex.numVar(0, 1, IloNumVarType.Float, "RHAT_" + product + "_" + k + "_" + l);
                    cplex.addLe(rHat[k][l], w[k][l]);
                    cplex.addLe(rHat[k][l], rSequenceToIS);
                } else {
                    thisPrecisionExists = true;
                }

                productSum = cplex.sum(productSum, cplex.prod(cplex.constant(Math.pow(10, -(l)) * k), rHat[k][l]));
                xSum = cplex.sum(xSum, rHat[k][l]);
                approxSum = cplex.sum(approxSum, cplex.prod(cplex.constant(Math.pow(10, -(l)) * k), w[k][l]));


            }
            if (!thisPrecisionExists) {
                cplex.addEq(rSequenceToIS, xSum);
                cplex.addEq(wSum, 1);
                xBehDelta.setUB(Math.pow(10, -(precision-1)));
                cplex.addLe(xSeqDelta, cplex.prod(Math.pow(10, -(precision-1)), rSequenceToIS));
            }
        }

        result[0] = cplex.addEq(cplex.diff(xBehStrategy,approxSum),0);
//        result[1] = cplex.addLe(cplex.diff(rSequenceFromIS,cplex.sum(productSum, cplex.constant(Math.pow(100, -(precision))))),0);
        result[1] = cplex.addEq(cplex.diff(rSequenceFromIS,productSum),0);

        wVariables.put(behavioral, w);
        rHatVariables.put(product, rHat);
        return result;
    }

    public Map<Object, Pair<Object, Object>> getBilinearVars() {
        return bilinearVars;
    }

    public void refinePrecision(LPData data, Action action) throws IloException{
        if (fixPreviousDigits) {
            fixDigits(action);
        }
        for (Set<Sequence> ss : ((SequenceFormIRInformationSet)action.getInformationSet()).getOutgoingSequences().values())
            for (Sequence productSequence : ss) {
                if (productSequence.getLast().equals(action))
                    addBilinearConstraint(data, productSequence, bilinearVars.get(productSequence).getLeft(), bilinearVars.get(productSequence).getRight(), bilinearPrecision.get(productSequence) + 1);
        }
    }

    public Map<Object, IloNumVar[][]> getwVariables() {
        return wVariables;
    }

    public Map<Object, IloNumVar[][]> getrHatVariables() {
        return rHatVariables;
    }

    public boolean isFixPreviousDigits() {
        return fixPreviousDigits;
    }

    public void setFixPreviousDigits(boolean fixPreviousDigits) {
        this.fixPreviousDigits = fixPreviousDigits;
    }

    private void fixDigits(Action behavioral) throws IloException {
        int precision;
//        Set<Action> allActionsInSet = informationSet.getActions();
//        for (Object behavioral : allActionsInSet) {
            if (wVariables.containsKey(behavioral)) {
                IloNumVar[][] existingWs = wVariables.get(behavioral);
                precision = wValues.get(behavioral)[0].length;
                boolean overflowPossible = false;
                for (int k = 0; k < digits; k++) {
                    for (int l = 0; l < precision; l++) {
                        if ((l == 0) && (k > 1)) continue;
                        existingWs[k][l].setUB(0);
                    }
                }
                for (int k = 0; k < digits; k++) {
                    for (int l = 0; l < precision; l++) {
                        if ((l == 0) && (k > 1)) continue;
                        if (wValues.get(behavioral)[k][l] > 0.5) {
                            existingWs[k][l].setUB(1);
                            if ((l > 0 && k < 9) || (l == 0 && k == 0)) existingWs[k + 1][l].setUB(1);
                            if (k > 0) existingWs[k - 1][l].setUB(1);
                            if (l > 0 && k == 0) existingWs[9][l].setUB(1);

                        }
                        //                    if (wValues.get(behavioral)[k][l] > 0.5) {
                        //                        existingWs[k][l].setLB(1);
                        //                    } else {
                        //                        existingWs[k][l].setUB(0);
                        //                    }
                    }
                }
//            } else {
//                continue;
//            }
        }
    }

    public void storeWValues(LPData data) throws IloException {
        wValues = new HashMap<>();
        for (Object productSequence : bilinearVars.keySet()) {
            Object behavioral = bilinearVars.get(productSequence).getRight();
            if (wVariables.containsKey(behavioral)) {
                IloNumVar[][] existingWs = wVariables.get(behavioral);
                double[][] wVals = new double[digits][existingWs[9].length];
                for (int k = 0; k < digits; k++) {
                    for (int l = 0; l < existingWs[k].length; l++) {
                        if ((l == 0) && (k > 1)) continue;
                        wVals[k][l] = data.getSolver().getValue(existingWs[k][l]);
                    }
                }
                wValues.put(behavioral, wVals);
            }
        }
    }

    public Map<Object, IloNumVar> getDeltaBehavioralVariables() {
        return deltaBehavioralVariables;
    }

    public Map<Object, IloNumVar> getDeltaSequenceVariables() {
        return deltaSequenceVariables;
    }
}
