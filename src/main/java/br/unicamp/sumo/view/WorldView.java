/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.unicamp.sumo.view;

import br.unicamp.cst.behavior.subsumption.SubsumptionAction;
import br.unicamp.cst.core.entities.Codelet;
import br.unicamp.cst.core.entities.MemoryObject;
import br.unicamp.cst.core.exceptions.CodeletActivationBoundsException;
import br.unicamp.cst.motivational.Drive;
import br.unicamp.cst.motivational.DriveLevel;
import br.unicamp.cst.motivational.Goal;
import br.unicamp.cst.motivational.GoalArchitecture;
import br.unicamp.mtcontroller.agents.AgentMind;
import br.unicamp.mtcontroller.codelets.actuators.TrafficLightActuator;
import br.unicamp.mtcontroller.codelets.actuators.ViewActuator;
import br.unicamp.mtcontroller.codelets.motivational.drives.TrafficDrive;
import br.unicamp.mtcontroller.codelets.motivational.goals.LaneHelpGoal;
import br.unicamp.mtcontroller.codelets.sensors.LaneSensor;
import br.unicamp.mtcontroller.codelets.sensors.TrafficLightSensor;
import br.unicamp.mtcontroller.codelets.state.StateSensor;
import br.unicamp.mtcontroller.comunication.CommunicationAgent;
import br.unicamp.mtcontroller.comunication.CommunicationServer;
import br.unicamp.mtcontroller.comunication.SingleAccessQuery;
import br.unicamp.mtcontroller.constants.MemoryObjectName;
import br.unicamp.mtcontroller.entity.TrafficLightLinkStatus;
import it.polito.appeal.traci.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Du
 */
public class WorldView extends javax.swing.JFrame {

    private JFileChooser fileChooser;
    private File selectedFile;
    private Thread sumoThread;
    private List<AgentMind> agentMinds;
    private SumoTraciConnection conn;
    private static Process sumoProcess;
    private int openFrameCount = 0;
    private CommunicationServer server;
    private int sumoPort = 8091;
    private int serverPort = 4011;


    /**
     * Creates new form World
     */
    public WorldView() {

        super("SUMO World View");
        initComponents();

    }

    public void finishApplication() {
        this.dispose();
    }

    public void clearFields() {

        btnStop.setEnabled(false);
        btnPause.setEnabled(false);
        btnPlay.setEnabled(false);
        selectedFile = null;
        lblFileValue.setText("-");
        lblVehiclesNumValue.setText("-");
        txtStep.setText("0");
        txtTimeStep.setText("200");
        txtTimeStep.setEnabled(true);
        txtTotalSteps.setText("1000");
        txtTotalSteps.setEnabled(true);
    }

    public void pauseEffect() {
        btnPlay.setEnabled(true);
        btnPause.setEnabled(false);
        btnStop.setEnabled(true);
        txtTimeStep.setEnabled(false);
        txtTotalSteps.setEnabled(false);

    }

    public void resumeEffect() {
        btnPlay.setEnabled(false);
        btnPause.setEnabled(true);
        btnStop.setEnabled(true);
        txtTimeStep.setEnabled(false);
        txtTotalSteps.setEnabled(false);
    }


    public void initGraphic(String title, String x, String y, XYDataset dataSet) {
        JFreeChart chart = ChartFactory.createXYLineChart(title, x, y, dataSet);

        XYPlot plot = (XYPlot) chart.getPlot();

        plot.setBackgroundPaint(Color.lightGray);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        //renderer.setShapesVisible(true);
        //renderer.setShapesFilled(true);

        ChartPanel panel = new ChartPanel(chart);
        pnlLog.setLayout(new java.awt.BorderLayout());
        pnlLog.add(panel, BorderLayout.CENTER);
        pnlLog.validate();

    }


    public synchronized void startSumoThread() {
        sumoThread = new Thread() {
            public void run() {
                try {


                    String[] sSumoArgs = new String[]{
                            "sumo-gui",
                            "-c", selectedFile.getAbsolutePath(),
                            "--remote-port", Integer.toString(getSumoPort()),
                            "-S"
                    };

                    int iSteps = getTotalSteps() <= 0 ? 1000 : getTotalSteps();
                    int iTimeStep = getTimeStep() <= 0 ? 200 : getTimeStep();

                    sumoProcess = Runtime.getRuntime().exec(sSumoArgs);

                    conn = new SumoTraciConnection(InetAddress.getByName("127.0.0.1"), getSumoPort());
                    SingleAccessQuery.setConnection(conn);


                    setServer(new CommunicationServer("CommunicationServer", getServerPort()));
                    getServer().startServer();

                    initTrafficAgent();

                    getServer().setEndConnections(true);


                    XYSeriesCollection dataset = new XYSeriesCollection();

                    dataset.addSeries(new XYSeries("Vehicles Number"));

                    initGraphic("Vehicles on Map", "Time", "Vehicles Number", dataset);

                    double initialTime = Calendar.getInstance().getTimeInMillis();


                    for (int i = 0; i < iSteps; i++) {

                        SingleAccessQuery.nextSimStep();

                        setCurrentStep(String.valueOf(i));

                        Collection<Vehicle> vehicles = SingleAccessQuery.getNumOfVehicles();

                        setCurrentStep(String.valueOf(i));
                        setTotalOfVehicles(String.valueOf(vehicles.size()));


                        if (i % 10 == 0) {
                            double instant = Calendar.getInstance().getTimeInMillis() - initialTime;

                            dataset.getSeries("Vehicles Number").add(instant, vehicles.size());
                        }


                        if (i == 0) {
                            for (int j = 0; j < getAgentMinds().size(); j++) {
                                getAgentMinds().get(j).start();
                            }
                        }

                        Thread.sleep(iTimeStep);
                    }

                    SingleAccessQuery.closeConnection();

                    for (int j = 0; j < getAgentMinds().size(); j++) {
                        getAgentMinds().get(j).shutDown();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };

        sumoThread.start();
    }

    private void findNeighbors() {

        getAgentMinds().forEach(agentMind -> {
            HashMap<String, List<Lane>> agentsLane = new HashMap<>();

            getAgentMinds().stream().filter(agent -> !agent.getName().equals(agentMind.getName())).collect(Collectors.toList())
                    .forEach(agentDiff -> agentsLane.put(agentDiff.getName(), agentDiff.getLstOfOutgoingLanes()));

            agentMind.findNeighborhood(agentsLane);
        });


    }

    public List<Integer> findLaneToGreenWave(ControlledLink[][] controlledLinks, Lane incomingLane) {

        Map<String, ControlledLink> mapOfLinkToIncomingLane = new HashMap<String, ControlledLink>();
        Map<String, ControlledLink> mapOfLinkToOthersLane = new HashMap<String, ControlledLink>();
        List<Integer> indexList = Collections.synchronizedList(new ArrayList<Integer>());


        for (int i = 0; i < controlledLinks.length; i++) {
            for (int j = 0; j < controlledLinks[i].length; j++) {

                if (controlledLinks[i][j].getIncomingLane().getID().equals(incomingLane.getID())) {
                    mapOfLinkToIncomingLane.put(String.valueOf(i), controlledLinks[i][j]);
                } else {
                    mapOfLinkToOthersLane.put(String.valueOf(i), controlledLinks[i][j]);
                }
            }
        }


        int iCount = 0;

        for (Map.Entry<String, ControlledLink> linkToOthersLane : mapOfLinkToOthersLane.entrySet()) {

            iCount = 0;

            for (Map.Entry<String, ControlledLink> linkToIncomingLane : mapOfLinkToIncomingLane.entrySet()) {
                try {
                    if (!linkToIncomingLane.getValue().getOutgoingLane().getParentEdge().equals(linkToOthersLane.getValue().getOutgoingLane().getParentEdge())) {
                        iCount++;
                    } else {
                        if (linkToIncomingLane.getValue().getIncomingLane().getParentEdge().equals(linkToOthersLane.getValue().getIncomingLane().getParentEdge())) {
                            iCount++;
                        } else
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (iCount == mapOfLinkToIncomingLane.size())
                indexList.add(new Integer(linkToOthersLane.getKey()));
        }

        return indexList;
    }

    public JInternalFrame createAgentView(List<Drive> lstOfDrives, List<Goal> lstOfGoals) {

        List<Codelet> codeletDrives = Collections.synchronizedList(new ArrayList<Codelet>());
        List<Codelet> codeletGoals = Collections.synchronizedList(new ArrayList<Codelet>());

        lstOfDrives.forEach(d -> codeletDrives.add(d));
        lstOfGoals.forEach(d -> codeletGoals.add(d));

        AgentView agentView = new AgentView(openFrameCount++, getTimeStep(), codeletDrives, codeletGoals);
        this.dpnAgents.add(agentView);
        return agentView;
    }

    public synchronized int getTotalSteps() {
        return Integer.parseInt(this.txtTotalSteps.getText().trim().equals("") ? "10000" : this.txtTotalSteps.getText());
    }

    public synchronized int getTimeStep() {
        return Integer.parseInt(this.txtTimeStep.getText().trim().equals("") ? "200" : this.txtTimeStep.getText());
    }

    public synchronized void setCurrentStep(String sStep) {
        this.txtStep.setText(sStep);
    }

    public synchronized void setTotalOfVehicles(String sTotalVehicles) {
        this.lblVehiclesNumValue.setText(sTotalVehicles);
    }


    //This method is resposible for init agent in the world.
    public void initTrafficAgent() throws IOException {
        try {

            Map<String, TrafficLight> mapTrafficLights = SingleAccessQuery.getSumoTraciConnection().getTrafficLightRepository().getAll();

            setAgentMinds(Collections.synchronizedList(new ArrayList<AgentMind>()));

            for (Map.Entry<String, TrafficLight> trafficLightPairs : mapTrafficLights.entrySet()) {

                TrafficLight trafficLight = trafficLightPairs.getValue();

                AgentMind agent = new AgentMind(trafficLight.getID());

                List<Drive> lstOfDrives = Collections.synchronizedList(new ArrayList<>());
                List<Goal> lstOfGoals = Collections.synchronizedList(new ArrayList<>());
                List<Lane> incomingLanes = Collections.synchronizedList(new ArrayList<>());
                List<Lane> outgoingLanes = Collections.synchronizedList(new ArrayList<>());

                Map<Lane, MemoryObject> mapOfOccupancyMO = new HashMap<>();
                Map<Lane, MemoryObject> mapOfMeanVelocityMO = new HashMap<>();
                Map<Lane, MemoryObject> mapOfMaxVelocityMO = new HashMap<>();
                Map<Lane, MemoryObject> mapOfVehiclesMO = new HashMap<>();
                List<ControlledLink> lstOfControlledLink = Collections.synchronizedList(new ArrayList<>());


                ControlledLink[][] links = trafficLight.queryReadControlledLinks().get().getLinks();

                /*
                    Init Traffic Light Sensor.
                 */
                List<TLState> trafficLightStatus = Collections.synchronizedList(new ArrayList<TLState>());
                Logic[] logics = trafficLight.queryReadCompleteDefinition().get().getLogics();

                for (Logic logic : logics) {
                    Phase[] phases = logic.getPhases();
                    for (Phase phase : phases) {
                        trafficLightStatus.add(phase.getState());

                    }
                }


                Codelet trafficLightSensor = new TrafficLightSensor(trafficLight, getTimeStep(), lstOfControlledLink);
                trafficLightSensor.addOutput(agent.createMemoryObject(MemoryObjectName.TRAFFICLIGHT_PROGRAM_PHASES.toString()));

                MemoryObject trafficLinkPhaseMO = agent.createMemoryObject(MemoryObjectName.TRAFFICLIGHT_LINKS_PHASE.toString(), Collections.synchronizedList(new ArrayList<TrafficLightLinkStatus>()));

                MemoryObject changeTrafficLightPhaseMO = agent.createMemoryObject(MemoryObjectName.TRAFFICLIGHT_MEMORY_CHANGED.toString());


                trafficLightSensor.addOutput(trafficLinkPhaseMO);
                trafficLightSensor.addBroadcast(changeTrafficLightPhaseMO);
                agent.insertCodelet(trafficLightSensor);

                CommunicationAgent communicationAgent = new CommunicationAgent(trafficLight.getID(), 4011);
                agent.setCommunicationAgent(communicationAgent);


                for (int i = 0; i < links.length; i++) {
                    for (int j = 0; j < links[i].length; j++) {
                        Lane incomingLane = links[i][j].getIncomingLane();
                        Lane outgoingLane = links[i][j].getOutgoingLane();

                        if (!outgoingLanes.contains(outgoingLane))
                            outgoingLanes.add(outgoingLane);

                        if (!incomingLanes.contains(incomingLane)) {
                            incomingLanes.add(incomingLane);

                            /*
                                Init Lane Sensor.
                             */
                            Codelet laneSensorInput = new LaneSensor(incomingLane, getTimeStep());

                            MemoryObject occupancyMO = agent.createMemoryObject(MemoryObjectName.LANE_OCCUPANCY.toString(), 0);
                            MemoryObject vehiclesMO = agent.createMemoryObject(MemoryObjectName.LANE_VEHICLES_ID_LIST.toString(), new HashMap<Lane, List<String>>());
                            MemoryObject meanVelocityMO = agent.createMemoryObject(MemoryObjectName.LANE_MEAN_VELOCITY.toString(), 0);
                            MemoryObject maxVelocityMO = agent.createMemoryObject(MemoryObjectName.LANE_MAX_VELOCITY.toString(), 0);

                            laneSensorInput.addOutput(occupancyMO);
                            laneSensorInput.addOutput(vehiclesMO);
                            laneSensorInput.addOutput(meanVelocityMO);
                            laneSensorInput.addOutput(maxVelocityMO);

                            agent.insertCodelet(laneSensorInput);

                            mapOfMeanVelocityMO.put(incomingLane, meanVelocityMO);
                            mapOfOccupancyMO.put(incomingLane, occupancyMO);
                            mapOfVehiclesMO.put(incomingLane, vehiclesMO);
                            mapOfMaxVelocityMO.put(incomingLane, maxVelocityMO);

                            /*
                                Init Traffic Drive for Lanes.
                            */
                            Drive trafficDrive = new TrafficDrive(incomingLane.getID(), DriveLevel.LOW_LEVEL, 0.7d, 0.9d);
                            trafficDrive.addInputs(laneSensorInput.getOutputs());
                            agent.insertCodelet(trafficDrive);

                            lstOfDrives.add(trafficDrive);

                            /*
                                Init Lane Goals.
                             */
                            List<Drive> lstOfDriveLaneGoal = Collections.synchronizedList(new ArrayList<>());
                            lstOfDriveLaneGoal.add(trafficDrive);

                            List<Integer> lstOfIndexGreenWave = findLaneToGreenWave(links, incomingLane);

                            Goal goalLane = new LaneHelpGoal(incomingLane.getID(), getTimeStep(), 80, 30, 0.75d, 0.0d, 0.7d, lstOfIndexGreenWave);

                            goalLane.addOutput(agent.createMemoryObject(MemoryObjectName.TRAFFICLIGHT_CHANGING_PHASE.toString(), "0"));
                            goalLane.addBroadcast(trafficLinkPhaseMO);

                            MemoryObject drivesLaneGoal = agent.createMemoryObject(Goal.DRIVES_VOTE_MEMORY, 0d);
                            drivesLaneGoal.setI(lstOfDriveLaneGoal);

                            goalLane.addInput(drivesLaneGoal);

                            SubsumptionAction trafficLightActuator = new TrafficLightActuator(trafficLight, getTimeStep(), agent.getSubsumptionArchitecture());
                            trafficLightActuator.addOutput(changeTrafficLightPhaseMO);
                            goalLane.addSubsumptionAction(trafficLightActuator, goalLane);

                            lstOfGoals.add(goalLane);

                            agent.getSubsumptionArchitecture().addLayer(goalLane.getSubsumptionBehaviourLayer());

                            agent.insertCodelet(goalLane);

                        }

                        lstOfControlledLink.add(links[i][j]);

                    }
                }

                agent.setLstOfIncomingLanes(incomingLanes);
                agent.setLstOfOutgoingLanes(outgoingLanes);


                /*
                    Init State Sensor.
                 */
                Map<String, Object> mapMOs = new HashMap<>();
                mapMOs.put("occupancy", mapOfOccupancyMO);
                mapMOs.put("meanvelocity", mapOfMeanVelocityMO);
                mapMOs.put("vehicles", mapOfVehiclesMO);
                mapMOs.put("maxvelocity", mapOfMaxVelocityMO);


                Codelet stateSensor = new StateSensor(trafficLight.getID(), getTimeStep());

                MemoryObject lanesStateMO = agent.createMemoryObject(MemoryObjectName.LANES_STATE.toString(), new HashMap<Lane, String>());
                stateSensor.addOutput(lanesStateMO);

                MemoryObject stateAgentMO = agent.createMemoryObject(MemoryObjectName.STATE_AGENT.toString(), "NORMAL");
                stateSensor.addOutput(stateAgentMO);

                MemoryObject averageMaxVelocityMO = agent.createMemoryObject(MemoryObjectName.AVERAGE_MEAN_VELOCITY.toString(), 0d);
                stateSensor.addOutput(averageMaxVelocityMO);

                MemoryObject averageOccupancyMO = agent.createMemoryObject(MemoryObjectName.AVERAGE_OCCUPANCY.toString(), 0d);
                stateSensor.addOutput(averageOccupancyMO);

                MemoryObject sumVehiclesNumMO = agent.createMemoryObject(MemoryObjectName.SUM_VEHICLES.toString(), new HashMap<Lane, List<String>>());
                stateSensor.addOutput(sumVehiclesNumMO);

                stateSensor.addInput(agent.createMemoryObject(MemoryObjectName.ALL_MOS.toString(), mapMOs));

                agent.insertCodelet(stateSensor);


                Random random = new Random();
                random.ints(0, lstOfGoals.size() - 1);

                lstOfGoals.get(random.nextInt((((lstOfGoals.size() - 1) - 0) + 1))).setActivation(0.7);

                /*
                    Init View Actuator.
                 */
                JInternalFrame ifAgent = createAgentView(lstOfDrives, lstOfGoals);
                Codelet viewActuator = new ViewActuator(ifAgent, getTimeStep(), trafficLight.getID());
                viewActuator.addInput(stateAgentMO);
                viewActuator.addInput(averageMaxVelocityMO);
                viewActuator.addInput(averageOccupancyMO);
                viewActuator.addInput(sumVehiclesNumMO);
                viewActuator.addBroadcast(trafficLinkPhaseMO);
                agent.insertCodelet(viewActuator);



                GoalArchitecture goalArchitecture = new GoalArchitecture();
                MemoryObject drives = agent.createMemoryObject(GoalArchitecture.DRIVES_MEMORY, 0d);
                drives.setI(lstOfDrives);
                MemoryObject goals = agent.createMemoryObject(GoalArchitecture.GOALS_MEMORY, 0d);
                goals.setI(lstOfGoals);

                goalArchitecture.addInput(drives);
                goalArchitecture.addInput(goals);

                agent.setGoalArchitecture(goalArchitecture);
                agent.insertCodelet(agent.getGoalArchitecture());



                getAgentMinds().add(agent);

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CodeletActivationBoundsException e) {
            e.printStackTrace();
        }

        findNeighbors();

    }


    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuBar2 = new javax.swing.JMenuBar();
        jMenu3 = new javax.swing.JMenu();
        jMenu4 = new javax.swing.JMenu();
        jMenuBar3 = new javax.swing.JMenuBar();
        jMenu5 = new javax.swing.JMenu();
        jMenu6 = new javax.swing.JMenu();
        pnlMain = new javax.swing.JPanel();
        spnDivisor = new javax.swing.JSplitPane();
        spnLogs = new javax.swing.JScrollPane();
        pnlLog = new javax.swing.JPanel();
        pnlAgents = new javax.swing.JPanel();
        spnAgents = new javax.swing.JScrollPane();
        dpnAgents = new javax.swing.JDesktopPane();
        tbrBar = new javax.swing.JToolBar();
        lblFile = new javax.swing.JLabel();
        lblFileValue = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        lblTotalVehiclesNumber = new javax.swing.JLabel();
        lblVehiclesNumValue = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        btnPlay = new javax.swing.JButton();
        btnStop = new javax.swing.JButton();
        lblStep = new javax.swing.JLabel();
        txtStep = new javax.swing.JTextField();
        lblTotalSteps = new javax.swing.JLabel();
        txtTotalSteps = new javax.swing.JTextField();
        lblTimeStep = new javax.swing.JLabel();
        txtTimeStep = new javax.swing.JTextField();
        btnPause = new javax.swing.JButton();
        jmbMenu = new javax.swing.JMenuBar();
        jmMenu = new javax.swing.JMenu();
        jmiOpen = new javax.swing.JMenuItem();
        jmiExit = new javax.swing.JMenuItem();

        jMenu3.setText("File");
        jMenuBar2.add(jMenu3);

        jMenu4.setText("Edit");
        jMenuBar2.add(jMenu4);

        jMenu5.setText("File");
        jMenuBar3.add(jMenu5);

        jMenu6.setText("Edit");
        jMenuBar3.add(jMenu6);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        spnDivisor.setDividerLocation(300);
        spnDivisor.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        javax.swing.GroupLayout pnlLogLayout = new javax.swing.GroupLayout(pnlLog);
        pnlLog.setLayout(pnlLogLayout);
        pnlLogLayout.setHorizontalGroup(
                pnlLogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 930, Short.MAX_VALUE)
        );
        pnlLogLayout.setVerticalGroup(
                pnlLogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 270, Short.MAX_VALUE)
        );

        spnLogs.setViewportView(pnlLog);
        pnlLog.getAccessibleContext().setAccessibleName("pnlLog");

        spnDivisor.setBottomComponent(spnLogs);

        pnlAgents.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        pnlAgents.setToolTipText("Agents");
        pnlAgents.setName("Agents"); // NOI18N

        dpnAgents.setAutoscrolls(true);
        dpnAgents.setDragMode(javax.swing.JDesktopPane.OUTLINE_DRAG_MODE);
        dpnAgents.setLayout(null);
        spnAgents.setViewportView(dpnAgents);

        javax.swing.GroupLayout pnlAgentsLayout = new javax.swing.GroupLayout(pnlAgents);
        pnlAgents.setLayout(pnlAgentsLayout);
        pnlAgentsLayout.setHorizontalGroup(
                pnlAgentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(spnAgents, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        pnlAgentsLayout.setVerticalGroup(
                pnlAgentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(spnAgents, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 297, Short.MAX_VALUE)
        );

        spnDivisor.setLeftComponent(pnlAgents);

        tbrBar.setFloatable(false);
        tbrBar.setRollover(true);

        lblFile.setText("File Loaded:");
        tbrBar.add(lblFile);

        lblFileValue.setText("-");
        tbrBar.add(lblFileValue);
        tbrBar.add(jSeparator1);

        lblTotalVehiclesNumber.setText("Total Vehicles Number:");
        tbrBar.add(lblTotalVehiclesNumber);

        lblVehiclesNumValue.setText("-");
        tbrBar.add(lblVehiclesNumValue);

        btnPlay.setText("Play");
        btnPlay.setEnabled(false);
        btnPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPlayActionPerformed(evt);
            }
        });

        btnStop.setText("Stop");
        btnStop.setEnabled(false);
        btnStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopActionPerformed(evt);
            }
        });

        lblStep.setText("Step:");

        txtStep.setText("0");
        txtStep.setToolTipText("");
        txtStep.setEnabled(false);

        lblTotalSteps.setText("Total Steps:");

        txtTotalSteps.setText("1000");
        txtTotalSteps.setToolTipText("");

        lblTimeStep.setText("Time Step:");

        txtTimeStep.setText("100");


        btnPause.setText("Pause");
        btnPause.setEnabled(false);
        btnPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPauseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(btnPlay)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnStop)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnPause)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblStep, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtStep)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblTotalSteps)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtTotalSteps)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblTimeStep)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(txtTimeStep)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(btnPlay, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnStop, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblTimeStep)
                                        .addComponent(txtTimeStep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtTotalSteps, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblTotalSteps)
                                        .addComponent(txtStep, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblStep)))
                        .addComponent(btnPause, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout pnlMainLayout = new javax.swing.GroupLayout(pnlMain);
        pnlMain.setLayout(pnlMainLayout);
        pnlMainLayout.setHorizontalGroup(
                pnlMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlMainLayout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(spnDivisor)
                                .addContainerGap())
                        .addComponent(tbrBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        pnlMainLayout.setVerticalGroup(
                pnlMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(pnlMainLayout.createSequentialGroup()
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spnDivisor, javax.swing.GroupLayout.DEFAULT_SIZE, 585, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(tbrBar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jmMenu.setText("File");

        jmiOpen.setText("Open Map");
        jmiOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmiOpenActionPerformed(evt);
            }
        });
        jmMenu.add(jmiOpen);

        jmiExit.setText("Exit");
        jmiExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmiExitActionPerformed(evt);
            }
        });
        jmMenu.add(jmiExit);

        jmbMenu.add(jmMenu);

        setJMenuBar(jmbMenu);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(pnlMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(pnlMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnStopActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStopActionPerformed
        sumoThread.stop();
        sumoThread.destroy();
        sumoProcess.exitValue();
        sumoProcess.destroy();
        clearFields();
    }//GEN-LAST:event_btnStopActionPerformed

    private void jmiOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmiOpenActionPerformed
        if (selectedFile == null) {

            fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Sumo Configuration File", "sumocfg", "sumo.cfg"));

            int result = fileChooser.showOpenDialog(getParent());
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                btnPlay.setEnabled(true);
                lblFileValue.setText(selectedFile.getAbsolutePath());
            }
        } else {
            JOptionPane.showMessageDialog(getParent(), "You can't choose another file when program running.", "Alert", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jmiOpenActionPerformed

    private void btnPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPlayActionPerformed
        if (sumoThread == null) {
            startSumoThread();
        } else {
            sumoThread.notify();
            getAgentMinds().notify();
        }
        resumeEffect();
    }//GEN-LAST:event_btnPlayActionPerformed

    private void btnPauseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPauseActionPerformed
        sumoThread.stop();
        sumoThread.destroy();
        sumoProcess.exitValue();
        sumoProcess.destroy();
        clearFields();
    }//GEN-LAST:event_btnPauseActionPerformed

    private void jmiExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmiExitActionPerformed
        this.dispose();
    }//GEN-LAST:event_jmiExitActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Macintosh".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(WorldView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(WorldView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(WorldView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(WorldView.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new WorldView().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnPause;
    private javax.swing.JButton btnPlay;
    private javax.swing.JButton btnStop;
    private javax.swing.JDesktopPane dpnAgents;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenuBar jMenuBar2;
    private javax.swing.JMenuBar jMenuBar3;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JMenu jmMenu;
    private javax.swing.JMenuBar jmbMenu;
    private javax.swing.JMenuItem jmiExit;
    private javax.swing.JMenuItem jmiOpen;
    private javax.swing.JLabel lblFile;
    private javax.swing.JLabel lblFileValue;
    private javax.swing.JLabel lblStep;
    private javax.swing.JLabel lblTimeStep;
    private javax.swing.JLabel lblTotalSteps;
    private javax.swing.JLabel lblTotalVehiclesNumber;
    private javax.swing.JLabel lblVehiclesNumValue;
    private javax.swing.JPanel pnlAgents;
    private javax.swing.JPanel pnlLog;
    private javax.swing.JPanel pnlMain;
    private javax.swing.JScrollPane spnAgents;
    private javax.swing.JSplitPane spnDivisor;
    private javax.swing.JScrollPane spnLogs;
    private javax.swing.JToolBar tbrBar;
    private javax.swing.JTextField txtStep;
    private javax.swing.JTextField txtTimeStep;
    private javax.swing.JTextField txtTotalSteps;
    // End of variables declaration//GEN-END:variables

    /**
     * @return the agentMinds
     */
    public List<AgentMind> getAgentMinds() {
        return agentMinds;
    }

    /**
     * @param agentMinds the agentMinds to set
     */
    public void setAgentMinds(List<AgentMind> agentMinds) {
        this.agentMinds = agentMinds;
    }

    /**
     * @return the server
     */
    public CommunicationServer getServer() {
        return server;
    }

    /**
     * @param server the server to set
     */
    public void setServer(CommunicationServer server) {
        this.server = server;
    }

    public int getSumoPort() {
        return sumoPort;
    }

    public void setSumoPort(int sumoPort) {
        this.sumoPort = sumoPort;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }
}
