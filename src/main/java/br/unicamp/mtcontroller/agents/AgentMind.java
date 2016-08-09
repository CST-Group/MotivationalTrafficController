package br.unicamp.mtcontroller.agents;


import br.unicamp.cst.behavior.subsumption.SubsumptionArchitecture;
import br.unicamp.cst.core.entities.Mind;
import br.unicamp.cst.motivational.GoalArchitecture;
import br.unicamp.mtcontroller.comunication.CommunicationAgent;
import it.polito.appeal.traci.Lane;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Du on 16/12/15.
 */
public class AgentMind extends Mind {

    //Variables and Objects
    private String name;
    private SubsumptionArchitecture subsumptionArchitecture;
    private GoalArchitecture goalArchitecture;
    private List<Lane> lstOfIncomingLanes;
    private List<Lane> lstOfOutgoingLanes;
    private Map<String, String> mapAgentsNeighbor;
    private CommunicationAgent communicationAgent;


    /**
     * Default Constructor
     */
    public AgentMind(String name) {
        this.setName(name);

        setSubsumptionArchitecture(new SubsumptionArchitecture(this));

        setMapAgentsNeighbor(new HashMap<String, String>());


    }

    public void findNeighborhood(HashMap<String, List<Lane>> agentsLanes) {

        for (Lane incomingLane : getLstOfIncomingLanes()) {

            for (Map.Entry<String, List<Lane>> agent : agentsLanes.entrySet()) {
                if (agent.getValue().stream().filter(x -> x.getID() == incomingLane.getID()).findFirst() != null) {
                    if(getMapAgentsNeighbor().entrySet().stream().filter(x->x.getKey() == incomingLane.getID()).findFirst() == null)
                        getMapAgentsNeighbor().put(incomingLane.getID(), agent.getKey());
                }
            }

        }

    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SubsumptionArchitecture getSubsumptionArchitecture() {
        return subsumptionArchitecture;
    }

    public void setSubsumptionArchitecture(SubsumptionArchitecture subsumptionArchitecture) {
        this.subsumptionArchitecture = subsumptionArchitecture;
    }

    public GoalArchitecture getGoalArchitecture() {
        return goalArchitecture;
    }

    public void setGoalArchitecture(GoalArchitecture goalArchitecture) {
        this.goalArchitecture = goalArchitecture;
    }

    public List<Lane> getLstOfIncomingLanes() {
        return lstOfIncomingLanes;
    }

    public void setLstOfIncomingLanes(List<Lane> lstOfIncomingLanes) {
        this.lstOfIncomingLanes = lstOfIncomingLanes;
    }

    public List<Lane> getLstOfOutgoingLanes() {
        return lstOfOutgoingLanes;
    }

    public void setLstOfOutgoingLanes(List<Lane> lstOfOutgoingLanes) {
        this.lstOfOutgoingLanes = lstOfOutgoingLanes;
    }

    public Map<String, String> getMapAgentsNeighbor() {
        return mapAgentsNeighbor;
    }

    public void setMapAgentsNeighbor(Map<String, String> mapAgentsNeighbor) {
        this.mapAgentsNeighbor = mapAgentsNeighbor;
    }

    public CommunicationAgent getCommunicationAgent() {
        return communicationAgent;
    }

    public void setCommunicationAgent(CommunicationAgent communicationAgent) {
        this.communicationAgent = communicationAgent;
    }
}
