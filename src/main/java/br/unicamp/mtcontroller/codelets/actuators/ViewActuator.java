package br.unicamp.mtcontroller.codelets.actuators;

import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.CodeletsMonitor;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.mtcontroller.constants.MemoryObjectName;
import br.unicamp.mtcontroller.entity.TrafficLightLinkStatus;
import br.unicamp.sumo.view.AgentView;
import it.polito.appeal.traci.Lane;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Du on 27/02/16.
 */
public class ViewActuator extends Codelet {


    private AgentView ifAgent;
    private String sAgentID;

    private MemoryObject stateMO;
    private MemoryObject averageMaxVelocityMO;
    private MemoryObject averageOccupancystateMO;
    private MemoryObject sumVehiclesMO;
    private MemoryObject lstOfLinkPhaseMO;

    private ArrayList<JPanel> lstOfLight;
    private int timestamp = 100;


    public ViewActuator(JInternalFrame ifAgent, int timestamp, String sAgentID){
        this.setIfAgent(ifAgent);
        this.setsAgentID(sAgentID);
        this.getIfAgent().setTitle(sAgentID);

        setTimestamp(timestamp);
    }

    private synchronized void createComponentLight(List<TrafficLightLinkStatus> lstTrafficLightLinkStatus) {

        setLstOfLight(new ArrayList<JPanel>());

        while(getIfAgent().getPnlLight().getComponentCount() > 0)
            getIfAgent().getPnlLight().remove(0);


        for (TrafficLightLinkStatus trafficLightLinkStatus: lstTrafficLightLinkStatus) {

            JLabel lblLink = new JLabel();
            lblLink.setText(trafficLightLinkStatus.getControlledLink().getIncomingLane().getID()
                    // + " - " + trafficLightLinkStatus.getControlledLink().getAcrossLane().getID()
                    + " - " + trafficLightLinkStatus.getControlledLink().getOutgoingLane().getID());

            lblLink.setHorizontalTextPosition(JLabel.CENTER);

            getIfAgent().getPnlLight().add(lblLink);

            JPanel panel = new JPanel();
            panel.setName(String.valueOf(trafficLightLinkStatus.hashCode()));
            panel.setSize(new Dimension(32, 32));

            if(trafficLightLinkStatus.getPhase().isGreen()){
                panel.setBackground(Color.GREEN);
            }
            else if(trafficLightLinkStatus.getPhase().isRed()){
                panel.setBackground(Color.RED);
            }
            else {
                panel.setBackground(Color.YELLOW);
            }

            getLstOfLight().add(panel);
            getIfAgent().getPnlLight().add(panel);
            getIfAgent().getPnlLight().setVisible(true);

        }
    }

    @Override
    public void accessMemoryObjects() {

        if(getStateMO() == null)
            setStateMO(this.getInput(MemoryObjectName.STATE_AGENT.toString(), 0));

        if(getAverageMaxVelocityMO() == null)
            setAverageMaxVelocityMO(this.getInput(MemoryObjectName.AVERAGE_MEAN_VELOCITY.toString(), 0));

        if(getAverageOccupancystateMO() == null)
            setAverageOccupancystateMO(this.getInput(MemoryObjectName.AVERAGE_OCCUPANCY.toString(), 0));

        if(getSumVehiclesMO() == null)
            setSumVehiclesMO(this.getInput(MemoryObjectName.SUM_VEHICLES.toString(), 0));

        if(getLstOfLinkPhaseMO() == null)
            setLstOfLinkPhaseMO(this.getBroadcast(MemoryObjectName.TRAFFICLIGHT_LINKS_PHASE.toString()));

    }

    @Override
    public void calculateActivation() {


    }

    @Override
    public void proc() {
        synchronized (getIfAgent()) {

            if (getIfAgent().getChkUpdate().isSelected()) {

                if (getIfAgent().getLblNameValue().getText().equals("-"))
                    getIfAgent().getLblNameValue().setText(getsAgentID());

                if (getStateMO() != null) {
                    getIfAgent().getLblStateValue().setText(getStateMO().getI().toString());
                }
                if (getAverageMaxVelocityMO() != null) {
                    getIfAgent().getLblMeanVelocityValue().setText(String.format("%.2f", ((double) getAverageMaxVelocityMO().getI())));
                }
                if (getAverageOccupancystateMO() != null) {
                    getIfAgent().getLblOccupancyValue().setText(String.format("%.2f", (((double) getAverageOccupancystateMO().getI()) * 100)) + "%");
                }
                if (getSumVehiclesMO() != null) {
                    int numOfVehicles = 0;

                    getIfAgent().clearTable();

                    for (Map.Entry<Lane, List<String>> entry : ((Map<Lane, List<String>>) getSumVehiclesMO().getI()).entrySet()) {
                        numOfVehicles += entry.getValue().size();

                        DefaultTableModel model = (DefaultTableModel) getIfAgent().getTbVehicles().getModel();

                        for (String VehicleID : entry.getValue()) {
                            model.addRow(new Object[]{entry.getKey().getID(), VehicleID});
                        }
                    }

                    getIfAgent().getLblNumVehiclesValue().setText(String.valueOf(numOfVehicles));

                }

                if (getLstOfLinkPhaseMO() != null) {

                    if(getLstOfLight() == null){
                        this.createComponentLight((List<TrafficLightLinkStatus>) getLstOfLinkPhaseMO().getI());
                    }
                    else {
                        this.updateComponentLight((List<TrafficLightLinkStatus>) getLstOfLinkPhaseMO().getI());
                    }
                }

            }
        }

        try {
            Thread.sleep(getTimestamp()*5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized void updateComponentLight(List<TrafficLightLinkStatus> lstTrafficLightLinkStatus){

        for (int i = 0; i< lstTrafficLightLinkStatus.size(); i++) {
                if(lstTrafficLightLinkStatus.get(i).getPhase().isGreen()){
                    lstOfLight.get(i).setBackground(Color.GREEN);
                }
                else if(lstTrafficLightLinkStatus.get(i).getPhase().isRed()){
                    lstOfLight.get(i).setBackground(Color.RED);
                }
                else {
                    lstOfLight.get(i).setBackground(Color.YELLOW);
                }
        }
    }


    public AgentView getIfAgent() {
        return ifAgent;
    }

    public void setIfAgent(JInternalFrame ifAgent) {
        this.ifAgent = (AgentView) ifAgent;
    }

    public synchronized MemoryObject getStateMO() {
        return stateMO;
    }

    public void setStateMO(MemoryObject stateMO) {
        this.stateMO = stateMO;
    }

    public synchronized MemoryObject getAverageMaxVelocityMO() {
        return averageMaxVelocityMO;
    }

    public void setAverageMaxVelocityMO(MemoryObject averageMaxVelocityMO) {
        this.averageMaxVelocityMO = averageMaxVelocityMO;
    }

    public synchronized MemoryObject getAverageOccupancystateMO() {
        return averageOccupancystateMO;
    }

    public synchronized void setAverageOccupancystateMO(MemoryObject averageOccupancystateMO) {
        this.averageOccupancystateMO = averageOccupancystateMO;
    }

    public synchronized MemoryObject getSumVehiclesMO() {
        return sumVehiclesMO;
    }

    public synchronized void setSumVehiclesMO(MemoryObject sumVehiclesMO){
        this.sumVehiclesMO = sumVehiclesMO;
    }

    public String getsAgentID() {
        return sAgentID;
    }

    public void setsAgentID(String sAgentID) {
        this.sAgentID = sAgentID;
    }

    public synchronized MemoryObject getLstOfLinkPhaseMO() {
        return lstOfLinkPhaseMO;
    }

    public synchronized void setLstOfLinkPhaseMO(MemoryObject lstOfLinkPhaseMO) {
        this.lstOfLinkPhaseMO = lstOfLinkPhaseMO;
    }

    public ArrayList<JPanel> getLstOfLight() {
        return lstOfLight;
    }

    public void setLstOfLight(ArrayList<JPanel> lstOfLight) {
        this.lstOfLight = lstOfLight;
    }


    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
}
