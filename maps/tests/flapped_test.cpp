#include <maps/analyzer/libs/flapped/include/flapped.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <algorithm>
#include <ctime>
#include <vector>

using namespace maps::analyzer::flapped;

TEST(Flapped, Sample) {
    Flapped<int, std::greater<int>> F(30, 120, 0, 0);
    F.set(130, 2);
    F.set(140, 3);
    EXPECT_EQ(F.get(150), 2);
    F.set(150, 3);
    F.set(176, 3);
    EXPECT_EQ(F.get(180), 3);
    F.set(180, 1);
    F.set(200, 10);
    F.set(250, 20);
    EXPECT_EQ(F.get(250), 3);
    F.set(320, 30);
    EXPECT_EQ(F.get(330), 20);
    EXPECT_EQ(F.get(3000), std::nullopt);
}

TEST(Flapped, HardTest) {
    Flapped<int, std::greater<int>> F(50, 100, 0, 10);
    F.set(50, 10);
    EXPECT_EQ(F.get(60), 10);
    F.set(70, 2);
    EXPECT_EQ(F.get(70), 10);
    F.set(80, 5);
    EXPECT_EQ(F.get(80), 10);
    EXPECT_EQ(F.get(150), 2);
    F.set(150, 5);
    EXPECT_EQ(F.get(150), 5);
    EXPECT_EQ(F.get(250), std::nullopt);
    F.set(260, 3);
    F.set(270, 2);
    EXPECT_EQ(F.get(280), 2);
    F.set(290, 5);
    EXPECT_EQ(F.get(300), 2);
    F.set(320, 5);
    EXPECT_EQ(F.get(320), 2);
    F.set(340, 5);
    EXPECT_EQ(F.get(340), 5);
}

TEST(Flapped, HardTest2) {
    Flapped<int, std::greater<int>> F(50, 100, 0, 100);
    F.set(100, 10);
    F.set(110, 10);
    F.set(130, 10);
    F.set(140, 10);
    F.set(150, 10);
    F.set(160, 5);
    EXPECT_EQ(F.get(170), 10);
    F.set(180, 10);
    F.set(190, 2);
    EXPECT_EQ(F.get(190), 10);
    F.set(210, 2);
    EXPECT_EQ(F.get(210), 10);
    F.set(240, 3);
    EXPECT_EQ(F.get(240), 10);
    F.set(250, 10);
    EXPECT_EQ(F.get(250), 2);
    EXPECT_EQ(F.get(310), 3);
}

TEST(Flapped, Asserts) {
   EXPECT_ANY_THROW(Flapped<int> G(200, 100, 0, 10));
    Flapped<int, std::greater<int>> F(50, 100, 0, 10);
    F.set(50, 2);
    EXPECT_ANY_THROW(F.set(40, 2));
    EXPECT_ANY_THROW(F.get(40));
    F.set(60, 2);
    F.set(60, 2);
    EXPECT_ANY_THROW(F.get(50));
    F.get(70);
    EXPECT_ANY_THROW(F.set(65, 2));
}

TEST(Flapped, Exept) {
    Flapped<int, std::greater<int>> F(50, 100, 0, 1000);
    F.set(1000, 100);
    F.set(1050, 100);
    F.set(1052, 50);
    EXPECT_EQ(F.get(1060), 100);
    F.set(2000, 100);
    EXPECT_EQ(F.get(2100), std::nullopt);
    F.set(2200,2100);
    F.set(2300,300);
    EXPECT_EQ(F.get(2300), 300);
    F.set(2350, 300);
    F.set(2350, 100);
    EXPECT_EQ(F.get(2350), 300);
    F.set(2400, 100);
    EXPECT_EQ(F.get(2400), 100);
}

TEST(Flapped, Default) {
    Flapped<int, std::greater<int>> F(50, 100, 0);
    EXPECT_EQ(F.get(0), 0);
    F.set(1000, 100);

}

TEST(Flapped, RandTest1) {
    srand(time(0));
    const int sz = 300, INF = 1e9, cons = 10000, ST = 5, FT = 30;
    Flapped<int, std::greater<int>> F(ST, FT, 0, INF);
    std::vector<int> ar(sz + 1);
    ar[0] = INF;
    for (int i = 1; i <= sz; i++) {
        int r = rand() % 2;
        if (r == 0) {
            int val = rand() % INF;
            ar[i] = val;
            F.set(i + cons, val);
        } else {
            ar[i] = INF;
        }
        int mi = INF;
        for (int j = i; j > std::max(0, i - FT); j--) {
            mi = std::min(mi, ar[j]);
        }
        if (mi == INF) {
            EXPECT_EQ(F.get(i + cons), std::nullopt);
        } else {
            EXPECT_EQ(F.get(i + cons), mi);
        }
    }
}

TEST(Flapped, RandTest2) {
    srand(time(0));
    const int sz = 300, INF = 1e9, cons = 10000, ST = 5, FT = 10;
    Flapped<int, std::greater<int>> F(ST, FT, 0, INF);
    std::vector<int> ar(sz + 1);
    ar[0] = INF;
    for (int i = 1; i <= sz; i++) {
        int r = rand() % 10;
        if (r == 0) {
            int val = rand() % INF;
            ar[i] = val;
            F.set(i + cons, val);
        } else {
            ar[i] = INF;
        }
        int mi = INF;
        for (int j = i; j > std::max(0, i - FT); j--) {
            mi = std::min(mi, ar[j]);
        }
        if (mi == INF) {
            EXPECT_EQ(F.get(i + cons), std::nullopt);
        } else {
            EXPECT_EQ(F.get(i + cons), mi);
        }
    }
}

TEST(Flapped, RandTest3) {
    srand(time(0));
    const int sz = 3000, INF = 1e9, cons = 10000, ST = 100, FT = 10000;
    Flapped<int, std::greater<int>> F(ST, FT, 0, INF);
    std::vector<int> ar(sz + 1);
    ar[0] = INF;
    int fl = 0, fl2 = 0;
    int cu = 0;
    for (int i = 1; i <= sz; i++) {
        int r = rand() % 30;
        if (r == 0) {
            fl2 = 1;
            cu = 0;
            ar[i] = 50;
            F.set(i + cons, 50);
        } else {
            cu++;
            ar[i] = 100;
            F.set(i + cons, 100);
        }
        if (cu == ST + 1) {
            fl = 1;
        }
        if (fl == 0 && fl2 == 1) {
            EXPECT_EQ(F.get(i + cons), 50);
        } else {
            EXPECT_EQ(F.get(i + cons), 100);
        }
    }
}

TEST(Flapped, StableAfterLoss) {
    Flapped<int, std::greater<int>> f(10, 20, 0, 10);
    f.set(2, 10);
    f.set(5, 10);
    f.set(10, 10);
    // долго нет значений
    EXPECT_EQ(f.get(30), std::nullopt);
    // данные снова начали появляться
    f.set(30, 10);
    f.set(35, 10);
    f.set(40, 10);
    f.set(45, 1);
    EXPECT_EQ(f.get(50), 10);
    f.set(65, 1);
    f.set(70, 1);
    EXPECT_EQ(f.get(70), 1);
}

TEST(Flapped, Mode){
    enum class Status {
        Offline,
        Online
    };

    srand(time(0));
    const int sz = 3000, INF = 1e9, cons = 100000, ST = 200, FT = 10000;
    Flapped<Status, std::greater<Status>> F(ST, FT, 0, Status::Offline);
    std::vector<int> ar(sz + 1);
    ar[0] = INF;
    int fl = 0, fl2 = 0;
    int cu = 0;
    for (int i = 1; i <= sz; i++) {
        int r = rand() % 30;
        if (r == 0) {
            fl2 = 1;
            cu = 0;
            ar[i] = 0;
            F.set(i + cons, Status::Offline);
        } else {
            cu++;
            ar[i] = 1;
            F.set(i + cons, Status::Online);
        }
        if (cu == ST + 1) {
            fl = 1;
        }
        if (fl == 0 && fl2 == 1) {
            EXPECT_EQ(F.get(i + cons), Status::Offline);
        } else {
            EXPECT_EQ(F.get(i + cons), Status::Online);
        }
    }
}

TEST(Flapped, StableAfterLoss2) {
    Flapped<int, std::greater<int>> f(10, 20, 0, 10);
    f.set(2, 10);
    f.set(5, 10);
    f.set(10, 10);
    // долго нет значений
    EXPECT_EQ(f.get(30), std::nullopt);
    // данные снова начали появляться
    f.set(30, 10);
    f.set(31, 8); // 10 на предыдущем шаге продлевает стабильность несмотря на пропажу данных
    EXPECT_EQ(f.get(32), 10);
    f.set(35, 10);
    f.set(40, 10);
    f.set(45, 10);
    f.set(50, 1);
    EXPECT_EQ(f.get(55), 10);
}

TEST(Flapped, FlappingAfterLoss) {
    Flapped<int, std::greater<int>> f(10, 20, 0, 10);
    f.set(2, 10);
    f.set(5, 10);
    f.set(10, 10);
    // долго нет значений
    EXPECT_EQ(f.get(30), std::nullopt);
    // данные снова начали появляться
    f.set(30, 8);
    f.set(31, 10);
    EXPECT_EQ(f.get(32), 8); // 8 после потери данных ломает стабильную последовательность => флапаем
    f.set(35, 10);
    f.set(40, 10);
    f.set(45, 10);
    f.set(50, 1);
    EXPECT_EQ(f.get(55), 10);
}

TEST(Flapped, last) {
    Flapped<int, std::greater<int>> F(10, 20, 0, 10);
    EXPECT_EQ(F.status(0), Mode::Stable);
    EXPECT_EQ(F.last(0), 10);
    F.set(10, 5);
    EXPECT_EQ(F.last(15), 5);
    EXPECT_EQ(F.status(15), Mode::Unstable);
    EXPECT_EQ(F.status(20), Mode::Flapping);
    F.set(20, 20);
    F.set(30, 20);
    EXPECT_EQ(F.status(30), Mode::Stable);
    EXPECT_ANY_THROW(F.status(20));
    EXPECT_ANY_THROW(F.last(20));
    F.set(35, 5);
    EXPECT_EQ(F.status(35), Mode::Unstable);
    F.set(50, 4);
    EXPECT_EQ(F.status(50), Mode::Flapping);
    F.set(100, 4);
    EXPECT_EQ(F.status(105), Mode::Stable);
    EXPECT_EQ(F.status(200), Mode::NoData);
    F.set(210, 5);
    EXPECT_EQ(F.status(210), Mode::Flapping);
    F.set(220, 5);
    EXPECT_EQ(F.status(225), Mode::Stable);
    EXPECT_EQ(F.status(240), Mode::NoData);
    F.set(300, 10);
    F.set(305, 10);
    F.set(310, 10);
    F.set(315, 5);
    EXPECT_EQ(F.status(315), Mode::Unstable);
}
