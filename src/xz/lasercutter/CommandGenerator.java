package xz.lasercutter;

import java.util.Queue;

class CommandGenerator {
	public static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
	public static final int[] DY = {1, 1, 0, -1, -1, -1, 0, 1};
	
	private int curX;
	private int curY;
	private int curL;
	
	private boolean enableErrorCorrection = false;
	private int lastDirX = 0;
	private int lastDirY = 0;
	
	public CommandGenerator(int x, int y, int l) {
		curX = x;
		curY = y;
		curL = l;
	}
	
	public CommandGenerator() {
		this(0, 0, 0);
	}
	
	public void setCorrectionEnabled(boolean state) {
		enableErrorCorrection = state;
	}
	
	public int getX() {
		return curX;
	}
	
	public int getY() {
		return curY;
	}
	
	public int getLaser() {
		return curL;
	}
	
	private void recordLastDir(int dir) {
		if (DX[dir] != 0)
			lastDirX = DX[dir];
		if (DY[dir] != 0)
			lastDirY = DY[dir];
	}
	
	private String correction(Integer dirNext) {
		StringBuffer cmd = new StringBuffer();
		if (dirNext == null || !enableErrorCorrection)
			return cmd.toString();
		if (DX[dirNext] * lastDirX == -1) {
			if (DX[dirNext] == 1)
				cmd.append("MOVE 6 " + PropertyManager.MOTOR_TURNING_EPS_X + ";\n");
			else
				cmd.append("MOVE 2 " + PropertyManager.MOTOR_TURNING_EPS_X + ";\n");
		}
		if (DY[dirNext] * lastDirY == -1) {
			if (DY[dirNext] == 1)
				cmd.append("MOVE 4 " + PropertyManager.MOTOR_TURNING_EPS_Y + ";\n");
			else
				cmd.append("MOVE 0 " + PropertyManager.MOTOR_TURNING_EPS_Y + ";\n");
		}
		
		return cmd.toString();
	}
	
	public String cDot(int dly, int brt) {
		String cmd = "DOT " + dly + " " + brt + ";\n";
		curL = 0;
		return cmd;
	}
	
	public String cMove(int dir, int len, int dly) {
		// generate command even if len == 0
		// in case of arousing correction by arduino 
		if (len < 0) {
			len = -len;
			dir = (dir + 4) % 8;
		}
		StringBuffer cmd = new StringBuffer();
		cmd.append(correction(dir));
		cmd.append("MOVE " + dir + " " + len + " " + dly + ";\n");
		recordLastDir(dir);
		curX += DX[dir] * len;
		curY += DY[dir] * len;
		return cmd.toString();
	}
	
	public String cLine(int dir, int len, int dly, int brt) {
		if (len < 0) {
			len = -len;
			dir = (dir + 4) % 8;
		}
		StringBuffer cmd = new StringBuffer();
		cmd.append(correction(dir));
		cmd.append("LINE " + dir + " " + len + " " + dly + " " + brt + ";\n");
		recordLastDir(dir);
		curX += DX[dir] * len;
		curY += DY[dir] * len;
		return cmd.toString();
	}
	
	public String cSteps(int dly, Queue<Integer> q) {
		StringBuffer cmd = new StringBuffer();
		int cnt = 0;
		if (q.size() == 0) return "";
		cmd.append(correction(q.peek()));
		while (q.size() > 0) {
			if (cnt == 0)
				cmd.append("STEPS " + dly);
			cmd.append(" " + q.peek());
			recordLastDir(q.peek());
			curX += DX[q.peek()];
			curY += DY[q.peek()];
			q.remove();
			++cnt;
			String cor = null;
			if (q.size() > 0)
				cor = correction(q.peek());
			if ((cor != null && cor.length() > 0) || cnt == 9) {
				if (cnt < 9)
					cmd.append(" -1");
				cmd.append(";\n");
				cnt = 0;
				if (cor != null)
					cmd.append(cor);
			}
		}
		if (cnt > 0) {
			if (cnt < 8)
				cmd.append(" -1");
			cmd.append(";\n");
		}
		return cmd.toString();
	}
	
	public String cStep(int dir) {
		StringBuffer cmd = new StringBuffer();
		cmd.append(correction(dir));
		cmd.append("STEP " + dir + ";\n");
		recordLastDir(dir);
		curX += DX[dir];
		curY += DY[dir];
		return cmd.toString();
	}
	
	public String cLaser(int brt) {
		curL = brt;
		return "LASER " + brt + ";\n";
	}

	public String cReset() {
		curX = 0;
		curY = 0;
		curL = 0;
		lastDirX = 0;
		lastDirY = 0;
		return "RESET;\n";
	}

	public String cReport() {
		return "REPORT;\n";
	}
	
	public String cWait(int dly) {
		return "WAIT " + dly + ";\n";
	}
	
	public String pMoveTo(int ty, int tx) {
		StringBuffer cmd = new StringBuffer();
		if (tx != curX)
			cmd.append(cMove(2, tx - curX, 0));
		if (ty != curY)
			cmd.append(cMove(0, ty - curY, 0));
		return cmd.toString();
	}
	
	public String pMoveLeft() {
		StringBuffer cmd = new StringBuffer();
		cmd.append(cMove(6, 100, 0));
		return cmd.toString();
	}
	
	public String pMoveRight() {
		StringBuffer cmd = new StringBuffer();
		cmd.append(cMove(2, 100, 0));
		return cmd.toString();
	}
	
	public String pMoveUp() {
		StringBuffer cmd = new StringBuffer();
		cmd.append(cMove(0, 100, 0));
		return cmd.toString();
	}
	
	public String pMoveDown() {
		StringBuffer cmd = new StringBuffer();
		cmd.append(cMove(4, 100, 0));
		return cmd.toString();
	}
	
	public String pMargin() {
		StringBuffer cmd = new StringBuffer();
		cmd.append(cMove(0, 25, 0));
		cmd.append(cMove(2, 25, 0));
		cmd.append(cReset());
		return cmd.toString();
	}
	
	public String pLaserOff() {
		StringBuffer cmd = new StringBuffer();
		cmd.append(cLaser(0));
		return cmd.toString();
	}
	
	public String pLaserLow() {
		StringBuffer cmd = new StringBuffer();
		cmd.append(cLaser(2));
		return cmd.toString();
	}
	
	public String pLaserHigh() {
		StringBuffer cmd = new StringBuffer();
		cmd.append(cLaser(3));
		return cmd.toString();
	}	
}

