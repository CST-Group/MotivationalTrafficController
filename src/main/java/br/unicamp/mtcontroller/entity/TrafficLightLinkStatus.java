package br.unicamp.mtcontroller.entity;

import it.polito.appeal.traci.ControlledLink;
import it.polito.appeal.traci.LightState;

/**
 * Created by Du on 01/03/16.
 */
public class TrafficLightLinkStatus {

    private LightState phase;
    private ControlledLink controlledLink;


    public TrafficLightLinkStatus(LightState phase, ControlledLink controlledLink){
        setPhase(phase);
        setControlledLink(controlledLink);
    }


    public synchronized LightState getPhase() {
        return phase;
    }

    public synchronized void setPhase(LightState phase) {
        this.phase = phase;
    }

    public synchronized ControlledLink getControlledLink() {
        return controlledLink;
    }

    public synchronized void setControlledLink(ControlledLink controlledLink) {
        this.controlledLink = controlledLink;
    }
}
