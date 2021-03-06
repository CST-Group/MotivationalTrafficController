package br.unicamp.mtcontroller.codelets.motivational.goals;

import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.motivational.Drive;
import br.unicamp.cst.motivational.Goal;
import br.unicamp.mtcontroller.constants.MemoryObjectName;
import br.unicamp.mtcontroller.entity.TrafficLightLinkStatus;
import it.polito.appeal.traci.LightState;
import it.polito.appeal.traci.TLState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Du on 15/03/16.
 */
public class LaneHelpGoal extends Goal {

    private MemoryObject changingPhaseMO;
    private MemoryObject trafficLinkPhaseMO;
    private List<Integer> lstOfIndexToGreenWave;
    private int timestamp = 100;

    public LaneHelpGoal(String name, int timestamp, int steps, int minSteps, double interventionThreshold, double belowUrgentInterventionThreshold, double priorityHighLevel) {
        super(name, steps, minSteps, interventionThreshold, belowUrgentInterventionThreshold, priorityHighLevel);

        this.setTimestamp(timestamp);

        this.setLstOfIndexToGreenWave(Collections.synchronizedList(new ArrayList<Integer>()));
    }


    public LaneHelpGoal(String name, int timestamp, int steps, int minSteps, double interventionThreshold, double belowUrgentInterventionThreshold, double priorityHighLevel, List<Integer> lstOfIndexToGreenWave) {
        super(name, steps, minSteps, interventionThreshold, belowUrgentInterventionThreshold, priorityHighLevel);

        this.setTimestamp(timestamp);

        this.setLstOfIndexToGreenWave(lstOfIndexToGreenWave);
    }


    @Override
    public synchronized double calculateUrgentVote(List<Drive> lstOfHighPriorityDrive) {
        return lstOfHighPriorityDrive.size() == 0 ? 0 : (lstOfHighPriorityDrive.stream().mapToDouble(drive -> drive.getActivation()).sum() / lstOfHighPriorityDrive.size());
    }


    @Override
    public synchronized double calculateVote(List<Drive> listOfDrivesVote) {
        List<Drive> lstOfHighLevelDrive = listOfDrivesVote.stream().filter(drive -> drive.getPriority() >= getPriorityHighLevel()).collect(Collectors.toList());
        List<Drive> lstOfLowLevelDrive = listOfDrivesVote.stream().filter(drive -> drive.getPriority() < getPriorityHighLevel()).collect(Collectors.toList());


        double dHighVote = lstOfHighLevelDrive.size() == 0 ? 0 : lstOfHighLevelDrive.stream().mapToDouble(drive -> drive.getActivation()).sum() /
                lstOfHighLevelDrive.size();

        double dLowVote = lstOfLowLevelDrive.size() == 0 ? 0 : lstOfLowLevelDrive.stream().mapToDouble(drive -> drive.getActivation()).sum() /
                lstOfLowLevelDrive.size();

        return (dHighVote * 1 + dLowVote * 0);

    }


    @Override
    public synchronized void executeActions() {

        List<TrafficLightLinkStatus> lstOfTrafficLightLinkStatus = (List<TrafficLightLinkStatus>) getTrafficLinkPhaseMO().getI();

        LightState[] lightStates = new LightState[lstOfTrafficLightLinkStatus.size()];

        try {
            if (getExecutedSteps() == 0 || isbPause()) {

                getChangingPhaseMO().setI(showYellowWave(lstOfTrafficLightLinkStatus, lightStates));
                Thread.sleep(getTimestamp() * 7);

                getChangingPhaseMO().setI(showRedWave(lightStates));
                Thread.sleep(getTimestamp() * 3);

            } else
                getChangingPhaseMO().setI(showGreenWave(lstOfTrafficLightLinkStatus, lightStates));


            Thread.sleep(getTimestamp());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public TLState showRedWave(LightState[] lightStates) {
        for (int i = 0; i < lightStates.length; i++) {
            lightStates[i] = LightState.RED;
        }

        TLState tlStateRed = new TLState(lightStates);

        return tlStateRed;
    }

    public TLState showYellowWave(List<TrafficLightLinkStatus> lstOfTrafficLightLinkStatus, LightState[] lightStates) {
        for (int i = 0; i < lstOfTrafficLightLinkStatus.size(); i++) {

            if (lstOfTrafficLightLinkStatus.get(i).getPhase().isGreen()) {
                lightStates[i] = LightState.YELLOW;
            } else {
                lightStates[i] = LightState.RED;
            }

        }

        TLState tlStateYellow = new TLState(lightStates);

        return tlStateYellow;
    }

    public TLState showGreenWave(List<TrafficLightLinkStatus> lstOfTrafficLightLinkStatus, LightState[] lightStates) {
        for (int i = 0; i < lstOfTrafficLightLinkStatus.size(); i++) {
            if (lstOfTrafficLightLinkStatus.get(i).getControlledLink().getIncomingLane().getID().equals(getName())) {
                lightStates[i] = LightState.GREEN_NODECEL;

            } else {
                lightStates[i] = LightState.RED;
            }
        }

        for (int j = 0; j < getLstOfIndexToGreenWave().size(); j++) {
            lightStates[getLstOfIndexToGreenWave().get(j)] = LightState.GREEN;
        }

        TLState tlState = new TLState(lightStates);

        return tlState;
    }

    @Override
    public void accessMemoryObjects() {

        if (getTrafficLinkPhaseMO() == null)
            setTrafficLinkPhaseMO(this.getBroadcast(MemoryObjectName.TRAFFICLIGHT_LINKS_PHASE.toString()));

        if (getChangingPhaseMO() == null)
            setChangingPhaseMO(this.getOutput(MemoryObjectName.TRAFFICLIGHT_CHANGING_PHASE.toString()));


    }

    public synchronized MemoryObject getChangingPhaseMO() {
        return changingPhaseMO;
    }

    public synchronized void setChangingPhaseMO(MemoryObject changingPhaseMO) {
        this.changingPhaseMO = changingPhaseMO;
    }

    public synchronized MemoryObject getTrafficLinkPhaseMO() {
        return trafficLinkPhaseMO;
    }

    public synchronized void setTrafficLinkPhaseMO(MemoryObject trafficLinkPhaseMO) {
        this.trafficLinkPhaseMO = trafficLinkPhaseMO;
    }

    public List<Integer> getLstOfIndexToGreenWave() {
        return lstOfIndexToGreenWave;
    }

    public void setLstOfIndexToGreenWave(List<Integer> lstOfIndexToGreenWave) {
        this.lstOfIndexToGreenWave = lstOfIndexToGreenWave;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
}
