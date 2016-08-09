package br.unicamp.mtcontroller.codelets.state;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.exceptions.CodeletActivationBoundsException;
import br.unicamp.mtcontroller.constants.MemoryObjectName;
import com.fuzzylite.Engine;
import com.fuzzylite.rule.Rule;
import com.fuzzylite.rule.RuleBlock;
import com.fuzzylite.term.Trapezoid;
import com.fuzzylite.variable.InputVariable;
import com.fuzzylite.variable.OutputVariable;
import it.polito.appeal.traci.Lane;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Du on 16/02/16.
 */
public class StateSensor extends Codelet {

    private Engine engine;
    private Map<Lane, MemoryObject> mapVehiclesMemoryObject;
    private Map<Lane, MemoryObject> mapOccupancyMemoryObject;
    private Map<Lane, MemoryObject> mapMeanVelocityMemoryObject;
    private double dAverageOccupancy;
    private double dAverageMeanVelocity;
    private Map<Lane, List<String>> mapLaneVehicles;
    private Map<Lane, String> mapLaneState;

    private MemoryObject allMOS;
    private MemoryObject stateMO;
    private MemoryObject averageMeanVelocityMO;
    private MemoryObject averageOccupancystateMO;
    private MemoryObject vehiclesMO;
    private MemoryObject lanesStateMO;

    private OutputVariable outputOV;
    private InputVariable occupancyIV;
    private InputVariable velocityIV;
    private RuleBlock ruleBlock;
    private String sAgentId;
    private int timestamp = 100;


    public StateSensor(String sAgentId, int timestamp) {
        this.setEngine(new Engine("Agent State"));
        this.setsAgentId(sAgentId);
        buildOccupationInput();
        buildOutput();
        buildRules();
        getEngine().configure("", "", "Minimum", "Maximum", "Centroid");
        Logger.getLogger("").setLevel(Level.OFF);
        Logger.getLogger("*").setLevel(Level.OFF);
        setdAverageOccupancy(0d);
        setdAverageMeanVelocity(0d);
        setMapLaneVehicles(new HashMap<Lane, List<String>>());
        setMapLaneState(new HashMap<Lane, String>());
        setTimestamp(timestamp);
    }

    public void buildOccupationInput() {
        setOccupancyIV(new InputVariable("Occupancy"));
        getOccupancyIV().setRange(0, 1);
        getOccupancyIV().addTerm(new Trapezoid("LOW", 0, 0, 0.2, 0.4));
        getOccupancyIV().addTerm(new Trapezoid("MEDIUM_LOW", 0.2, 0.3, 0.4, 0.5));
        getOccupancyIV().addTerm(new Trapezoid("MEDIUM_HIGH", 0.4, 0.5, 0.6, 0.7));
        getOccupancyIV().addTerm(new Trapezoid("HIGH", 0.5, 0.7, 1, 1));
        getEngine().addInputVariable(getOccupancyIV());
    }


    /*public void buildMeanVelocityInput() {
        setOccupancyIV(new InputVariable("Velocity"));
        getOccupancyIV().setRange(0, 1);
        getOccupancyIV().addTerm(new Trapezoid("LOW", 0, 0, 0.2, 0.4));
        getOccupancyIV().addTerm(new Trapezoid("MEDIUM_LOW", 0.2, 0.3, 0.4, 0.5));
        getOccupancyIV().addTerm(new Trapezoid("MEDIUM_HIGH", 0.4, 0.5, 0.6, 0.7));
        getOccupancyIV().addTerm(new Trapezoid("HIGH", 0.5, 0.7, 1, 1));
        getEngine().addInputVariable(getOccupancyIV());
    }*/


    public void buildOutput() {
        setOutputOV(new OutputVariable());
        getOutputOV().setName("State");
        getOutputOV().setRange(0.000, 1.000);
        getOutputOV().setDefaultValue(Double.NaN);
        getOutputOV().addTerm(new Trapezoid("FREE", 0, 0, 0.2, 0.4));
        getOutputOV().addTerm(new Trapezoid("NORMAL", 0.2, 0.3, 0.4, 0.5));
        getOutputOV().addTerm(new Trapezoid("MEDIUM", 0.4, 0.5, 0.6, 0.7));
        getOutputOV().addTerm(new Trapezoid("CONGESTED", 0.5, 0.7, 1, 1));
        getEngine().addOutputVariable(getOutputOV());
    }


    public void buildRules() {
        setRuleBlock(new RuleBlock());
        getRuleBlock().addRule(Rule.parse("if Occupancy is LOW then State is FREE", getEngine()));
        getRuleBlock().addRule(Rule.parse("if Occupancy is MEDIUM_LOW then State is NORMAL", getEngine()));
        getRuleBlock().addRule(Rule.parse("if Occupancy is MEDIUM_HIGH then State is MEDIUM", getEngine()));
        getRuleBlock().addRule(Rule.parse("if Occupancy is HIGH then State is CONGESTED", getEngine()));
        getEngine().addRuleBlock(getRuleBlock());

    }


    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }


    @Override
    public void accessMemoryObjects() {

        if (getStateMO() == null)
            setStateMO(this.getOutput(MemoryObjectName.STATE_AGENT.toString(), 0));

        if (getAllMOS() == null) {
            setAllMOS(this.getInput(MemoryObjectName.ALL_MOS.toString(), 0));
            setMapOccupancyMemoryObject((Map<Lane, MemoryObject>) ((Map<String, Object>) getAllMOS().getI()).get("occupancy"));
            setMapMeanVelocityMemoryObject((Map<Lane, MemoryObject>) ((Map<String, Object>) getAllMOS().getI()).get("meanvelocity"));
            setMapVehiclesMemoryObject((Map<Lane, MemoryObject>) ((Map<String, Object>) getAllMOS().getI()).get("vehicles"));
        }

        if (getAverageMeanVelocityMO() == null)
            setAverageMeanVelocityMO(this.getOutput(MemoryObjectName.AVERAGE_MEAN_VELOCITY.toString(), 0));

        if (getAverageOccupancystateMO() == null)
            setAverageOccupancystateMO(this.getOutput(MemoryObjectName.AVERAGE_OCCUPANCY.toString(), 0));

        if (getMapLaneState() == null)
            setAverageOccupancystateMO(this.getOutput(MemoryObjectName.AVERAGE_OCCUPANCY.toString(), 0));

        if (getVehiclesMO() == null)
            setVehiclesMO(this.getOutput(MemoryObjectName.SUM_VEHICLES.toString(), 0));

        if (getLanesStateMO() == null)
            setLanesStateMO(this.getOutput(MemoryObjectName.LANES_STATE.toString(), 0));

    }

    @Override
    public void calculateActivation() {
        try {
            this.setActivation(0.0);
        } catch (CodeletActivationBoundsException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void proc() {

        if (getMapOccupancyMemoryObject().size() > 0) {
            setdAverageOccupancy(0);

            getMapLaneState().clear();

            for (Map.Entry<Lane, MemoryObject> memoryObject : getMapOccupancyMemoryObject().entrySet()) {

                MemoryObject occupancyMO = memoryObject.getValue();
                setdAverageOccupancy(getdAverageOccupancy() + Double.parseDouble(occupancyMO.getI().toString()));

                getOccupancyIV().setInputValue(Double.parseDouble(occupancyMO.getI().toString()));
                getEngine().process();
                getMapLaneState().put(memoryObject.getKey(), getOutputOV().highestMembershipTerm(getOutputOV().getOutputValue()).getName());
            }

            getLanesStateMO().setI(getMapLaneState());

            setdAverageOccupancy(getdAverageOccupancy() / getMapOccupancyMemoryObject().size());

            getOccupancyIV().setInputValue(getdAverageOccupancy());
            getEngine().process();

            stateMO.setI(getOutputOV().highestMembershipTerm(getOutputOV().getOutputValue()).getName());
            getAverageOccupancystateMO().setI(getdAverageOccupancy());

        }

        if (getMapMeanVelocityMemoryObject().size() > 0) {

            setdAverageMeanVelocity(0);

            for (Map.Entry<Lane, MemoryObject> memoryObject : getMapMeanVelocityMemoryObject().entrySet()) {

                MemoryObject meanVelocityMO = memoryObject.getValue();
                setdAverageMeanVelocity(getdAverageMeanVelocity() + Double.parseDouble(meanVelocityMO.getI().toString()));
            }

            setdAverageMeanVelocity(getdAverageMeanVelocity() / getMapMeanVelocityMemoryObject().size());

            getAverageMeanVelocityMO().setI(getdAverageMeanVelocity());

        }

        if (getMapVehiclesMemoryObject().size() > 0) {

            for (Map.Entry<Lane, MemoryObject> memoryObject : getMapVehiclesMemoryObject().entrySet()) {

                MemoryObject vehiclesMO = memoryObject.getValue();

                if (vehiclesMO.getI() instanceof List)
                    getMapLaneVehicles().put(memoryObject.getKey(), (List<String>) vehiclesMO.getI());
            }

            getVehiclesMO().setI(getMapLaneVehicles());

        }

        try {
            Thread.sleep(getTimestamp() * 5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public double getdAverageOccupancy() {
        return dAverageOccupancy;
    }

    public void setdAverageOccupancy(double dAverageOccupancy) {
        this.dAverageOccupancy = dAverageOccupancy;
    }

    public MemoryObject getStateMO() {
        return stateMO;
    }

    public void setStateMO(MemoryObject stateMO) {
        this.stateMO = stateMO;
    }

    public OutputVariable getOutputOV() {
        return outputOV;
    }

    public void setOutputOV(OutputVariable outputOV) {
        this.outputOV = outputOV;
    }

    public InputVariable getOccupancyIV() {
        return occupancyIV;
    }

    public void setOccupancyIV(InputVariable occupancyIV) {
        this.occupancyIV = occupancyIV;
    }

    public RuleBlock getRuleBlock() {
        return ruleBlock;
    }

    public void setRuleBlock(RuleBlock ruleBlock) {
        this.ruleBlock = ruleBlock;
    }

    public String getsAgentId() {
        return sAgentId;
    }

    public void setsAgentId(String sAgentId) {
        this.sAgentId = sAgentId;
    }

    public MemoryObject getAllMOS() {
        return allMOS;
    }

    public void setAllMOS(MemoryObject allMOS) {
        this.allMOS = allMOS;
    }

    public Map<Lane, MemoryObject> getMapVehiclesMemoryObject() {
        return mapVehiclesMemoryObject;
    }

    public void setMapVehiclesMemoryObject(Map<Lane, MemoryObject> mapVehiclesMemoryObject) {
        this.mapVehiclesMemoryObject = mapVehiclesMemoryObject;
    }

    public double getdAverageMeanVelocity() {
        return dAverageMeanVelocity;
    }

    public void setdAverageMeanVelocity(double dAverageMeanVelocity) {
        this.dAverageMeanVelocity = dAverageMeanVelocity;
    }

    public Map<Lane, List<String>> getMapLaneVehicles() {
        return mapLaneVehicles;
    }

    public void setMapLaneVehicles(Map<Lane, List<String>> mapLaneVehicles) {
        this.mapLaneVehicles = mapLaneVehicles;
    }

    public MemoryObject getAverageMeanVelocityMO() {
        return averageMeanVelocityMO;
    }

    public void setAverageMeanVelocityMO(MemoryObject averageMeanVelocityMO) {
        this.averageMeanVelocityMO = averageMeanVelocityMO;
    }

    public MemoryObject getAverageOccupancystateMO() {
        return averageOccupancystateMO;
    }

    public void setAverageOccupancystateMO(MemoryObject averageOccupancystateMO) {
        this.averageOccupancystateMO = averageOccupancystateMO;
    }

    public MemoryObject getVehiclesMO() {
        return vehiclesMO;
    }

    public void setVehiclesMO(MemoryObject vehiclesMO) {
        this.vehiclesMO = vehiclesMO;
    }

    public Map<Lane, MemoryObject> getMapOccupancyMemoryObject() {
        return mapOccupancyMemoryObject;
    }

    public void setMapOccupancyMemoryObject(Map<Lane, MemoryObject> mapOccupancyMemoryObject) {
        this.mapOccupancyMemoryObject = mapOccupancyMemoryObject;
    }

    public Map<Lane, MemoryObject> getMapMeanVelocityMemoryObject() {
        return mapMeanVelocityMemoryObject;
    }

    public void setMapMeanVelocityMemoryObject(Map<Lane, MemoryObject> mapMeanVelocityMemoryObject) {
        this.mapMeanVelocityMemoryObject = mapMeanVelocityMemoryObject;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public InputVariable getVelocityIV() {
        return velocityIV;
    }

    public void setVelocityIV(InputVariable velocityIV) {
        this.velocityIV = velocityIV;
    }

    public Map<Lane, String> getMapLaneState() {
        return mapLaneState;
    }

    public void setMapLaneState(Map<Lane, String> mapLaneState) {
        this.mapLaneState = mapLaneState;
    }

    public MemoryObject getLanesStateMO() {
        return lanesStateMO;
    }

    public void setLanesStateMO(MemoryObject lanesStateMO) {
        this.lanesStateMO = lanesStateMO;
    }
}
