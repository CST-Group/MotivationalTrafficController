package br.unicamp.mtcontroller.codelets.actuators;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.mtcontroller.comunication.CommunicationAgent;
import br.unicamp.mtcontroller.constants.MemoryObjectName;

/**
 * Created by Du on 04/04/16.
 */
public class CommunicationActuator extends Codelet {



    private MemoryObject lanesStateMO;

    private CommunicationAgent communicationAgent;

    public CommunicationActuator(String name, CommunicationAgent communicationAgent){
        this.setName(name);
        this.setCommunicationAgent(communicationAgent);
    }

    @Override
    public void accessMemoryObjects() {

        if(getLanesStateMO() ==null)
            setLanesStateMO(this.getInput(MemoryObjectName.LANES_STATE.toString()));

    }

    @Override
    public void calculateActivation() {

    }

    @Override
    public void proc() {

    }



    public CommunicationAgent getCommunicationAgent() {
        return communicationAgent;
    }

    public void setCommunicationAgent(CommunicationAgent communicationAgent) {
        this.communicationAgent = communicationAgent;
    }

    public MemoryObject getLanesStateMO() {
        return lanesStateMO;
    }

    public void setLanesStateMO(MemoryObject lanesStateMO) {
        this.lanesStateMO = lanesStateMO;
    }
}
