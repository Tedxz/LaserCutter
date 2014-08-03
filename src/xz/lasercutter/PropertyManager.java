package xz.lasercutter;

import java.io.*;
import java.util.Properties;

public class PropertyManager {
	public static final int MOTOR_MOVE_DISTANCE 	= 1400;
	public static final int MOTOR_MOVE_MARGIN 		= 25;
	public static final int MOTOR_TURNING_EPS_Y 	= 15; //vertical
	public static final int MOTOR_TURNING_EPS_X 	= 9; //horizontal
	
	public static final int BRIGHTNESS_OFF 	= 0;
	public static final int BRIGHTNESS_LOW 	= 2;
	public static final int BRIGHTNESS_HIGH = 3;
	
	
	// Constants 
	private static final String directoryTempFiles 	= "./tmp/";
	private static final String fileNameConvPic 	= "converted_pic.png";
	private static final String fileNameConfig 		= "config.properties";
	private static final String fileNameLog 		= "log.txt";
	private static final String fileNameCmdLst 		= "laser_cmd.txt";
	private static final String fileNamePosCmd 		= "show_position_cmd.txt";
	
	// Default value of variables
	private static String defaultPortName 			= "";
	private static String defaultPossiblePortNames 	= "";
	private static String defaultInitPicDir 		= ".";
	private static int defaultLineDelay 			= 50;
	private static int defaultDotDelay 				= 500;
	private static int defaultChoicedPrintMethod 	= ImageConverter.NUMBER_OF_PRINT_METHODS - 1;
	
	// Property
	private static Properties properties = new Properties();
	
	static {
		loadProperties();
	}
	
	public static void loadProperties() {
		String filePathConfig = directoryTempFiles + fileNameConfig;
		File fileConfig = null;
		FileInputStream configInputStream = null;
		
		properties.clear();
		
		try {
			fileConfig = new File(filePathConfig);
			configInputStream = new FileInputStream(fileConfig);
			properties.load(configInputStream);
		} catch (IOException e) {
			MainWindow.log("SYSTEM\t|Configure file not found.");
		} finally {
			if (configInputStream != null)
				try {
					configInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		
	}
	
	public static void storeProperties() {
		String filePathConfig = directoryTempFiles + fileNameConfig;
		File fileConfig = null;
		FileOutputStream configOutputStream = null;
		
		try {
			fileConfig = new File(filePathConfig);
			fileConfig.createNewFile();
			configOutputStream = new FileOutputStream(fileConfig);
			properties.store(configOutputStream, null);
		} catch (IOException e) {
			MainWindow.log("SYSTEM\t|Configure file writing error.");
		} finally {
			if (configOutputStream != null)
				try {
					configOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		
	}
	
	public static String getLogFilePath() {
		return directoryTempFiles + fileNameLog;
	}
	public static String getConvPicFilePath() {
		return directoryTempFiles + fileNameConvPic;
	}
	public static String getCmdLstFilePath() {
		return directoryTempFiles + fileNameCmdLst;
	}
	public static String getPosCmdFilePath() {
		return directoryTempFiles + fileNamePosCmd;
	}
	
	public static int getDrawLineDelay() {
		String s = properties.getProperty("LineDelay");
		if (s == null) {
			properties.setProperty("LineDelay", "" + defaultLineDelay);
			s = "" + defaultLineDelay;
		}
		return Integer.parseInt(s);
	}

	public static int getDrawDotDelay() {
		String s = properties.getProperty("DotDelay");
		if (s == null) {
			properties.setProperty("DotDelay", "" + defaultDotDelay);
			s = "" + defaultDotDelay;
		}
		return Integer.parseInt(s);
	}
	
	public static int getChoicedPrintMethod() {
		String s = properties.getProperty("ChoicedPrintMethod");
		if (s == null) {
			properties.setProperty("ChoicedPrintMethod", "" + defaultChoicedPrintMethod);
			s = "" + defaultChoicedPrintMethod;
		}
		int x = Integer.parseInt(s);
		if (x < 0 || x >= ImageConverter.NUMBER_OF_PRINT_METHODS)
			x = defaultChoicedPrintMethod;
		return x;
	}
	
	public static void setChoicedPrintMethod(int cpm) {
		properties.setProperty("ChoicedPrintMethod", "" + cpm);
		storeProperties();
	}
	
	public static String getInitialImageDirectory() {
		String s = properties.getProperty("InitialPictureDirectory");
		if (s == null) {
			properties.setProperty("InitialPictureDirectory", defaultInitPicDir);
			s = defaultInitPicDir;
		}
		return s;
	}
	
	public static void setInitialImageDirectory(String iid) {
		properties.setProperty("InitialPictureDirectory", iid);
		storeProperties();
	}
	
	public static String[] getPossiblePortNames() {
		String s = properties.getProperty("PossiblePortNames");
		if (s == null) {
			properties.setProperty("PossiblePortNames", defaultPossiblePortNames);
			s = defaultPossiblePortNames;
		}
		return (getPortName() + "," + s).split(",");
	}
	
	public static String getPortName() {
		String s = properties.getProperty("PortName");
		if (s == null) {
			properties.setProperty("PortName", defaultPortName);
			s = defaultPortName;
		}
		return s;
	}
	
	public static void setPortName(String pn) {
		properties.setProperty("PortName", pn);
		MainWindow.updatePortName(pn);
		storeProperties();
	}
}
