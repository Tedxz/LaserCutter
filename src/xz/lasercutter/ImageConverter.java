package xz.lasercutter;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import javax.imageio.ImageIO;

import static xz.lasercutter.PropertyManager.*;

public class ImageConverter {
	private static final PrintMethod PRINT_METHODS[] = {
		new PrintMethodPrintByLine(),
		new PrintMethodPrintByLineFaster(),
		new PrintMethodPrintByLineSnakelike(),
		new PrintMethodBlockEdging(),
		new PrintMethodBlockEdgingWithErrorCorrection()
	};
	public static final int NUMBER_OF_PRINT_METHODS = PRINT_METHODS.length;
	
	private static String picturePath;
	private static int colorThreshold = 100; 
	
	private static int choicedPrintMethod = 3;
	
	
	public static int getChoicedPrintMethod() {
		return choicedPrintMethod;
	}

	public static void choicePrintMethod(int printMethod) {
		ImageConverter.choicedPrintMethod = printMethod;
	}

	public static String getPrintMethodName(int index) {
		try {
			return PRINT_METHODS[index].getName();
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}
	}
	
	private static int brightness(int color) {
		int sum = 0;
		sum += color & 0xff;
		color >>= 8;
		sum += color & 0xff;
		color >>= 8;
		sum += color & 0xff;
		return sum / 3;
	}
	
	public static void setPictuerPath(String st) {
		picturePath = st;
	}
	
	private static void generateCommandFile(int bitmap[][]) {
		try {
			PRINT_METHODS[choicedPrintMethod].generatePrintCommandList(bitmap, PropertyManager.getTempCmdPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void processPicture() {
		//open file
		if (picturePath == null)
			return;
		File picFile = new File(picturePath);
		BufferedImage bi;
		try {
			bi = ImageIO.read(picFile);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		//file read
		int height = bi.getHeight();
		int width = bi.getWidth();
		
		if (height > MOTOR_MOVE_DISTANCE || width > MOTOR_MOVE_DISTANCE) {
			MainWindow.log("SYSTEM\t|Image is too large.");
			return ;
		}
		
		int pixelArray[] = new int[MOTOR_MOVE_DISTANCE * MOTOR_MOVE_DISTANCE];
		for (int i = 0; i < pixelArray.length; ++i)
			pixelArray[i] = 0xFFFFFFFF;
		int offset = (MOTOR_MOVE_DISTANCE - height) / 2 * MOTOR_MOVE_DISTANCE + (MOTOR_MOVE_DISTANCE - width) / 2;
		bi.getRGB(0, 0, width, height, pixelArray, offset, MOTOR_MOVE_DISTANCE);
		
		int bitmap[][] = new int[MOTOR_MOVE_DISTANCE][MOTOR_MOVE_DISTANCE];
		for (int i = 0; i < MOTOR_MOVE_DISTANCE; ++i)
			for (int j = 0; j < MOTOR_MOVE_DISTANCE; ++j) {
				if (brightness(pixelArray[i * MOTOR_MOVE_DISTANCE + j]) > colorThreshold)
					bitmap[i][j] = 0;
				else
					bitmap[i][j] = 1;
			}
		// Convert picture to bitmap
		for (int i = 0; i < MOTOR_MOVE_DISTANCE; ++i)
			for (int j = 0; j < MOTOR_MOVE_DISTANCE; ++j) {
				if (brightness(pixelArray[i * MOTOR_MOVE_DISTANCE + j]) > colorThreshold)
					pixelArray[i * MOTOR_MOVE_DISTANCE + j] = 0xffffffff;
				else
					pixelArray[i * MOTOR_MOVE_DISTANCE + j] = 0xff000000;
			}
		
		// output modified picture
		BufferedImage nbi = new BufferedImage(MOTOR_MOVE_DISTANCE, MOTOR_MOVE_DISTANCE, BufferedImage.TYPE_INT_ARGB);
		nbi.setRGB(0, 0, MOTOR_MOVE_DISTANCE, MOTOR_MOVE_DISTANCE, pixelArray, 0, MOTOR_MOVE_DISTANCE);
		
		File testPic = new File(PropertyManager.getTempPicPath());
		try {
			ImageIO.write(nbi, "png", testPic);
		} catch (IOException e) {
			e.printStackTrace();
			return ;
		}

		// generate command file
		generateCommandFile(bitmap);

	}

	private interface PrintMethod {
		void generatePrintCommandList(int bitmap[][], String path) throws IOException;
		String getName();
	}
	private static class PrintMethodPrintByLineFaster implements PrintMethod {
		private static String name = "Print by Line (Faster)";
		
		public String getName() {
			return name;
		}
		
		public void generatePrintCommandList(int bitmap[][], String path) throws IOException {
			// more faster than 1
			File cmdList = new File(path);
			cmdList.createNewFile();
			FileWriter fw = new FileWriter(cmdList.getAbsolutePath());
			BufferedWriter bw = new BufferedWriter(fw);
			
			int lineBegin = -1, preI = 0, preJ = 0;
			for (int i = 0; i < bitmap.length; ++i) {
				lineBegin = -1;
				for (int j = 0; j < bitmap[i].length; ++j) {
					if (bitmap[i][j] == 0)
						continue;
					// check if is on a start of a line, record the position, move laser here
					if (j == 0 || bitmap[i][j - 1] == 0) {
						lineBegin = j;
						int deltaI = i - preI, deltaJ = j - preJ;
						if (deltaI != 0)
							bw.write("MOVE 0 " + deltaI + " 0;\n");
						if (deltaJ > 0)
							bw.write("MOVE 2 " + deltaJ + " 0;\n");
						if (deltaJ < 0)
							bw.write("MOVE 6 " + (-deltaJ) + " 0;\n");
					}
					// check if is on a end of a line, draw the line
					if (j + 1 == bitmap[i].length || bitmap[i][j + 1] == 0) {
						 bw.write("DOT " + PropertyManager.getDrawDotDelay() + " " + PropertyManager.getDrawBrightness() + ";\n");
						if (j - lineBegin > 0)
							bw.write("LINE 2 " + (j - lineBegin) + " " + PropertyManager.getDrawLineDelay() + 
									" " + PropertyManager.getDrawBrightness() + ";\n");
						preI = i;
						preJ = j;
					}
					
				}
			}
			bw.write("MOVE 4 " + preI + " 0;\n");
			bw.write("MOVE 6 " + preJ + " 0;\n");
			
			bw.close();

		}
	}
	private static class PrintMethodPrintByLine implements PrintMethod {
		private static String name = "Print by Line";
		
		public String getName() {
			return name;
		}
		
		public void generatePrintCommandList(int bitmap[][], String path) throws IOException {
			// convert bitmap to machine commands, brute force
			File cmdList = new File(path);
			cmdList.createNewFile();
			FileWriter fw = new FileWriter(cmdList.getAbsolutePath());
			BufferedWriter bw = new BufferedWriter(fw);
			
			int len = 0;
			
			for (int i = 0; i < bitmap.length; ++i) {
				for (int j = 1; j < bitmap[i].length; ++j) {
					if (bitmap[i][j] == bitmap[i][j - 1])
						++len;
					else if (bitmap[i][j] == 1) {
						// 2 segments must separate with a space
						// write move command -
						bw.write("MOVE 2 " + len + 1 + " 0;\n"); //extra step
						// laser on (not needed)
						// dot command -
						len = 0;
					} else {
						// write draw command (move with delay, laser off) -
						bw.write("DOT " + PropertyManager.getDrawDotDelay() + " " + PropertyManager.getDrawBrightness() + ";\n");
						bw.write("LINE 2 " + len + " " + PropertyManager.getDrawLineDelay() + " " + PropertyManager.getDrawBrightness() + ";\n");
						bw.write("MOVE 2 1 0;\n"); // extra step
						len = 0;
					}
				}
				// write uncompleted command
				if (bitmap[i][bitmap[i].length - 1] == 0) {
					bw.write("MOVE 2 " + len + " 0;\n");
				} else {
					bw.write("DOT " + PropertyManager.getDrawDotDelay() + " " + PropertyManager.getDrawBrightness() + ";\n");
					bw.write("LINE 2 " + len + " " + PropertyManager.getDrawLineDelay() + " " + PropertyManager.getDrawBrightness() + ";\n");
				}
				// return
				bw.write("MOVE 6 1399 0;\n");
				// new line
				bw.write("MOVE 0 1 0;\n");
			}
			bw.close();
		}
	}	
	private static class PrintMethodPrintByLineSnakelike implements PrintMethod {
		private static String name = "Print by Line (Snakelike)";
		
		public String getName() {
			return name;
		}
		
		public void generatePrintCommandList(int bitmap[][], String path) throws IOException {
			// snake shape, watch if error exists
			File cmdList = new File(path);
			cmdList.createNewFile();
			FileWriter fw = new FileWriter(cmdList.getAbsolutePath());
			BufferedWriter bw = new BufferedWriter(fw);
			
			int lineBegin = -1, preI = 0, preJ = 0;
			for (int i = 0; i < bitmap.length; ++i) {
				lineBegin = -1;
				if ((1 & i) == 1) {
					for (int j = bitmap[i].length - 1; j >= 0; --j) {
						if (bitmap[i][j] == 0)
							continue;
						if (j == bitmap[i].length - 1 || bitmap[i][j + 1] == 0) {
							lineBegin = j;
							int deltaI = i - preI, deltaJ = j - preJ;
							if (deltaI != 0)
								bw.write("MOVE 0 " + deltaI + " 0;\n");
							if (deltaJ > 0)
								bw.write("MOVE 2 " + deltaJ + " 0;\n");
							if (deltaJ < 0)
								bw.write("MOVE 6 " + (-deltaJ) + " 0;\n");
						}
						if (j == 0 || bitmap[i][j - 1] == 0) {
							bw.write("DOT " + PropertyManager.getDrawDotDelay() + " " + PropertyManager.getDrawBrightness() + ";\n");
							if (j - lineBegin < 0)
								bw.write("LINE 6 " + (lineBegin - j) + " " + PropertyManager.getDrawLineDelay() + 
										" " + PropertyManager.getDrawBrightness() + ";\n");
							preI = i;
							preJ = j;
						}
					}
				} else {
					for (int j = 0; j < bitmap[i].length; ++j) {
						if (bitmap[i][j] == 0)
							continue;
						if (j == 0 || bitmap[i][j - 1] == 0) {
							lineBegin = j;
							int deltaI = i - preI, deltaJ = j - preJ;
							if (deltaI != 0)
								bw.write("MOVE 0 " + deltaI + " 0;\n");
							if (deltaJ > 0)
								bw.write("MOVE 2 " + deltaJ + " 0;\n");
							if (deltaJ < 0)
								bw.write("MOVE 6 " + (-deltaJ) + " 0;\n");
						}
						if (j + 1 == bitmap[i].length || bitmap[i][j + 1] == 0) {
							bw.write("DOT " + PropertyManager.getDrawDotDelay() + " " + PropertyManager.getDrawBrightness() + ";\n");
							if (j - lineBegin > 0)
								bw.write("LINE 2 " + (j - lineBegin) + " " + PropertyManager.getDrawLineDelay() + 
										" " + PropertyManager.getDrawBrightness() + ";\n");
							preI = i;
							preJ = j;
						}
						
					}
				}
			}
			bw.write("MOVE 4 " + preI + " 0;\n");
			bw.write("MOVE 6 " + preJ + " 0;\n");
			
			bw.close();
		}
	}
	private static class PrintMethodBlockEdgingWithErrorCorrection implements PrintMethod {
		private static String name = "Block Edging (Error Correction)";
		
		public String getName() {
			return name;
		}
		
		public void generatePrintCommandList(int bitmap[][], String path) throws IOException {
			// edging with error compensation
			File cmdList = new File(path);
			cmdList.createNewFile();
			FileWriter fw = new FileWriter(cmdList.getAbsolutePath());
			BufferedWriter bw = new BufferedWriter(fw);
			
			int bitmapBackup[][] = new int[bitmap.length][];
			for (int i = 0; i < bitmapBackup.length; ++i)
				bitmapBackup[i] = Arrays.copyOf(bitmap[i], bitmap[i].length);
			
			bitmap = bitmapBackup;
			
			int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
			int[] dy = {0, 1, 1, 1, 0, -1, -1, -1};
			int x = 0, y = 0;
			int lastDX = 1, lastDY = 1;
			for (int i = 0; i < bitmap.length; ++i) {
				for (int j = 0, k; j < bitmap[i].length; ++j) {
					if (bitmap[i][j] == 1) {
						int dir = 2;
						// move here and laser on(xy -> ij), delay
						int deltaI = i - x, deltaJ = j - y;
						if (deltaI > 0) {
							if (lastDX == -1) 
								deltaI += PropertyManager.MOTOR_TURNING_EPS_X;
							bw.write("MOVE 0 " + deltaI + " 0;\n");
							lastDX = 1;
						}
						if (deltaI < 0) {
							if (lastDX == 1) 
								deltaI -= PropertyManager.MOTOR_TURNING_EPS_X;
							bw.write("MOVE 4 " + (-deltaI) + " 0;\n");
							lastDX = -1;
						}
						if (deltaJ > 0) {
							if (lastDY == -1) 
								deltaJ += PropertyManager.MOTOR_TURNING_EPS_Y;
							bw.write("MOVE 2 " + deltaJ + " 0;\n");
							lastDY = 1;
						}
						if (deltaJ < 0) {
							if (lastDY == 1) 
								deltaJ -= PropertyManager.MOTOR_TURNING_EPS_Y;
							bw.write("MOVE 6 " + (-deltaJ) + " 0;\n");
							lastDY = -1;
						}
						bw.write("LASER " + PropertyManager.getDrawBrightness() + ";\n");
						bw.write("WAIT " + PropertyManager.getDrawDotDelay() + ";\n");
						// steps command should not delay on the start, because of the need of command consequence
						Queue<Integer> q = new LinkedList<Integer>();
						for (x = i, y = j; ; ) {
							int tx = x, ty = y;
							bitmap[x][y] = 0;
							for (k = 0, dir = (dir + 5) % 8; k < 8; ++k, dir = (dir + 1) % 8) {
								tx = x + dx[dir]; ty = y + dy[dir];
								if (tx >= 0 && tx < bitmap.length && ty > 0 && ty < bitmap[i].length && bitmap[tx][ty] != 0)
									break;
							}
							if (tx >= 0 && tx < bitmap.length && ty > 0 && ty < bitmap[i].length && bitmap[tx][ty] != 0) {
								x = tx; y = ty;
								q.add(dir);
							} else {
								// if queue is not empty, generate a command, end with -1
								if (q.size() > 0) {
									// write some steps command with error compensation
									while (q.size() > 0) {
										
										// if x changes direction, compensate x
										if (dx[q.peek()] * lastDX == -1) {
											if (lastDX == -1)
												bw.write("MOVE 0 " + PropertyManager.MOTOR_TURNING_EPS_X + ";\n");
											else
												bw.write("MOVE 4 " + PropertyManager.MOTOR_TURNING_EPS_X + ";\n");
										}
										// if y changes direction, compensate y
										if (dy[q.peek()] * lastDY == -1) {
											if (lastDY == -1)
												bw.write("MOVE 2 " + PropertyManager.MOTOR_TURNING_EPS_Y + ";\n");
											else
												bw.write("MOVE 6 " + PropertyManager.MOTOR_TURNING_EPS_Y + ";\n");
										}
										// write a steps command with the first step
										bw.write("STEPS " + PropertyManager.getDrawLineDelay() + " " + q.peek());
										if (dx[q.peek()] != 0)
											lastDX = dx[q.peek()];
										if (dy[q.peek()] != 0)
											lastDY = dy[q.peek()];
										q.remove();
										// loop 8 times, add steps, break when any direction changes
										int p;
										for (p = 0; q.size() > 0 && p < 8; ++p) {
											if (lastDY * dy[q.peek()] == -1 || lastDX * dx[q.peek()] == -1)
												break;
											bw.write(" " + q.peek());
											if (dx[q.peek()] != 0)
												lastDX = dx[q.peek()];
											if (dy[q.peek()] != 0)
												lastDY = dy[q.peek()];
											q.remove();
										}
										// enclose the command
										if (p == 8)
											bw.write(";\n");
										else
											bw.write(" -1;\n");
									}
								}
								// laser off
								bw.write("LASER 0;\n");
								break;
							}
						}
					}
				}
			}
			bw.write("MOVE 4 " + x + " 0;\n");
			bw.write("MOVE 6 " + y + " 0;\n");
			
			bw.close();
		}
	}
	private static class PrintMethodBlockEdging implements PrintMethod {
		private static String name = "Block Edging";
		
		public String getName() {
			return name;
		}
		
		public void generatePrintCommandList(int bitmap[][], String path) throws IOException {
			// edging with error compensation
			File cmdList = new File(path);
			cmdList.createNewFile();
			FileWriter fw = new FileWriter(cmdList.getAbsolutePath());
			BufferedWriter bw = new BufferedWriter(fw);
			
			int bitmapBackup[][] = new int[bitmap.length][];
			for (int i = 0; i < bitmapBackup.length; ++i)
				bitmapBackup[i] = Arrays.copyOf(bitmap[i], bitmap[i].length);
			
			bitmap = bitmapBackup;
			
			int[] dx = {1, 1, 0, -1, -1, -1, 0, 1};
			int[] dy = {0, 1, 1, 1, 0, -1, -1, -1};
			int x = 0, y = 0;
			int lastDX = 1, lastDY = 1;
			for (int i = 0; i < bitmap.length; ++i) {
				for (int j = 0, k; j < bitmap[i].length; ++j) {
					if (bitmap[i][j] == 1) {
						int dir = 2;
						// move here and laser on(xy -> ij), delay
						int deltaI = i - x, deltaJ = j - y;
						if (deltaI > 0) {
							if (lastDX == -1) 
								deltaI += PropertyManager.MOTOR_TURNING_EPS_X;
							bw.write("MOVE 0 " + deltaI + " 0;\n");
							lastDX = 1;
						}
						if (deltaI < 0) {
							if (lastDX == 1) 
								deltaI -= PropertyManager.MOTOR_TURNING_EPS_X;
							bw.write("MOVE 4 " + (-deltaI) + " 0;\n");
							lastDX = -1;
						}
						if (deltaJ > 0) {
							if (lastDY == -1) 
								deltaJ += PropertyManager.MOTOR_TURNING_EPS_Y;
							bw.write("MOVE 2 " + deltaJ + " 0;\n");
							lastDY = 1;
						}
						if (deltaJ < 0) {
							if (lastDY == 1) 
								deltaJ -= PropertyManager.MOTOR_TURNING_EPS_Y;
							bw.write("MOVE 6 " + (-deltaJ) + " 0;\n");
							lastDY = -1;
						}
						bw.write("LASER " + PropertyManager.getDrawBrightness() + ";\n");
						bw.write("WAIT " + PropertyManager.getDrawDotDelay() + ";\n");
						// steps command should not delay on the start, because of the need of command consequence
						Queue<Integer> q = new LinkedList<Integer>();
						for (x = i, y = j; ; ) {
							int tx = x, ty = y;
							bitmap[x][y] = 0;
							for (k = 0, dir = (dir + 5) % 8; k < 8; ++k, dir = (dir + 1) % 8) {
								tx = x + dx[dir]; ty = y + dy[dir];
								if (tx >= 0 && tx < bitmap.length && ty > 0 && ty < bitmap[i].length && bitmap[tx][ty] != 0)
									break;
							}
							if (tx >= 0 && tx < bitmap.length && ty > 0 && ty < bitmap[i].length && bitmap[tx][ty] != 0) {
								x = tx; y = ty;
								q.add(dir);
							} else {
								// if queue is not empty, generate a command, end with -1
								if (q.size() > 0) {
									// write some steps command with error compensation
									while (q.size() > 0) {
										// write a steps command with the first step
										bw.write("STEPS " + PropertyManager.getDrawLineDelay() + " " + q.peek());
										if (dx[q.peek()] != 0)
											lastDX = dx[q.peek()];
										if (dy[q.peek()] != 0)
											lastDY = dy[q.peek()];
										q.remove();
										// loop 8 times, add steps, break when any direction changes
										int p;
										for (p = 0; q.size() > 0 && p < 8; ++p) {
											if (lastDY * dy[q.peek()] == -1 || lastDX * dx[q.peek()] == -1)
												break;
											bw.write(" " + q.peek());
											if (dx[q.peek()] != 0)
												lastDX = dx[q.peek()];
											if (dy[q.peek()] != 0)
												lastDY = dy[q.peek()];
											q.remove();
										}
										// enclose the command
										if (p == 8)
											bw.write(";\n");
										else
											bw.write(" -1;\n");
									}
								}
								// laser off
								bw.write("LASER 0;\n");
								break;
							}
						}
					}
				}
			}
			bw.write("MOVE 4 " + x + " 0;\n");
			bw.write("MOVE 6 " + y + " 0;\n");
			
			bw.close();
		}
	}
	
}

