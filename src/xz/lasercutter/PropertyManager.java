package xz.lasercutter;

import javax.swing.JFrame;

public class PropertyManager extends JFrame {
	public static final int MOTOR_MOVE_DISTANCE = 1400;
	public static final int MOTOR_MOVE_MARGIN = 25;
	public static final int MOTOR_TURNING_EPS_Y = 15; //vertical
	public static final int MOTOR_TURNING_EPS_X = 9; //horizontal
	
	public static final String[] POSSIBLE_PORT_NAMES = {
		"/dev/ttyUSB0",
		"COM5",
		"COM4",
		"/dev/ttyUSB1",
		"/dev/ttyUSB2"
	};
	
	private static String portName;
	//Directory
	private static String initialPicDirectory = "./";
	private static String tempDirectory = "./tmp/";
	private static String tempPicFileName = "converted_pic.png";
	private static String tempLogFileName = "log.txt";
	private static String tempCmdFileName = "laser_cmd.txt";
	
	private static int drawBrightness = 3;
	private static int drawLineDelay = 50; // draft paper:50
	private static int drawDotDelay = 500;  // draft paper:90+
	
	public static String getTempLogFileName() {
		return tempLogFileName;
	}

	public static void setTempLogFileName(String tempLogFileName) {
		PropertyManager.tempLogFileName = tempLogFileName;
	}

	public static int getDrawBrightness() {
		return drawBrightness;
	}

	public static void setDrawBrightness(int drawBrightness) {
		PropertyManager.drawBrightness = drawBrightness;
	}

	public static int getDrawLineDelay() {
		return drawLineDelay;
	}

	public static void setDrawLineDelay(int drawLineDelay) {
		PropertyManager.drawLineDelay = drawLineDelay;
	}

	public static int getDrawDotDelay() {
		return drawDotDelay;
	}

	public static void setDrawDotDelay(int drawDotDelay) {
		PropertyManager.drawDotDelay = drawDotDelay;
	}

	public static String getTempLogPath() {
		return tempDirectory + tempLogFileName;
	}
	public static String getTempPicPath() {
		return tempDirectory + tempPicFileName;
	}
	
	public static String getTempCmdPath() {
		return tempDirectory + tempCmdFileName;
	}
	
	public static String getTempPicFileName() {
		return tempPicFileName;
	}

	public static void setTempPicFileName(String tempPicFileName) {
		PropertyManager.tempPicFileName = tempPicFileName;
	}


	public static String getTempCmdFileName() {
		return tempCmdFileName;
	}


	public static void setTempCmdFileName(String tempCmdFileName) {
		PropertyManager.tempCmdFileName = tempCmdFileName;
	}


	public static String getTempPath() {
		return tempDirectory;
	}


	public static void setTempPath(String tempPath) {
		PropertyManager.tempDirectory = tempPath;
	}


	public static String getInitialPicPath() {
		return initialPicDirectory;
	}


	public static void setInitialPicPath(String initialPicPath) {
		PropertyManager.initialPicDirectory = initialPicPath;
	}


	public static String getPortName() {
		return portName;
	}

	public static void setPortName(String portName) {
		PropertyManager.portName = portName;
		MainWindow.setPortName(portName);
	}


	public PropertyManager() {
	}

}
