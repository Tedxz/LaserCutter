package xz.lasercutter;

import javax.swing.JFrame;
import javax.swing.JButton;

import java.awt.*;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.border.LineBorder;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JTextField;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JTextArea;

import java.awt.Font;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.*;

import java.awt.SystemColor;

import javax.swing.JSeparator;

import java.awt.Component;

import static xz.lasercutter.SerialCommunicationManager.*;

import java.awt.FlowLayout;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

public class MainWindow extends JFrame {
	private static final Font cmdFont = new Font("Serif", Font.PLAIN, 17);
	
	private static MainWindow mainWindow;
	
	private static String picturePath;
	private String lastCmd = "";
	BufferedWriter logWriter = null;
	CommandGenerator cg;
	
	private JTextField textFieldPicPath;
	private JTextField textFieldCmdLine;
	private JLabel lblConnectionState;
	private JTextArea textAreaCommandArea;
	private JScrollPane scrollPaneCmd;
	private JButton btnSend;
	private JButton btnConnect;
	private JButton btnDisconnect;
	private JButton btnOpenImage;
	private JButton btnTest;
	private JButton btnPause;
	private JButton btnContinue;
	private JButton btnUp;
	private JButton btnDown;
	private JButton btnLeft;
	private JButton btnRight;
	private JButton btnMargin;
	private JButton btnLaserOff;
	private JButton btnLaserLow;
	private JButton btnLaserHigh;
	
	private JPanel panelOriPic;
	private JPanel panelMdfPic;
	private JTextField textFieldPortName;
	
	private JLabel lblOriPic;
	private JLabel lblMdfPic;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private JRadioButton[] rbtnConvertMethod;
	
	
	public static void main(String[] args) {
		mainWindow = new MainWindow();
	}
	
	public static void setConnectionStateStr(String s) {
		if (mainWindow == null) 
			return;
		mainWindow.lblConnectionState.setText(s);
	}
	
	public static void setConnectionState(int state) {
		switch (state) {
		case CONNECTION_STATE_CONNECTED:
			setConnectionStateStr("Connected.");
			mainWindow.textFieldPortName.setEditable(false);
			break;
		case CONNECTION_STATE_DISCONNECTED:
			setConnectionStateStr("Disconnected.");
			mainWindow.textFieldPortName.setEditable(true);
			break;
		case CONNECTION_STATE_CONNECTING:
			setConnectionStateStr("Connecting...");
			break;
		case CONNECTION_STATE_DISCONNECTING:
			setConnectionStateStr("Disconnecting...");
			break;
		case CONNECTION_STATE_BUSY:
			setConnectionStateStr("Busy...");
			break;
		default:
			return;
		}
	}
	
	public static void log(String s) {
		if (mainWindow == null) 
			return ;
		mainWindow.textAreaCommandArea.append(s + "\n");
		int scl = mainWindow.scrollPaneCmd.getVerticalScrollBar().getMaximum();
		mainWindow.scrollPaneCmd.getVerticalScrollBar().setValue(scl);
		try {
			mainWindow.logWriter.append("" + System.currentTimeMillis() + "\t");
			mainWindow.logWriter.append(s);
			mainWindow.logWriter.newLine();
			mainWindow.logWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setOriPic(String path) {
		if (path == null)
			return;
		ImageIcon img = new ImageIcon(path);
		img.setImage(img.getImage().getScaledInstance(280, 280, Image.SCALE_AREA_AVERAGING));
		lblOriPic.setIcon(img);
	}
	
	public void setMdfPic(String path) {
		if (path == null)
			return;
		ImageIcon img = new ImageIcon(path);
		img.setImage(img.getImage().getScaledInstance(280, 280, Image.SCALE_AREA_AVERAGING));
		lblMdfPic.setIcon(img);
	}
	
	private void changePrintMethod() {
		for (int i = 0; i < rbtnConvertMethod.length; ++i) {
			if (rbtnConvertMethod[i].isSelected()) {
				ImageConverter.choicePrintMethod(i);
				break;
			}
		}
		ImageConverter.processPicture();
	}
	
	private MainWindow() {
		File logFile = new File(PropertyManager.getTempLogPath());
		try {
			if (!logFile.exists())
				logFile.createNewFile();
			logWriter = new BufferedWriter(new FileWriter(PropertyManager.getTempLogPath(), true));
			logWriter.newLine();
			logWriter.append("" + System.currentTimeMillis() + "\tLASER CUTTER WORKSHOP STARTED");
			logWriter.newLine();
		} catch (IOException e2) {
			e2.printStackTrace();
		} 
		
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				//SerialCommunicationManager.disconnect();
				System.exit(0);
			}
			@Override
			public void windowOpened(WindowEvent e) {
				SerialCommunicationManager.connect();
			}
		});
		setSize(new Dimension(1200, 700));
		setTitle("Laser Cutter Workshop -- XZ");
		getContentPane().setLayout(null);
		
		panelOriPic = new JPanel();
		panelOriPic.setBorder(new LineBorder(new Color(0, 0, 0), 2));
		panelOriPic.setSize(new Dimension(100, 100));
		panelOriPic.setBounds(26, 71, 284, 284);
		getContentPane().add(panelOriPic);
		panelOriPic.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0));
		
		lblOriPic = new JLabel();
		panelOriPic.add(lblOriPic);
		
		panelMdfPic = new JPanel();
		panelMdfPic.setSize(new Dimension(100, 100));
		panelMdfPic.setBorder(new LineBorder(new Color(0, 0, 0), 2));
		panelMdfPic.setBounds(355, 71, 284, 284);
		getContentPane().add(panelMdfPic);
		panelMdfPic.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0));
		
		lblMdfPic = new JLabel();
		panelMdfPic.add(lblMdfPic);
		
		btnOpenImage = new JButton("Open Image");
		btnOpenImage.setBackground(SystemColor.control);
		btnOpenImage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser(PropertyManager.getInitialPicPath());
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fileChooser.setVisible(true);
				int opt = fileChooser.showDialog(null, "Choose Picture");
				if (opt == JFileChooser.APPROVE_OPTION) {
					try {
						picturePath = fileChooser.getSelectedFile().getAbsolutePath();
					} catch (NullPointerException e1) {
						System.out.println("No Such File.");
					}
					textFieldPicPath.setText(picturePath);
					ImageConverter.setPictuerPath(picturePath);
					ImageConverter.processPicture();
				}
				setOriPic(picturePath);
				setMdfPic(PropertyManager.getTempPicPath());

			}
		});
		btnOpenImage.setBounds(26, 426, 117, 27);
		getContentPane().add(btnOpenImage);
		
		textFieldPicPath = new JTextField();
		textFieldPicPath.setEditable(false);
		textFieldPicPath.setBounds(155, 429, 495, 21);
		getContentPane().add(textFieldPicPath);
		textFieldPicPath.setColumns(10);
		
		btnConnect = new JButton("Connect");
		btnConnect.setBackground(SystemColor.control);
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PropertyManager.setPortName(textFieldPortName.getText());
				SerialCommunicationManager.connect();
			}
		});
		btnConnect.setBounds(39, 519, 107, 27);
		getContentPane().add(btnConnect);
		
		btnDisconnect = new JButton("Disconnect");
		btnDisconnect.setBackground(SystemColor.control);
		btnDisconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.disconnect();
			}
		});
		btnDisconnect.setBounds(340, 519, 107, 27);
		getContentPane().add(btnDisconnect);
		
		btnTest = new JButton("Test Connect");
		btnTest.setBackground(SystemColor.control);
		btnTest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.testConnection();
			}
		});
		btnTest.setBounds(188, 519, 131, 27);
		getContentPane().add(btnTest);
		
		textFieldCmdLine = new JTextField();
		textFieldCmdLine.setFont(cmdFont);
		textFieldCmdLine.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (textFieldCmdLine.getText().length() > 0) {
					SerialCommunicationManager.sendCommand(textFieldCmdLine.getText());
					lastCmd = textFieldCmdLine.getText();
					textFieldCmdLine.setText("");
				} else {
					SerialCommunicationManager.sendCommand(lastCmd);
				}
			}
		});
		textFieldCmdLine.setBounds(753, 400, 279, 21);
		getContentPane().add(textFieldCmdLine);
		textFieldCmdLine.setColumns(10);
		
		btnSend = new JButton("Send");
		btnSend.setBackground(SystemColor.control);
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(textFieldCmdLine.getText());
				textFieldCmdLine.setText("");
			}
		});
		btnSend.setBounds(1041, 397, 96, 27);
		getContentPane().add(btnSend);
		
		lblConnectionState = new JLabel("No Connection.");
		lblConnectionState.setAlignmentY(Component.TOP_ALIGNMENT);
		lblConnectionState.setBounds(10, 680, 174, 17);
		getContentPane().add(lblConnectionState);
		
		scrollPaneCmd = new JScrollPane();
		scrollPaneCmd.setAutoscrolls(true);
		scrollPaneCmd.setBounds(753, 71, 384, 297);
		getContentPane().add(scrollPaneCmd);
		
		textAreaCommandArea = new JTextArea();
		scrollPaneCmd.setViewportView(textAreaCommandArea);
		textAreaCommandArea.setFont(cmdFont);
		textAreaCommandArea.setBorder(new LineBorder(new Color(0, 0, 0)));
		textAreaCommandArea.setEditable(false);
		
		JButton btnSendFile = new JButton("Send File");
		btnSendFile.setBackground(SystemColor.control);
		btnSendFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					SerialCommunicationManager.sendCommandList(PropertyManager.getTempCmdPath());
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
			}
		});
		btnSendFile.setBounds(1041, 462, 107, 27);
		getContentPane().add(btnSendFile);
		
		btnPause = new JButton("Pause");
		btnPause.setBackground(SystemColor.control);
		btnPause.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.pauseCommandList();
			}
		});
		btnPause.setBounds(753, 462, 107, 27);
		getContentPane().add(btnPause);
		
		btnContinue = new JButton("Continue");
		btnContinue.setBackground(SystemColor.control);
		btnContinue.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.continueCommandList();
			}
		});
		btnContinue.setBounds(901, 462, 107, 27);
		getContentPane().add(btnContinue);
		
		cg = new CommandGenerator();
		
		final int btnMovePositionX = 800;
		final int btnMovePositionY = 570;
		
		btnUp = new JButton("Up");
		btnUp.setBackground(SystemColor.control);
		btnUp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pMoveUp());
			}
		});
		btnUp.setBounds(btnMovePositionX, btnMovePositionY - 30, 70, 24);
		getContentPane().add(btnUp);
		
		btnDown = new JButton("Down");
		btnDown.setBackground(SystemColor.control);
		btnDown.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pMoveDown());
			}
		});
		btnDown.setBounds(btnMovePositionX, btnMovePositionY + 30, 70, 24);
		getContentPane().add(btnDown);
		
		btnLeft = new JButton("Left");
		btnLeft.setBackground(SystemColor.control);
		btnLeft.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pMoveLeft());
			}
		});
		btnLeft.setBounds(btnMovePositionX - 50, btnMovePositionY, 70, 24);
		getContentPane().add(btnLeft);
		
		btnRight = new JButton("Right");
		btnRight.setBackground(SystemColor.control);
		btnRight.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pMoveRight());
			}
		});
		btnRight.setBounds(btnMovePositionX + 50, btnMovePositionY, 70, 24);
		getContentPane().add(btnRight);
		
		btnMargin = new JButton("Margin");
		btnMargin.setBackground(SystemColor.control);
		btnMargin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pMargin());
			}
		});
		btnMargin.setBounds(btnMovePositionX, btnMovePositionY + 60, 80, 24);
		getContentPane().add(btnMargin);
		
		final int btnLaserX = btnMovePositionX + 200;
		final int btnLaserY = btnMovePositionY - 12;
		final int btnLaserW = 100;
		btnLaserOff = new JButton("Laser Off");
		btnLaserOff.setBackground(SystemColor.control);
		btnLaserOff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pLaserOff());
			}
		});
		btnLaserOff.setBounds(btnLaserX, btnLaserY, btnLaserW, 24);
		getContentPane().add(btnLaserOff);
		
		btnLaserLow = new JButton("Laser Low");
		btnLaserLow.setBackground(SystemColor.control);
		btnLaserLow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pLaserLow());
			}
		});
		btnLaserLow.setBounds(btnLaserX, btnLaserY + 30, btnLaserW, 24);
		getContentPane().add(btnLaserLow);
		
		btnLaserHigh = new JButton("Laser High");
		btnLaserHigh.setBackground(SystemColor.control);
		btnLaserHigh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pLaserHigh());
			}
		});
		btnLaserHigh.setBounds(btnLaserX, btnLaserY + 60, btnLaserW, 24);
		getContentPane().add(btnLaserHigh);
		
		
		JSeparator separator = new JSeparator();
		separator.setBounds(0, 664, 1200, 6);
		getContentPane().add(separator);
		
		textFieldPortName = new JTextField();
		textFieldPortName.setText(PropertyManager.getPortName());
		textFieldPortName.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PropertyManager.setPortName(textFieldPortName.getText());
			}
		});
		textFieldPortName.setBounds(49, 554, 140, 27);
		getContentPane().add(textFieldPortName);
		textFieldPortName.setColumns(10);
		
		int rbtnConvertMethodX = 461;
		int rbtnConvertMethodY = 483;
		int rbtnConvertMethodWidth = 248;
		int rbtnConvertMethodHeight = 24;
		
		rbtnConvertMethod = new JRadioButton[ImageConverter.NUMBER_OF_PRINT_METHODS];
		for (int i = 0; i < rbtnConvertMethod.length; ++i) {
			rbtnConvertMethod[i] = new JRadioButton(ImageConverter.getPrintMethodName(i));
			rbtnConvertMethod[i].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					mainWindow.changePrintMethod();
				}
			});
			buttonGroup.add(rbtnConvertMethod[i]);
			rbtnConvertMethod[i].setBounds(rbtnConvertMethodX, rbtnConvertMethodY + i * rbtnConvertMethodHeight, rbtnConvertMethodWidth, rbtnConvertMethodHeight);
			getContentPane().add(rbtnConvertMethod[i]);
		}
		rbtnConvertMethod[ImageConverter.getChoicedPrintMethod()].setSelected(true);
		/*
		JRadioButton rdbtnConvertMethod0 = new JRadioButton("Method 0");
		buttonGroup.add(rdbtnConvertMethod0);
		rdbtnConvertMethod0.setBounds(431, 483, 148, 24);
		getContentPane().add(rdbtnConvertMethod0);
		
		JRadioButton rdbtnConvertMethod1 = new JRadioButton("Method 1");
		buttonGroup.add(rdbtnConvertMethod1);
		rdbtnConvertMethod1.setBounds(431, 503, 148, 24);
		getContentPane().add(rdbtnConvertMethod1);
		*/
		setVisible(true);
		
	}
}
