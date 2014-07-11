package xz.lasercutter;

import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.imageio.ImageIO;

import static xz.lasercutter.PropertyManager.*;
import static xz.lasercutter.CommandGenerator.*;

public class ImageConverter {
	private static final PrintMethod PRINT_METHODS[] = {
		new PrintMethodPrintByLine(),
		new PrintMethodPrintByLineFaster(),
		new PrintMethodBlockEdging(),
		new PrintMethodBlockEdgingWithErrorCorrection(),
		new PrintMethodBlockEdgingWithDenoise()
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
			
			CommandGenerator cg = new CommandGenerator();
			bw.write(cg.cReset());
			
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
							bw.write(cg.cMove(0, deltaI, 0));
						if (deltaJ > 0)
							bw.write(cg.cMove(2, deltaJ, 0));
						if (deltaJ < 0)
							bw.write(cg.cMove(6, -deltaJ, 0));
					}
					// check if is on a end of a line, draw the line
					if (j + 1 == bitmap[i].length || bitmap[i][j + 1] == 0) {
						bw.write(cg.cDot(PropertyManager.getDrawDotDelay(), PropertyManager.getDrawBrightness()));
						if (j - lineBegin > 0)
							bw.write(cg.cLine(2, j - lineBegin, PropertyManager.getDrawLineDelay(), PropertyManager.getDrawBrightness()));
						preI = i;
						preJ = j;
					}				
				}
			}
			bw.write(cg.cMove(4, preI, 0));
			bw.write(cg.cMove(6, preJ, 0));
			bw.write(cg.cReport());
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
			
			CommandGenerator cg = new CommandGenerator();
			bw.write(cg.cReset());
			
			int len = 0;
			
			for (int i = 0; i < bitmap.length; ++i) {
				for (int j = 1; j < bitmap[i].length; ++j) {
					if (bitmap[i][j] == bitmap[i][j - 1])
						++len;
					else if (bitmap[i][j] == 1) {
						// 2 segments must separate with an extra space
						bw.write(cg.cMove(2, len + 1, 0));
						len = 0;
					} else {
						// write draw command (move with delay, laser off) -
						bw.write(cg.cDot(PropertyManager.getDrawDotDelay(), PropertyManager.getDrawBrightness()));
						bw.write(cg.cLine(2, len, PropertyManager.getDrawLineDelay(), PropertyManager.getDrawBrightness()));
						bw.write(cg.cMove(2, 1, 0)); // extra step
						len = 0;
					}
				}
				// write uncompleted command
				if (bitmap[i][bitmap[i].length - 1] == 0) {
					bw.write(cg.cMove(2, len, 0));
				} else {
					bw.write(cg.cDot(PropertyManager.getDrawDotDelay(), PropertyManager.getDrawBrightness()));
					bw.write(cg.cLine(2, len, PropertyManager.getDrawLineDelay(), PropertyManager.getDrawBrightness()));
				}
				// return
				bw.write(cg.cMove(6, 1399, 0));
				// new line
				bw.write(cg.cMove(0, 1, 0));
			}
			bw.write(cg.cReport());
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
			
			CommandGenerator cg = new CommandGenerator();
			cg.setCorrectionEnabled(true);
			bw.write(cg.cReset());
			
			int bitmapBackup[][] = new int[bitmap.length][];
			for (int i = 0; i < bitmapBackup.length; ++i)
				bitmapBackup[i] = Arrays.copyOf(bitmap[i], bitmap[i].length);
			
			bitmap = bitmapBackup;
			
			int y = 0, x = 0;
			for (int i = 0; i < bitmap.length; ++i) {
				for (int j = 0, k; j < bitmap[i].length; ++j) {
					if (bitmap[i][j] == 1) {
						int dir = 2;
						// move here and laser on(yx -> ij), delay
						bw.write(cg.pMoveTo(i, j));
						bw.write(cg.cLaser(PropertyManager.getDrawBrightness()));
						bw.write(cg.cWait(PropertyManager.getDrawDotDelay()));
						// steps command should not delay on the start, because of the need of command consequence
						Queue<Integer> q = new LinkedList<Integer>();
						for (y = i, x = j; ; ) {
							int ty = y, tx = x;
							bitmap[y][x] = 0;
							for (k = 0, dir = (dir + 5) % 8; k < 8; ++k, dir = (dir + 1) % 8) {
								ty = y + DY[dir]; tx = x + DX[dir];
								if (ty >= 0 && ty < bitmap.length && tx > 0 && tx < bitmap[i].length && bitmap[ty][tx] != 0)
									break;
							}
							if (ty >= 0 && ty < bitmap.length && tx > 0 && tx < bitmap[i].length && bitmap[ty][tx] != 0) {
								y = ty; x = tx;
								q.add(dir);
							} else {
								// if queue is not empty, generate a command, end with -1
								if (q.size() > 0) {
									// write some steps command with error compensation
									bw.write(cg.cSteps(PropertyManager.getDrawLineDelay(), q));
								}
								// laser off
								bw.write(cg.cLaser(0));
								break;
							}
						}
					}
				}
			}
			bw.write(cg.pMoveTo(0, 0));
			bw.write(cg.cMove(0, 0, 0));
			bw.write(cg.cMove(2, 0, 0));
			bw.write(cg.cReport());
			bw.close();
		}
	}
	private static class PrintMethodBlockEdging implements PrintMethod {
		private static String name = "Block Edging";
		
		public String getName() {
			return name;
		}
		
		public void generatePrintCommandList(int bitmap[][], String path) throws IOException {
			// edging without error compensation
			File cmdList = new File(path);
			cmdList.createNewFile();
			FileWriter fw = new FileWriter(cmdList.getAbsolutePath());
			BufferedWriter bw = new BufferedWriter(fw);
			
			CommandGenerator cg = new CommandGenerator();
			bw.write(cg.cReset());
			
			int bitmapBackup[][] = new int[bitmap.length][];
			for (int i = 0; i < bitmapBackup.length; ++i)
				bitmapBackup[i] = Arrays.copyOf(bitmap[i], bitmap[i].length);
			
			bitmap = bitmapBackup;
			
			int y = 0, x = 0;
			for (int i = 0; i < bitmap.length; ++i) {
				for (int j = 0, k; j < bitmap[i].length; ++j) {
					if (bitmap[i][j] == 1) {
						int dir = 2;
						// move here and laser on(yx -> ij), delay
						bw.write(cg.pMoveTo(i, j));
						bw.write(cg.cLaser(PropertyManager.getDrawBrightness()));
						bw.write(cg.cWait(PropertyManager.getDrawDotDelay()));
						// steps command should not delay on the start, because of the need of command consequence
						Queue<Integer> q = new LinkedList<Integer>();
						for (y = i, x = j; ; ) {
							int ty = y, tx = x;
							bitmap[y][x] = 0;
							for (k = 0, dir = (dir + 5) % 8; k < 8; ++k, dir = (dir + 1) % 8) {
								ty = y + DY[dir]; tx = x + DX[dir];
								if (ty >= 0 && ty < bitmap.length && tx > 0 && tx < bitmap[i].length && bitmap[ty][tx] != 0)
									break;
							}
							if (ty >= 0 && ty < bitmap.length && tx > 0 && tx < bitmap[i].length && bitmap[ty][tx] != 0) {
								y = ty; x = tx;
								q.add(dir);
							} else {
								// if queue is not empty, generate a command, end with -1
								if (q.size() > 0) {
									// write some steps command
									bw.write(cg.cSteps(PropertyManager.getDrawLineDelay(), q));
								}
								// laser off, cannot be moved into the above if
								bw.write(cg.cLaser(0));
								break;
							}
						}
					}
				}
			}
			bw.write(cg.pMoveTo(0, 0));
			bw.write(cg.cReport());
			bw.close();
		}
	}
	
	private static class PrintMethodBlockEdgingWithDenoise implements PrintMethod {
		private static String name = "Block Edging (Denoise)";
		
		public String getName() {
			return name;
		}
		
		public void generatePrintCommandList(int bitmap[][], String path) throws IOException {
			// implemented by modifying from BE
			File cmdList = new File(path);
			cmdList.createNewFile();
			FileWriter fw = new FileWriter(cmdList.getAbsolutePath());
			BufferedWriter bw = new BufferedWriter(fw);
			
			CommandGenerator cg = new CommandGenerator();
			bw.write(cg.cReset());
			
			int bitmapBackup[][] = new int[bitmap.length][];
			for (int i = 0; i < bitmapBackup.length; ++i)
				bitmapBackup[i] = Arrays.copyOf(bitmap[i], bitmap[i].length);
			
			bitmap = bitmapBackup;
			
			int y = 0, x = 0;
			for (int i = 0; i < bitmap.length; ++i) {
				for (int j = 0, k; j < bitmap[i].length; ++j) {
					if (bitmap[i][j] == 1) {
						// steps command should not delay on the start, because of the need of command consequence
						Queue<Integer> q = new LinkedList<Integer>();
						for (int by = i, bx = j, dir = 2; ; ) {
							int ty = by, tx = bx;
							bitmap[by][bx] = 0;
							for (k = 0, dir = (dir + 5) % 8; k < 8; ++k, dir = (dir + 1) % 8) {
								ty = by + DY[dir]; tx = bx + DX[dir];
								if (ty >= 0 && ty < bitmap.length && tx > 0 && tx < bitmap[i].length && bitmap[ty][tx] != 0)
									break;
							}
							if (ty >= 0 && ty < bitmap.length && tx > 0 && tx < bitmap[i].length && bitmap[ty][tx] != 0) {
								by = ty; bx = tx;
								q.add(dir);
							} else {
								// if queue is not empty, generate a command, end with -1
								if (q.size() > 5) {
									// move here and laser on(yx -> ij), delay
									bw.write(cg.pMoveTo(i, j));
									bw.write(cg.cLaser(PropertyManager.getDrawBrightness()));
									bw.write(cg.cWait(PropertyManager.getDrawDotDelay()));
									// write some steps command
									bw.write(cg.cSteps(PropertyManager.getDrawLineDelay(), q));
									bw.write(cg.cLaser(0));
									// update currect position
									y = by; x = bx;
								}
								break;
							}
						}
					}
				}
			}
			bw.write(cg.pMoveTo(0, 0));
			bw.write(cg.cReport());
			bw.close();
		}
	}
	
}

