const int PIN_STEP_X = 6;
const int PIN_DIR_X = 5;
const int PIN_STEP_Y = 12;
const int PIN_DIR_Y = 11;
const int PIN_TEST = 13;

const int PIN_LASER_0 = 9;
const int PIN_LASER_1 = 10;

int DY[] = {-1, -1, 0, 1, 1, 1, 0, -1};
int DX[] = {0, -1, -1, -1, 0, 1, 1, 1};

namespace LaserCutterControl {
    const int MOTOR_TURING_ERROR_X = 9;
    const int MOTOR_TURING_ERROR_Y = 15;

    bool enableErrorCorrection = true;

    int lastMoveDirX = 0;
    int lastMoveDirY = 0;

    int currentPositionX = 0;
    int currentPositionY = 0;
    int currentLaserBrightness = 0;

    //basic arduino functions
    void step(int dir);
    void steps(int dly, int dir0, int dir1, int dir2, int dir3, 
            int dir4, int dir5, int dir6, int dir7, int dir8);
    void reset();
    void report();
    void move(int mot, int len, int dly);
    void line(int mot, int len, int dly, int brightness);
    void dot(int dly, int brightness);
    void laser(int brightness);
    void wait(int time);
    void test(int times);
    void hardstep(int dir, int times);
    void correction(int state);

    //high level ardiono functions
    //...
}
//char functions

bool isSignedNumber(char c) {
    if (c >= '0' && c <= '9') return true;
    if (c == '-') return true;
    return false;
}
bool isAlpha(char c) {
    if (c >= 'a' && c <= 'z') return true;
    if (c >= 'A' && c <= 'Z') return true;
    return false;
}
bool isTerminal(char c) {
    if (c == ';') return true;
    return false;
}
bool isValidChar(char c) {
    if (c == ' ') return true;
    if (c == ';') return true;
    if (isAlpha(c)) return true;
    if (isSignedNumber(c)) return true;
    return false;
}
char toUpperCase(char c) {
    if (c >= 'a' && c <= 'z')
        c = c - 'a' + 'A';
    return c;
}


char buffer[64];
int bbeg, bend; //close on front, open on end

char pop() {
    char c = buffer[bbeg++];
    bbeg &= 63;
    return c;
}

void depop() {
    bbeg += 63;
    bbeg &= 63;
}

void push(char c) {
    buffer[bend++] = c;
    bend &= 63;
}

void clearBuffer() {
    bbeg = 0;
    bend = 0;
}

void setup() {
    clearBuffer();
    Serial.begin(9600);

    pinMode(PIN_STEP_X, OUTPUT);
    pinMode(PIN_DIR_X, OUTPUT);
    pinMode(PIN_STEP_Y, OUTPUT);
    pinMode(PIN_DIR_Y, OUTPUT);
    pinMode(PIN_LASER_0, OUTPUT);
    pinMode(PIN_LASER_1, OUTPUT);

    pinMode(PIN_TEST, OUTPUT);
    digitalWrite(PIN_TEST, LOW);

}

bool strCompare(char *s1, char *s2) {
    int i;
    for (i = 0; s1[i] && s2[i]; ++i) {
        if (s1[i] != s2[i])
            return false;
    }
    if (s1[i] != s2[i])
        return false;
    return true;
}

void parseCommand() {
    //parse cmd name
    char cmdName[32];
    int cnt = 0;

    for (char c = pop(); cnt <= 10; c = pop()) {
        if (c == ' ')
            break;
        if (isAlpha(c)) {
            cmdName[cnt++] = c;
            continue;
        }
        if (c == ';') {
            depop();
            break;
        }
        Serial.println("Command Error!");
        return;
    }
    cmdName[cnt] = 0;
   
    //parse params
    int params[10];
    int tparam = 0;
    bool mflag = false;
    bool nflag = false;
    cnt = 0;

    for (char c = pop(); cnt < 10; c = pop()) {
        if (isSignedNumber(c)) {
            nflag = true;
            if (c == '-')
                mflag = true;
            else 
                tparam = tparam * 10 + c - '0';
            continue;
        }
        if (c == ' ') {
            if (mflag)
                tparam = -tparam;
            params[cnt++] = tparam;
            tparam = 0;
            mflag = false;
            nflag = false;
            continue;
        }
        if (c == ';') {
            if (mflag)
                tparam = -tparam;
            if (nflag) {
                params[cnt++] = tparam;
                tparam = 0;
                mflag = false;
            }
            depop();
            break;
        }
        Serial.println("Command Error!");
        return;
    }

    if (pop() != ';') {
        Serial.println("Command Error!");
        return;        
    }

    Serial.print("Received [");
    Serial.print(cmdName);
    for (int i = 0; i < cnt; ++i) {
        Serial.print(" ");
        Serial.print(params[i]);
    }
    Serial.println("].");

    for (; cnt < 10; ++cnt)
        params[cnt] = -1;

    if (strCompare(cmdName, "STEP")) {
        LaserCutterControl::step(params[0]);
    } else if (strCompare(cmdName, "STEPS")) {
        LaserCutterControl::steps(params[0], params[1], params[2], params[3], params[4], params[5], params[6], params[7], params[8], params[9]);
    } else if (strCompare(cmdName, "LASER")) {
        LaserCutterControl::laser(params[0]);
    } else if (strCompare(cmdName, "MOVE")) {
        LaserCutterControl::move(params[0], params[1], params[2]);
    } else if (strCompare(cmdName, "LINE")) {
        LaserCutterControl::line(params[0], params[1], params[2], params[3]);
    } else if (strCompare(cmdName, "DOT")) {
        LaserCutterControl::dot(params[0], params[1]);
    } else if (strCompare(cmdName, "WAIT")) {
        LaserCutterControl::wait(params[0]);
    } else if (strCompare(cmdName, "TEST")) {
        LaserCutterControl::test(params[0]);
    } else if (strCompare(cmdName, "RESET")) {
        LaserCutterControl::reset();
    } else if (strCompare(cmdName, "REPORT")) {
        LaserCutterControl::report();
    } else if (strCompare(cmdName, "CORRECTION")) {
        LaserCutterControl::correction(params[0]);
    } else {
        Serial.println("Unsupported Command.");
    }
    Serial.println("Executed.");
}


void loop() {
    char c;
    while (Serial.available() > 0) {
        delay(2);
        c = toUpperCase((char)Serial.read());
        if (c == 0x0a || c == 0x0d) {
            clearBuffer();
            continue;
        }
        push(c);
        if (c == ';') {
            parseCommand();
            clearBuffer();
        }
    }
}

void digitalPulse(int pin) {
    digitalWrite(pin, LOW);
    delay(1);
    digitalWrite(pin, HIGH);
    delay(1);
}

void error(char *s) {
    Serial.println(s);
}

void LaserCutterControl::dot(int dly, int brightness) {
    if (dly <= 0)
        return;
    laser(brightness);
    delay(dly);
    laser(0);
}

void LaserCutterControl::line(int dir, int len, int dly, int brightness) {
    if (brightness <= 0) 
        brightness = 3;
    if (dly < 0) 
        dly = 0;
    laser(brightness);
    delay(dly);
    move(dir, len, dly);
    laser(0);

}
void LaserCutterControl::move(int dir, int len, int dly) {
    if (dly < 0) 
        dly = 0;
    for (int i = 0; i < len; ++i) {
        step(dir);
        delay(dly);
    }

}

void LaserCutterControl::hardstep(int dir, int times = 1) {
    if (DX[dir])
        digitalWrite(PIN_DIR_X, (DX[dir] == 1)); 
    if (DY[dir])
        digitalWrite(PIN_DIR_Y, (DY[dir] == 1)); 
    for (int i = 0; i < times; ++i) {
        if (DX[dir])
            digitalPulse(PIN_STEP_X);
        if (DY[dir])
            digitalPulse(PIN_STEP_Y);
    }

}

void LaserCutterControl::step(int dir) {
    if (dir < 0 || dir >= 8)
        return;
    if (enableErrorCorrection) {
        if (DX[dir] * lastMoveDirX == -1)
            hardstep((DX[dir] == 1) ? 6 : 2, MOTOR_TURING_ERROR_X);
        if (DY[dir] * lastMoveDirY == -1)
            hardstep((DY[dir] == 1) ? 4 : 0, MOTOR_TURING_ERROR_Y);
    }
    
    currentPositionX += DX[dir];
    currentPositionY += DY[dir];
    if (DX[dir]) {
        lastMoveDirX = DX[dir];
        digitalWrite(PIN_DIR_X, (DX[dir] == 1)); 
        digitalPulse(PIN_STEP_X);
    }
    if (DY[dir]) {
        lastMoveDirY = DY[dir];
        digitalWrite(PIN_DIR_Y, (DY[dir] == 1)); 
        digitalPulse(PIN_STEP_Y);
    }

}

void LaserCutterControl::steps(int dly, int dir0, int dir1, int dir2, int dir3, 
            int dir4, int dir5, int dir6, int dir7, int dir8) {
    if (dir0 < 0) return;
    step(dir0);
    delay(dly);
    if (dir1 < 0) return;
    step(dir1);
    delay(dly);
    if (dir2 < 0) return;
    step(dir2);
    delay(dly);
    if (dir3 < 0) return;
    step(dir3);
    delay(dly);
    if (dir4 < 0) return;
    step(dir4);
    delay(dly);
    if (dir5 < 0) return;
    step(dir5);
    delay(dly);
    if (dir6 < 0) return;
    step(dir6);
    delay(dly);
    if (dir7 < 0) return;
    step(dir7);
    delay(dly);
    if (dir8 < 0) return;
    step(dir8);
    delay(dly);
}

void LaserCutterControl::laser(int brightness) {
    if (brightness > 3 || brightness < 0)
        return;
    currentLaserBrightness = brightness;
    if (brightness & 1)
        digitalWrite(PIN_LASER_0, HIGH);
    else
        digitalWrite(PIN_LASER_0, LOW);
    if (brightness & 2)
        digitalWrite(PIN_LASER_1, HIGH);
    else
        digitalWrite(PIN_LASER_1, LOW);
}

void LaserCutterControl::reset() {
    currentPositionX = 0;
    currentPositionY = 0;
}

void LaserCutterControl::report() {
    Serial.print("Position: (");
    Serial.print(currentPositionX);
    Serial.print(", ");
    Serial.print(currentPositionY);
    Serial.println(")");
    Serial.print("Laser: ");
    Serial.print(currentLaserBrightness);
}

void LaserCutterControl::wait(int time) {
    delay(time);
}

void LaserCutterControl::test(int times) {
    laser(2);
    delay(1000);
    laser(0);

    for (int k = 0; k < times; ++k) {
        for (int i = 0; i < 500; ++i) {
            step(0);
            step(2);
        }
        for (int i = 0; i < 500; ++i) {
            step(4);
            step(6);
        }
    }
}

void LaserCutterControl::correction(int state) {
    if (state < 0) return;
    enableErrorCorrection = (bool)state;
    if (enableErrorCorrection)
        Serial.println("Error Corrention Enabled.");
    else       
        Serial.println("Error Corrention Disabled.");
}