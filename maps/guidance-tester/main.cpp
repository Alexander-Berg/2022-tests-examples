#include "report.h"

#include <yandex/maps/mapkit/guidance/internal/tester.h>

#include <yandex/maps/mapkit/internal/default_request_factory.h>

#include <yandex/maps/proto/mobile-config/mapkit2/driving.pb.h>

#include <yandex/maps/runtime/async/dispatcher.h>
#include <yandex/maps/runtime/logging/logging.h>
#include <yandex/maps/runtime/time.h>

#include <boost/program_options.hpp>

#include <iostream>

#include <QtCore/QCoreApplication>

namespace runtime = yandex::maps::runtime;
namespace tester = yandex::maps::mapkit::guidance::internal::tester;
namespace proto = yandex::maps::proto;

struct Application {
    void processOptions(int argc, char *argv[]);
    void run();

    tester::TestParameters parameters() const {
        tester::TestParameters params;

        if (enableNetwork) {
            params.requestFactory =
                yandex::maps::mapkit::internal::createDefaultRequestFactory();
        }
        params.realTimeClock = realTimeClock;

        return params;
    }

    std::string pathToTest;

    bool enableNetwork;
    bool realTimeClock;
    bool printSuccess;

    tester::Result result;
};

void Application::processOptions(int argc, char *argv[])
{
    using namespace boost::program_options;

    options_description options("Allowed options");
    options.add_options()
        ("help,h", "Print help")
        ("print-all,a", "Print all expectations, even realized")
        ("real-time,r", "Use real time clock")
        ("enable-network,n", "Enable real network")
        ("test", value(&pathToTest),
            "Test file in JSON format");

    positional_options_description positional;
    positional.add("test", 1);

    variables_map vm;
    store(command_line_parser(argc, argv)
        .options(options)
        .positional(positional)
        .run(), vm);
    notify(vm);

    bool showHelp = vm.count("help") != 0;
    if (pathToTest.empty()) {
        showHelp = true;
    }

    REQUIRE(
        !showHelp,
        "usage: " << argv[0] << " [options] [file with test]\n"
        << options);

    realTimeClock = (vm.count("real-time") > 0);
    enableNetwork = (vm.count("enable-network") > 0);
    printSuccess = (vm.count("print-all") > 0);
}

void Application::run()
{
    result = tester::runTestGuide(
        pathToTest,
        parameters()
    );
}

int run(int argc, char *argv[])
{
    Application app;
    try {
        app.processOptions(argc, argv);
    } catch (std::exception& ex) {
        std::cerr << ex.what() << "\n";
        return 1;
    }

    try {
        app.run();
    } catch (std::exception& exp) {
        std::cerr << "Something wrong in test: " << exp.what() << "\n";
        return 1;
    }

    if (app.result.neededUnsavedTiles) {
        std::cerr << "Needed unknown tiles" << "\n";
        return 1;
    }

    printReport(app.result, std::cout, app.printSuccess);
    return app.result.succeeded() ? 0 : 1;
}

int main(int argc, char *argv[])
{
    QCoreApplication qtApp(argc, argv);
    qtApp.setApplicationVersion("1.0");
    qtApp.setApplicationName("com.yandex.maps.GuidanceTester");

    std::thread thread([&] { qtApp.exit(run(argc, argv)); });

    int res = qtApp.exec();
    thread.join();
    return res;
}
