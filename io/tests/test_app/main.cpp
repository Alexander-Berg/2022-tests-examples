#include <yandex_io/libs/terminate_waiter/terminate_waiter.h>

#include <chrono>
#include <fstream>
#include <iostream>
#include <random>
#include <thread>

#include <unistd.h>

int main(int argc, char** argv) {
    if (argc < 2) {
        return -1;
    }
    quasar::TerminateWaiter waiter;
    { std::ofstream t(argv[1]); }
    if (argc == 3) {
        const int sleep_for = std::stoi(std::string(argv[2]));
        std::this_thread::sleep_for(std::chrono::seconds(sleep_for));
    }
    waiter.wait();
    exit(0);
}
