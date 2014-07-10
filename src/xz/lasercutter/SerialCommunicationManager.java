package xz.lasercutter;

import java.io.*;
import java.util.Enumeration;

import gnu.io.*;

public class SerialCommunicationManager {
	
	public static final int CONNECTION_STATE_CONNECTED 		= 0;
	public static final int CONNECTION_STATE_DISCONNECTED 	= 1;
	public static final int CONNECTION_STATE_CONNECTING 	= 2;
	public static final int CONNECTION_STATE_DISCONNECTING 	= 3;
	public static final int CONNECTION_STATE_BUSY			= 4;
	public static final int CONNECTION_STATE_PAUSED			= 5;
	
	private static final int TIME_OUT = 2000;
	private static final int DATA_RATE = 9600;
	
	private static SerialPort serialPort;
	//private static String portName = "/dev/ttyUSB0";
	
	private static BufferedReader input;
	private static OutputStream output;
	
	private static int connectionState = CONNECTION_STATE_DISCONNECTED;
	
	private static SerialPortEventListener serialPortEventListener = new MySerialPortEventListener();
	private static BufferedReader commandReader;
	
	private static void setConnectionState(int state) {
		switch (state) {
		case CONNECTION_STATE_CONNECTED:
			if (connectionState == CONNECTION_STATE_BUSY)
				MainWindow.log("SYSTEM\t|File Sent Successfully.");
			else
				MainWindow.log("SYSTEM\t|Connected.");
			break;
		case CONNECTION_STATE_DISCONNECTED:
			MainWindow.log("SYSTEM\t|Disconnected.");
			break;
		case CONNECTION_STATE_CONNECTING:
			break;
		case CONNECTION_STATE_DISCONNECTING:
			break;
		case CONNECTION_STATE_BUSY:
			break;
		case CONNECTION_STATE_PAUSED:
			break;
		default:
			return;
		}
		MainWindow.setConnectionState(state);
		connectionState = state;
	}
	
	private static void sendCommandLine(String s) {
		try {
			MainWindow.log("SEND\t|" + s);
			output.write(s.getBytes("ASCII"));
			output.write((byte)0x0a);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void sendCommand(String s) {
		if (connectionState != CONNECTION_STATE_CONNECTED)
			return;
		String[] cmd = s.split("\n");
		for (String st : cmd) {
			sendCommandLine(st);
		}
	}
	
	public static void connect() {
		if (connectionState != CONNECTION_STATE_DISCONNECTED) {
			return ;
		}
		setConnectionState(CONNECTION_STATE_DISCONNECTING);
        System.setProperty("gnu.io.rxtx.SerialPorts", PropertyManager.getPortName());
        CommPortIdentifier portId = null;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

		//First, Find an instance of serial port as set in PORT_NAMES.
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
			if (currPortId.getName().equals(PropertyManager.getPortName())) {
				portId = currPortId;
				break;
			}
		
		}
		if (portId == null) {
			MainWindow.log("Could not find port.");
			setConnectionState(CONNECTION_STATE_DISCONNECTED);
			return ;
		}

		try {
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open("SerialCommunicationManager", TIME_OUT);

			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);

			// open the streams
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			output = serialPort.getOutputStream();

			// add event listeners
			serialPort.addEventListener(serialPortEventListener);
			serialPort.notifyOnDataAvailable(true);
		} catch (Exception e) {
			System.err.println(e.toString());
			return ;
		}
		setConnectionState(CONNECTION_STATE_CONNECTED);
	}
	
	public static void testConnection() {
		sendCommandLine("TEST;");
	}
	
	public static void disconnect() {
		setConnectionState(CONNECTION_STATE_DISCONNECTING);
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
		setConnectionState(CONNECTION_STATE_DISCONNECTED);
	}

	private static void sendCommandFromList() {
		String cmdLine = null;
		try {
			for (cmdLine = commandReader.readLine(); cmdLine != null && !cmdLine.endsWith(";"); )
				cmdLine = commandReader.readLine(); 
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (cmdLine == null) {
			setConnectionState(CONNECTION_STATE_CONNECTED);
		} else {
			sendCommandLine(cmdLine);
		}
	}
	
	public static void sendCommandList(String fileName) throws FileNotFoundException {
		File cmdFile = new File(fileName);
		if(!cmdFile.exists() || cmdFile.isDirectory())
             throw new FileNotFoundException();
		commandReader = new BufferedReader(new FileReader(cmdFile));
		
		setConnectionState(CONNECTION_STATE_BUSY);
		MainWindow.log("SYSTEM\t|Sending Started.");
		sendCommandFromList(); // send the first command and when state is busy, automatically send the next command.
		
	}
	
	public static void pauseCommandList() {
		if (connectionState != CONNECTION_STATE_BUSY)
			return;
		setConnectionState(CONNECTION_STATE_PAUSED);
	}
	public static void continueCommandList() {
		if (connectionState != CONNECTION_STATE_PAUSED)
			return;
		MainWindow.log("SYSTEM\t|Continue.");
		setConnectionState(CONNECTION_STATE_BUSY);
		sendCommandFromList();
	}
	public static void abortCommandList() {
		if (connectionState != CONNECTION_STATE_PAUSED && connectionState != CONNECTION_STATE_BUSY)
			return;
		MainWindow.log("SYSTEM\t|Aborted.");
		setConnectionState(CONNECTION_STATE_CONNECTED);
	}
	
	private static class MySerialPortEventListener implements SerialPortEventListener {
		public synchronized void serialEvent(SerialPortEvent oEvent) {
			if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
				String inputLine = null;
				try {
					inputLine = input.readLine();
					MainWindow.log("RECEIVE\t|" + inputLine);
				} catch (Exception e) {
					System.err.println(e.toString());
				}
				
				if (connectionState == CONNECTION_STATE_BUSY && inputLine.startsWith("Executed")) {
					sendCommandFromList();
				}
				
				if (connectionState == CONNECTION_STATE_PAUSED && inputLine.startsWith("Executed")) {
					MainWindow.log("SYSTEM\t|Paused.");
				}
				
			}
		}
	}
	
}
