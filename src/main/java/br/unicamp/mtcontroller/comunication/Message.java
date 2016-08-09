package br.unicamp.mtcontroller.comunication;

/**
 * Created by Du on 05/04/16.
 */
public class Message {

    private String agentIdSender;
    private String agentIdReceiver;
    private String value;

    public Message(String agentIdSender, String agentIdReceiver, String value){
        this.setAgentIdSender(agentIdSender);
        this.setAgentIdReceiver(agentIdReceiver);
        this.setValue(value);
    }


    public String getAgentIdSender() {
        return agentIdSender;
    }

    public void setAgentIdSender(String agentIdSender) {
        this.agentIdSender = agentIdSender;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAgentIdReceiver() {
        return agentIdReceiver;
    }

    public void setAgentIdReceiver(String agentIdReceiver) {
        this.agentIdReceiver = agentIdReceiver;
    }
}
