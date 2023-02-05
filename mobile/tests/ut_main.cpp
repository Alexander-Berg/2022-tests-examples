#include <thread>

#define BOOST_TEST_NO_MAIN
#include <boost/test/included/unit_test.hpp>

#include <QCoreApplication>

boost::unit_test::test_suite* init_unit_test(int, char**)
{
    return nullptr;
}

int main(int argc, char** argv)
{
    QCoreApplication app(argc, argv);
    app.setApplicationVersion("1.0");

    auto thread = std::thread(
        [&] { app.exit(boost::unit_test::unit_test_main(&init_unit_test, argc, argv)); });

    int res = app.exec();

    thread.join();

    return res;
}
