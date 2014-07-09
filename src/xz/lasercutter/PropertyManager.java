package xz.lasercutter;

import javax.swing.JFrame;

public class PropertyManager extends JFrame {
	public static final int MOTOR_MOVE_DISTANCE = 1400;
	public static final int MOTOR_MOVE_MARGIN = 25;
	public static final int MOTOR_TURNING_EPS_Y = 15; //vertical
	public static final int MOTOR_TURNING_EPS_X = 9; //horizontal
	
	
	private static String portName = "/dev/ttyUSB0";
	
	private static String initialPicPath = "/home/xz/Pictures/";
	private static String tempPath = "/home/xz/Pictures/lasercut/";
	private static String tempPicFileName = "laser_tmp.png";
	private static String tempLogFileName = "laser_log.txt";
	
	private static int drawBrightness = 3;
	private static int drawLineDelay = 50; // draft paper:50
	private static int drawDotDelay = 90;  // draft paper:90+
	
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
		return tempPath + tempLogFileName;
	}
	public static String getTempPicPath() {
		return tempPath + tempPicFileName;
	}
	
	public static String getTempCmdPath() {
		return tempPath + tempCmdFileName;
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


	private static String tempCmdFileName = "laser_cmd.txt";
	
	public static String getTempPath() {
		return tempPath;
	}


	public static void setTempPath(String tempPath) {
		PropertyManager.tempPath = tempPath;
	}


	public static String getInitialPicPath() {
		return initialPicPath;
	}


	public static void setInitialPicPath(String initialPicPath) {
		PropertyManager.initialPicPath = initialPicPath;
	}


	public static String getPortName() {
		return portName;
	}


	public static void setPortName(String portName) {
		PropertyManager.portName = portName;
	}


	public PropertyManager() {
	}

}
