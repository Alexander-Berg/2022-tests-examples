#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <util/random/entropy.h>
#include <util/random/mersenne.h>
#include <util/system/fs.h>
#include <util/system/shellcommand.h>

#include <thread>

struct Fixture: public NUnitTest::TBaseFixture {
    Fixture()
        : rnd(Seed())
    {
        options.SetQuoteArguments(false).SetDetachSession(true).SetAsync(true).SetUseShell(false);
    };
    TShellCommandOptions options;
    TMersenne<ui64> rnd;

    static constexpr char testedApp[] = "yandex_io/libs/terminate_waiter/tests/test_app/test_app";

    static void waitForFile(const TString startFile) {
        while (!NFs::Exists(startFile)) {
            std::this_thread::sleep_for(std::chrono::milliseconds(20));
        }
    }
};

Y_UNIT_TEST_SUITE(TestUpdaterGateway) {
    Y_UNIT_TEST_F(TestTermination, Fixture) {
        for (const auto sig : {SIGTERM, SIGINT}) {
            const TString startFile{std::to_string(rnd.GenRand64())};
            TShellCommand cmd(BinaryPath(testedApp), {startFile}, options);
            cmd.Run();
            UNIT_ASSERT_EQUAL(cmd.GetStatus(), TShellCommand::SHELL_RUNNING);
            waitForFile(startFile);
            // We're sleeping so that there's some insurance against application terminating by itself, without waiting
            std::this_thread::sleep_for(std::chrono::seconds(5));
            kill(cmd.GetPid(), sig);
            cmd.Wait();

            UNIT_ASSERT_EQUAL(cmd.GetStatus(), TShellCommand::SHELL_FINISHED);
        }
    }

    Y_UNIT_TEST_F(TestBlocking, Fixture) {
        for (const auto sig : {SIGTERM, SIGINT}) {
            const TString startFile{std::to_string(rnd.GenRand64())};
            TShellCommand cmd(BinaryPath(testedApp), {startFile, TString{"5"}}, options);
            cmd.Run();
            UNIT_ASSERT_EQUAL(cmd.GetStatus(), TShellCommand::SHELL_RUNNING);
            waitForFile(startFile);

            kill(cmd.GetPid(), sig);
            cmd.Wait();

            UNIT_ASSERT_EQUAL(cmd.GetStatus(), TShellCommand::SHELL_FINISHED);
        }
    }

    Y_UNIT_TEST_F(TestSIGPIPE, Fixture) {
        const TString startFile{std::to_string(rnd.GenRand64())};
        TShellCommand cmd(BinaryPath(testedApp), {startFile, TString{"5"}}, options);
        cmd.Run();
        UNIT_ASSERT_EQUAL(cmd.GetStatus(), TShellCommand::SHELL_RUNNING);
        waitForFile(startFile);

        // Send SIGPIPE twice to test for continuous ignoring
        kill(cmd.GetPid(), SIGPIPE);
        kill(cmd.GetPid(), SIGPIPE);
        std::this_thread::sleep_for(std::chrono::seconds(1));

        UNIT_ASSERT_EQUAL(cmd.GetStatus(), TShellCommand::SHELL_RUNNING);
        cmd.Terminate();
    }
}
