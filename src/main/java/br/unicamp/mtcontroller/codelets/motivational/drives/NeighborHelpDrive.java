package br.unicamp.mtcontroller.codelets.motivational.drives;

import br.unicamp.cst.core.exceptions.CodeletActivationBoundsException;
import br.unicamp.cst.motivational.Drive;
import br.unicamp.cst.motivational.DriveLevel;
import br.unicamp.mtcontroller.comunication.CommunicationAgent;
import br.unicamp.mtcontroller.comunication.Message;

/**
 * Created by Du on 04/04/16.
 */
public class NeighborHelpDrive extends Drive {


    private CommunicationAgent communicationAgent;
    private Message messageAct;

    public NeighborHelpDrive(String name, DriveLevel level, double priority, double relevance, CommunicationAgent communicationAgent) throws CodeletActivationBoundsException {
        super(name, level, priority, relevance);
        this.setCommunicationAgent(communicationAgent);
    }

    @Override
    public double calculateSimpleActivation() {

        Message message = getCommunicationAgent().readCurrentMessage();

        if(message != null)
        {

        }

        return 0;
    }

    @Override
    public double calculateSecundaryDriveActivation() {
        return 0;
    }

    @Override
    public void accessMemoryObjects() {

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

    public Message getMessageAct() {
        return messageAct;
    }

    public void setMessageAct(Message messageAct) {
        this.messageAct = messageAct;
    }
}
