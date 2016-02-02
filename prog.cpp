#include <iostream>
#include <fstream>
#include <string>
#include <cstdio>
#include <cstring>
#include <list>
#include <vector>
#include <tuple>
#include <set>
#include <cmath>
#include <algorithm>

#define PIN_COUNT 40

using std::vector;
using std::list;
using std::cin;
using std::cout;
using std::endl;
using std::ifstream;
using std::string;
using std::pair;
using std::make_pair;
using std::set;

struct MCU_t {
    float x_coords[PIN_COUNT], 
          y_coords[PIN_COUNT];

    int connection[PIN_COUNT];

    void connect(int pin, int target) {
        connection[pin] = target;
    }

    void put(int pos, float x, float y) {
        x_coords[pos] = x;
        y_coords[pos] = y;
    }
};

struct MEM_t {
    float x_coords[PIN_COUNT], 
          y_coords[PIN_COUNT];

    int connection[PIN_COUNT];

    void connect(int pin, int target) {
        connection[pin] = target;
    }

    void put(int pos, float x, float y) {
        x_coords[pos] = x;
        y_coords[pos] = y;
    }
};

struct vec2 {
    float x;
    float y;

    vec2(float _x, float _y): x(_x), y(_y) {}
};

struct cmp
{
    bool operator()(const pair<int, double> a, const pair<int, double> b)
    {
        return a.second < b.second;
    }
};


double length(vec2 a, vec2 b) {
    return sqrt(pow(a.x-b.x, 2) + pow(a.y-b.y, 2));
}

void readCoords(MCU_t &MCU, MEM_t &MEM) {

    int pin, controller;
    float x, y;
    char s[1024];
    char *token;

    string f1, f2;

    FILE *input = fopen("coordinates.csv", "r");

    while (!feof(input)) {
        fscanf(input, "%d;%d;%s", &pin, &controller, s);
        f1 = strtok(s, ";");
        f2 = strtok(NULL, ";");

        int pos = f1.find(',');
        if (pos != std::string::npos)
            f1.replace(pos, 1, 1, '.');

        pos = f2.find(',');
        if (pos != std::string::npos)
            f2.replace(pos, 1, 1, '.');

        if (controller == 1)
            MCU.put(pin-1, stof(f1), stof(f2));
        else
            MEM.put(pin-1, stof(f1), stof(f2));
        memset(s, 0, 1024);
    }

    for (int i = PIN_COUNT/2; i < PIN_COUNT; ++i) {
        MEM.y_coords[i] += 0.3f;
    }

    fclose(input);
}

void readConnections(MCU_t &MCU, MEM_t &MEM) {
    FILE *input = fopen("connect.csv", "r");

    int mcu_pin, mem_pin;

    while (!feof(input)) {
        fscanf(input, "%d;%d\n", &mcu_pin, &mem_pin);
        mcu_pin -= 1;
        mem_pin -= 1;

        MCU.connect(mcu_pin, mem_pin);
        MEM.connect(mem_pin, mcu_pin);
    }

    fclose(input); 
}

void calculateLengths(MCU_t &MCU, MEM_t &MEM) {
    vector<pair<int, double>> lengths;

    for (int i = 0; i < PIN_COUNT; ++i) {
        vec2 a(MEM.x_coords[i], MEM.y_coords[i]);

        int idx = MEM.connection[i];
        vec2 b(MCU.x_coords[idx], MCU.y_coords[idx]);

        lengths.push_back(make_pair(i, length(a, b)));
    }

    std::sort(lengths.begin(), lengths.end(), cmp());

    for (auto elem: lengths) {
        cout << elem.first << ' ' << elem.second << endl;
    }
}

/*
set<int> Dive(vector<pair<float, int>> &x_coords, set<int> _set, int cur) {
    if (cur == PIN_COUNT - 1) {
        return _set;
    }

    set<int> max_set;

    for (int i = cur + 1; i < PIN_COUNT; ++i) {
        if (x_coords[i].first > x_coords[cur].first) {
            set<int> new_set = _set;
            new_set.insert(i);
            new_set = Dive(x_coords, new_set, i);

            if (max_set.size() < new_set.size())
                max_set = new_set;
        }
    }

    return max_set;
}
*/

/*
int countMax1Level(MCU_t &MCU, MEM_t &MEM) {
    int left_max = 19;
    int right_max = 39;

    bool ping_pong = 0;
    int cur = left_max;

    vector<pair<float, int>> x_coords;

    while ((cur = ping_pong ? right_max-- : left_max--) >= 0) {
        x_coords.push_back(make_pair(MCU.x_coords[MEM.connection[cur]], cur));
        ping_pong = !ping_pong;
    }

    for (int i = 0; i < PIN_COUNT/2; ++i) {
        set<int> _set;
        _set.insert(i);

        _set = Dive(x_coords, _set, i);

        for (int elem: _set)
            cout << elem << ' ';

        cout << endl;
    }

    return deep;
}
*/

int main(int argc, char const *argv[])
{
    MCU_t MCU;
    MEM_t MEM;

    readCoords(MCU, MEM);
    readConnections(MCU, MEM);
    calculateLengths(MCU, MEM);
    //cout << countMax1Level(MCU, MEM) << endl;

    return 0;
}