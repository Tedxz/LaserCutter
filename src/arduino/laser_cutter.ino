const int PIN_STEP_X = 6;
const int PIN_DIR_X = 5;
const int PIN_STEP_Y = 12;
const int PIN_DIR_Y = 11;
const int PIN_TEST = 13;

const int PIN_LASER_0 = 9;
const int PIN_LASER_1 = 10;

namespace LaserCutterControl {
    //basic arduino functions
    void step(int dir);
    void steps(int dly, int dir0, int dir1, int dir2, int dir3, 
            int dir4, int dir5, int dir6, int dir7, int dir8);
    void reset();
    void move(int mot, int len, int dly);
    void line(int mot, int len, int dly, int brightness);
    void dot(int dly, int brightness);
    void laser(int brightness);
    void wait(int time);
    void test(int times);

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

bool strCompaer(char *s1, char *s2) {
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
    char cmdName[10];
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
        params[cnt] = 0;

    if (strCompaer(cmdName, "STEP")) {
        LaserCutterControl::step(params[0]);
    } else if (strCompaer(cmdName, "STEPS")) {
        LaserCutterControl::steps(params[0], params[1], params[2], params[3], params[4], params[5], params[6], params[7], params[8], params[9]);
    } else if (strCompaer(cmdName, "LASER")) {
        LaserCutterControl::laser(params[0]);
    } else if (strCompaer(cmdName, "MOVE")) {
        LaserCutterControl::move(params[0], params[1], params[2]);
    } else if (strCompaer(cmdName, "LINE")) {
        LaserCutterControl::line(params[0], params[1], params[2], params[3]);
    } else if (strCompaer(cmdName, "DOT")) {
        LaserCutterControl::dot(params[0], params[1]);
    } else if (strCompaer(cmdName, "WAIT")) {
        LaserCutterControl::wait(params[0]);
    } else if (strCompaer(cmdName, "TEST")) {
        LaserCutterControl::test(params[0]);
    } else if (strCompaer(cmdName, "RESET")) {
        LaserCutterControl::reset();
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

void LaserCutterControl::dot(int dly, int brightness) {
    if (dly == 0)
        return;
    laser(brightness);
    delay(dly);
    laser(0);
}

void LaserCutterControl::line(int dir, int len, int dly, int brightness) {
    if (brightness == 0) brightness = 3;
    laser(brightness);
    delay(dly);
    move(dir, len, dly);
    laser(0);

}
void LaserCutterControl::move(int dir, int len, int dly) {
    for (int i = 0; i < len; ++i) {
        step(dir);
        delay(dly);
    }

}

void LaserCutterControl::step(int dir) {
    //set dir, pulse step
    switch (dir) {
        case 0:
            digitalWrite(PIN_DIR_Y, LOW);
            digitalPulse(PIN_STEP_Y);
            break;
        case 1:
            step(0);
            step(2);
            // digitalWrite(PIN_DIR_Y, LOW);
            // digitalWrite(PIN_DIR_X, LOW);
            // digitalPulse(PIN_STEP_Y);
            // digitalPulse(PIN_STEP_X);
            break;
        case 2:
            digitalWrite(PIN_DIR_X, LOW);
            digitalPulse(PIN_STEP_X);
            break;
        case 3:
            step(2);
            step(4);
            // digitalWrite(PIN_DIR_X, LOW);
            // digitalWrite(PIN_DIR_Y, HIGH);
            // digitalPulse(PIN_STEP_X);
            // digitalPulse(PIN_STEP_Y);
            break;
        case 4:
            digitalWrite(PIN_DIR_Y, HIGH);
            digitalPulse(PIN_STEP_Y);
            break;
        case 5:
            step(4);
            step(6);
            // digitalWrite(PIN_DIR_Y, HIGH);
            // digitalWrite(PIN_DIR_X, HIGH);
            // digitalPulse(PIN_STEP_Y);
            // digitalPulse(PIN_STEP_X);
            break;
        case 6:
            digitalWrite(PIN_DIR_X, HIGH);
            digitalPulse(PIN_STEP_X);
            break;
        case 7:
            step(6);
            step(0);
            // digitalWrite(PIN_DIR_X, HIGH);
            // digitalWrite(PIN_DIR_Y, LOW);
            // digitalPulse(PIN_STEP_X);
            // digitalPulse(PIN_STEP_Y);
            break;
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
    if (brightness > 3)
        return;
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
    for (int i = 0; i < 100; ++i) {
        step(4);
        step(6);
    }
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
