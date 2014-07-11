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
import javax.swing.border.TitledBorder;

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
	private JButton btnAbort;
	private JPanel panel;
	private JPanel panel_1;
	private JPanel panel_4;
	private JPanel panel_5;
	private JPanel panel_6;
	private JPanel panel_3;
	
	
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
		
		panel_5 = new JPanel();
		panel_5.setBorder(new TitledBorder(null, "Origin Image", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_5.setBounds(23, 12, 296, 309);
		getContentPane().add(panel_5);
		panel_5.setLayout(null);
		
		panelOriPic = new JPanel();
		panelOriPic.setBounds(6, 19, 284, 284);
		panel_5.add(panelOriPic);
		panelOriPic.setBorder(new LineBorder(new Color(0, 0, 0), 2));
		panelOriPic.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0));
		
		lblOriPic = new JLabel();
		panelOriPic.add(lblOriPic);
		
		panel_6 = new JPanel();
		panel_6.setBorder(new TitledBorder(null, "Converted Image", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_6.setBounds(352, 12, 296, 309);
		getContentPane().add(panel_6);
		panel_6.setLayout(null);
		
		panelMdfPic = new JPanel();
		panelMdfPic.setBounds(6, 19, 284, 284);
		panel_6.add(panelMdfPic);
		panelMdfPic.setBorder(new LineBorder(new Color(0, 0, 0), 2));
		panelMdfPic.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0));
		
		lblMdfPic = new JLabel();
		panelMdfPic.add(lblMdfPic);
		
		panel = new JPanel();
		panel.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Image", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel.setBounds(23, 367, 636, 52);
		getContentPane().add(panel);
		panel.setLayout(null);
		
		btnOpenImage = new JButton("Open Image");
		btnOpenImage.setBounds(6, 19, 117, 27);
		panel.add(btnOpenImage);
		btnOpenImage.setBackground(SystemColor.control);
		
		textFieldPicPath = new JTextField();
		textFieldPicPath.setBounds(135, 22, 495, 21);
		panel.add(textFieldPicPath);
		textFieldPicPath.setEditable(false);
		textFieldPicPath.setColumns(10);
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
		
		panel_1 = new JPanel();
		panel_1.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Connection", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_1.setBounds(23, 431, 369, 125);
		getContentPane().add(panel_1);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[] {110, 110, 110};
		gbl_panel_1.rowHeights = new int[]{30, 30};
		gbl_panel_1.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0};
		gbl_panel_1.rowWeights = new double[]{0.0, 0.0};
		panel_1.setLayout(gbl_panel_1);
		
		JLabel labelPortName = new JLabel("Port Name");
		GridBagConstraints gbc_labelPortName = new GridBagConstraints();
		gbc_labelPortName.anchor = GridBagConstraints.EAST;
		gbc_labelPortName.fill = GridBagConstraints.VERTICAL;
		gbc_labelPortName.insets = new Insets(5, 5, 5, 5);
		gbc_labelPortName.gridx = 0;
		gbc_labelPortName.gridy = 0;
		panel_1.add(labelPortName, gbc_labelPortName);
		
		textFieldPortName = new JTextField();
		GridBagConstraints gbc_textFieldPortName = new GridBagConstraints();
		gbc_textFieldPortName.fill = GridBagConstraints.BOTH;
		gbc_textFieldPortName.insets = new Insets(5, 5, 5, 5);
		gbc_textFieldPortName.gridwidth = 3;
		gbc_textFieldPortName.gridx = 1;
		gbc_textFieldPortName.gridy = 0;
		panel_1.add(textFieldPortName, gbc_textFieldPortName);
		textFieldPortName.setText(PropertyManager.getPortName());
		textFieldPortName.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PropertyManager.setPortName(textFieldPortName.getText());
			}
		});
		textFieldPortName.setColumns(10);
		
		btnConnect = new JButton("Connect");
		GridBagConstraints gbc_btnConnect = new GridBagConstraints();
		gbc_btnConnect.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnConnect.insets = new Insets(5, 5, 5, 5);
		gbc_btnConnect.gridx = 0;
		gbc_btnConnect.gridy = 1;
		panel_1.add(btnConnect, gbc_btnConnect);
		btnConnect.setBackground(SystemColor.control);
		btnConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PropertyManager.setPortName(textFieldPortName.getText());
				SerialCommunicationManager.connect();
			}
		});
		
		btnTest = new JButton("Test Connect");
		GridBagConstraints gbc_btnTest = new GridBagConstraints();
		gbc_btnTest.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnTest.insets = new Insets(5, 5, 5, 5);
		gbc_btnTest.gridx = 1;
		gbc_btnTest.gridy = 1;
		panel_1.add(btnTest, gbc_btnTest);
		btnTest.setBackground(SystemColor.control);
		btnTest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.testConnection();
			}
		});
		
		btnDisconnect = new JButton("Disconnect");
		GridBagConstraints gbc_btnDisconnect = new GridBagConstraints();
		gbc_btnDisconnect.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnDisconnect.insets = new Insets(5, 5, 5, 5);
		gbc_btnDisconnect.gridx = 2;
		gbc_btnDisconnect.gridy = 1;
		panel_1.add(btnDisconnect, gbc_btnDisconnect);
		btnDisconnect.setBackground(SystemColor.control);
		btnDisconnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.disconnect();
			}
		});
		
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
		textFieldCmdLine.setBounds(756, 360, 279, 21);
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
		btnSend.setBounds(1044, 357, 96, 27);
		getContentPane().add(btnSend);
		
		lblConnectionState = new JLabel("No Connection.");
		lblConnectionState.setAlignmentY(Component.TOP_ALIGNMENT);
		lblConnectionState.setBounds(10, 680, 174, 17);
		getContentPane().add(lblConnectionState);
		
		scrollPaneCmd = new JScrollPane();
		scrollPaneCmd.setAutoscrolls(true);
		scrollPaneCmd.setBounds(756, 31, 384, 297);
		getContentPane().add(scrollPaneCmd);
		
		textAreaCommandArea = new JTextArea();
		scrollPaneCmd.setViewportView(textAreaCommandArea);
		textAreaCommandArea.setFont(cmdFont);
		textAreaCommandArea.setBorder(new LineBorder(new Color(0, 0, 0)));
		textAreaCommandArea.setEditable(false);
		
		panel_4 = new JPanel();
		panel_4.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Print", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_4.setBounds(760, 406, 384, 88);
		getContentPane().add(panel_4);
		GridBagLayout gbl_panel_4 = new GridBagLayout();
		gbl_panel_4.columnWidths = new int[]{0, 120, 30, 120};
		gbl_panel_4.rowHeights = new int[]{27, 27};
		gbl_panel_4.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0};
		gbl_panel_4.rowWeights = new double[]{0.0, 0.0};
		panel_4.setLayout(gbl_panel_4);
		
		JButton btnSendFile = new JButton("Start");
		GridBagConstraints gbc_btnSendFile = new GridBagConstraints();
		gbc_btnSendFile.fill = GridBagConstraints.BOTH;
		gbc_btnSendFile.insets = new Insets(0, 0, 5, 5);
		gbc_btnSendFile.gridx = 1;
		gbc_btnSendFile.gridy = 0;
		panel_4.add(btnSendFile, gbc_btnSendFile);
		btnSendFile.setBackground(SystemColor.controlLtHighlight);
		btnSendFile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					SerialCommunicationManager.sendCommandList(PropertyManager.getTempCmdPath());
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
			}
		});
		
		btnPause = new JButton("Pause");
		GridBagConstraints gbc_btnPause = new GridBagConstraints();
		gbc_btnPause.fill = GridBagConstraints.BOTH;
		gbc_btnPause.insets = new Insets(0, 0, 5, 0);
		gbc_btnPause.gridx = 3;
		gbc_btnPause.gridy = 0;
		panel_4.add(btnPause, gbc_btnPause);
		btnPause.setBackground(SystemColor.control);
		btnPause.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.pauseCommandList();
			}
		});
		
		btnAbort = new JButton("Abort");
		GridBagConstraints gbc_btnAbort = new GridBagConstraints();
		gbc_btnAbort.insets = new Insets(0, 0, 0, 5);
		gbc_btnAbort.fill = GridBagConstraints.BOTH;
		gbc_btnAbort.gridx = 1;
		gbc_btnAbort.gridy = 1;
		panel_4.add(btnAbort, gbc_btnAbort);
		btnAbort.setBackground(SystemColor.control);
		btnAbort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.abortCommandList();
			}
		});
		
		btnContinue = new JButton("Continue");
		GridBagConstraints gbc_btnContinue = new GridBagConstraints();
		gbc_btnContinue.fill = GridBagConstraints.BOTH;
		gbc_btnContinue.gridx = 3;
		gbc_btnContinue.gridy = 1;
		panel_4.add(btnContinue, gbc_btnContinue);
		btnContinue.setBackground(SystemColor.control);
		btnContinue.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.continueCommandList();
			}
		});
		
		cg = new CommandGenerator();
		
		final int btnMovePositionX = 800;
		final int btnMovePositionY = 570;
		
		panel_3 = new JPanel();
		panel_3.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Quick Control", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_3.setBounds(760, 509, 384, 154);
		getContentPane().add(panel_3);
		panel_3.setLayout(null);
		
		btnUp = new JButton("Up");
		btnUp.setBounds(92, 28, 70, 24);
		panel_3.add(btnUp);
		btnUp.setBackground(SystemColor.control);
		
		btnDown = new JButton("Down");
		btnDown.setBounds(92, 88, 70, 24);
		panel_3.add(btnDown);
		btnDown.setBackground(SystemColor.control);
		
		btnLeft = new JButton("Left");
		btnLeft.setBounds(42, 58, 70, 24);
		panel_3.add(btnLeft);
		btnLeft.setBackground(SystemColor.control);
		
		btnRight = new JButton("Right");
		btnRight.setBounds(142, 58, 70, 24);
		panel_3.add(btnRight);
		btnRight.setBackground(SystemColor.control);
		
		btnMargin = new JButton("Margin");
		btnMargin.setBounds(87, 118, 80, 24);
		panel_3.add(btnMargin);
		btnMargin.setBackground(SystemColor.control);
		btnLaserOff = new JButton("Laser Off");
		btnLaserOff.setBounds(256, 37, 100, 24);
		panel_3.add(btnLaserOff);
		btnLaserOff.setBackground(SystemColor.control);
		
		btnLaserLow = new JButton("Laser Low");
		btnLaserLow.setBounds(256, 67, 100, 24);
		panel_3.add(btnLaserLow);
		btnLaserLow.setBackground(SystemColor.control);
		
		btnLaserHigh = new JButton("Laser High");
		btnLaserHigh.setBounds(256, 97, 100, 24);
		panel_3.add(btnLaserHigh);
		btnLaserHigh.setBackground(SystemColor.control);
		btnLaserHigh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pLaserHigh());
			}
		});
		btnLaserLow.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pLaserLow());
			}
		});
		btnLaserOff.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pLaserOff());
			}
		});
		btnMargin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pMargin());
			}
		});
		btnRight.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pMoveRight());
			}
		});
		btnLeft.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pMoveLeft());
			}
		});
		btnDown.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pMoveDown());
			}
		});
		btnUp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SerialCommunicationManager.sendCommand(cg.pMoveUp());
			}
		});
		
		final int btnLaserX = btnMovePositionX + 200;
		final int btnLaserY = btnMovePositionY - 12;
		final int btnLaserW = 100;
		
		
		JPanel panel_2 = new JPanel();
		panel_2.setBorder(new TitledBorder(null, "Print Method", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_2.setBounds(411, 431, 248, 189);
		getContentPane().add(panel_2);
		panel_2.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		
		rbtnConvertMethod = new JRadioButton[ImageConverter.NUMBER_OF_PRINT_METHODS];
		for (int i = 0; i < rbtnConvertMethod.length; ++i) {
			rbtnConvertMethod[i] = new JRadioButton(ImageConverter.getPrintMethodName(i));
			rbtnConvertMethod[i].addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					mainWindow.changePrintMethod();
				}
			});
			buttonGroup.add(rbtnConvertMethod[i]);
			panel_2.add(rbtnConvertMethod[i]);
			
		}
		rbtnConvertMethod[ImageConverter.getChoicedPrintMethod()].setSelected(true);

		setVisible(true);
		
	}
}
