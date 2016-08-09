package br.unicamp.mtcontroller.codelets.sensors;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.mtcontroller.comunication.SingleAccessQuery;
import br.unicamp.mtcontroller.constants.MemoryObjectName;
import br.unicamp.mtcontroller.entity.TrafficLightLinkStatus;
import it.polito.appeal.traci.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by Du on 22/01/16.
 */
public class TrafficLightSensor extends Codelet {

    private List<ControlledLink> controlledLinks;

    private TrafficLight trafficLight;
    private MemoryObject trafficLightProgramMO;
    private MemoryObject trafficLinkPhaseMO;
    private MemoryObject changingPhaseMO;
    private int timestamp = 100;



    public TrafficLightSensor(TrafficLight trafficLight, int timestamp, List<ControlledLink> controlledLinks){
        this.setTrafficLight(trafficLight);
        this.setControlledLinks(controlledLinks);
        this.setTimestamp(timestamp);
    }

    @Override
    public void accessMemoryObjects() {
        if (getTrafficLightProgramMO() == null)
            setTrafficLightProgramMO(this.getOutput(MemoryObjectName.TRAFFICLIGHT_PROGRAM_PHASES.toString()));

        if(getTrafficLinkPhaseMO() == null)
            setTrafficLinkPhaseMO(this.getOutput(MemoryObjectName.TRAFFICLIGHT_LINKS_PHASE.toString()));

        if(getChangingPhaseMO() ==null)
            setChangingPhaseMO(this.getBroadcast(MemoryObjectName.TRAFFICLIGHT_MEMORY_CHANGED.toString()));

    }

    @Override
    public void calculateActivation() {



    }

    @Override
    public synchronized void proc() {
        try {

            if (getChangingPhaseMO().getI() != null && getChangingPhaseMO().getI() != "0") {
                TLState tlState = (TLState) getChangingPhaseMO().getI();
                LightState[] lightStates = tlState.lightStates;

                List<TrafficLightLinkStatus> lstOfLinkPhase = Collections.synchronizedList(new ArrayList<>());

                for (int i = 0; i < lightStates.length; i++) {
                    lstOfLinkPhase.add(new TrafficLightLinkStatus(lightStates[i], (getControlledLinks()).get(i)));
                }

                getTrafficLinkPhaseMO().setI(lstOfLinkPhase);
            }

            else {

                ReadObjectVarQuery readCompleteDefinitionQuery = getTrafficLight().queryReadCompleteDefinition();
                ReadObjectVarQuery readCurrentPhase = getTrafficLight().queryReadCurrentPhase();

                List<ReadObjectVarQuery<?>> readObjectVarQueries = Collections.synchronizedList(new ArrayList<ReadObjectVarQuery<?>>());
                readObjectVarQueries.add(readCompleteDefinitionQuery);
                readObjectVarQueries.add(readCurrentPhase);

                Map<Integer, Object> objectMap = SingleAccessQuery.executeReadQueries(readObjectVarQueries);

                Object completeDef = objectMap.containsKey(readCompleteDefinitionQuery.hashCode()) ? objectMap.get(readCompleteDefinitionQuery.hashCode()) : null;

                if (completeDef != null) {
                    List<TLState> trafficLightStatus = Collections.synchronizedList(new ArrayList<TLState>());
                    Logic[] logics = ((Program) completeDef).getLogics();

                    for (Logic logic : logics) {
                        Phase[] phases = logic.getPhases();
                        for (Phase phase : phases) {
                            trafficLightStatus.add(phase.getState());

                        }
                    }

                    getTrafficLightProgramMO().setI(trafficLightStatus);
                }

                Object currentIndexPhase = objectMap.containsKey(readCurrentPhase.hashCode()) ? objectMap.get(readCurrentPhase.hashCode()) : null;

                if (currentIndexPhase != null) {
                    if (getTrafficLightProgramMO() != null) {

                        LightState[] lightStates = (((List<TLState>) getTrafficLightProgramMO().getI()).get((int) currentIndexPhase)).lightStates;

                        ArrayList<TrafficLightLinkStatus> lstOfLinkPhase = new ArrayList<>();

                        for (int i = 0; i < lightStates.length; i++) {
                            lstOfLinkPhase.add(new TrafficLightLinkStatus(lightStates[i], (getControlledLinks()).get(i)));
                        }

                        getTrafficLinkPhaseMO().setI(lstOfLinkPhase);
                    }
                }
            }

            Thread.sleep(getTimestamp()*2);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    public synchronized TrafficLight getTrafficLight() {
        return trafficLight;
    }

    public synchronized void setTrafficLight(TrafficLight trafficLight) {
        this.trafficLight = trafficLight;
    }

    public synchronized MemoryObject getTrafficLightProgramMO() {
        return trafficLightProgramMO;
    }

    public synchronized void setTrafficLightProgramMO(MemoryObject trafficLightProgramMO) {
        this.trafficLightProgramMO = trafficLightProgramMO;
    }

    public synchronized MemoryObject getTrafficLinkPhaseMO() {
        return trafficLinkPhaseMO;
    }

    public synchronized void setTrafficLinkPhaseMO(MemoryObject trafficLinkPhaseMO) {
        this.trafficLinkPhaseMO = trafficLinkPhaseMO;
    }

    public synchronized List<ControlledLink> getControlledLinks() {
        return controlledLinks;
    }

    public synchronized void setControlledLinks(List<ControlledLink> controlledLinks) {
        this.controlledLinks = controlledLinks;
    }

    public synchronized MemoryObject getChangingPhaseMO() {
        return changingPhaseMO;
    }

    public synchronized void setChangingPhaseMO(MemoryObject changingPhaseMO) {
        this.changingPhaseMO = changingPhaseMO;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
}
