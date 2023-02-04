#include <yandex_io/libs/metrica/base/metrica_session_provider.h>

#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <fstream>
#include <iostream>
#include <stdio.h>

namespace {
    struct Fixture: public QuasarUnitTestFixture {
        std::string sessionIdPersistentPart_;
        std::string sessionIdTemporaryPart_;

        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            const std::string basePath = tryGetRamDrivePath();

            sessionIdPersistentPart_ = basePath + "/metrica/clickdaemon_session_id_pers.txt";
            sessionIdTemporaryPart_ = basePath + "/temp_metrica/clickdaemon_session_id_temp.txt";
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            std::remove(sessionIdPersistentPart_.c_str());
            std::remove(sessionIdTemporaryPart_.c_str());

            Base::TearDown(context);
        }
    };
} // namespace

void assertValueInFile(const std::string& path, uint64_t expected) {
    std::ifstream input(path);
    uint64_t actual = 0;
    UNIT_ASSERT(input.good());
    input >> actual;
    UNIT_ASSERT_VALUES_EQUAL(actual, expected);
}

Y_UNIT_TEST_SUITE_F(MetricaSessionProviderTest, Fixture) {
    Y_UNIT_TEST(testInitialValues) {
        MetricaSessionProvider sessionProvider(sessionIdPersistentPart_, sessionIdTemporaryPart_);
        auto session = sessionProvider.getAndIncrementSession();
        // It is first usage of sessionProvider, so eventNumber must be 1
        UNIT_ASSERT_VALUES_EQUAL(session.eventNumber, 1);
        // On first sessionProvider creation (files has not been created yet) persistent and temporary parts has to be set to 1
        // Session id is combination of two parts (first 4 bytes is persistent part, last 4 is temporary)
        UNIT_ASSERT_VALUES_EQUAL(session.id, (1ULL << 32) | 1);
        // Some (not zero) start time has been set
        UNIT_ASSERT_VALUES_UNEQUAL(session.startTime, 0);
        // Check persistent and temporary parts has been properly saved
        assertValueInFile(sessionIdPersistentPart_, 1);
        assertValueInFile(sessionIdTemporaryPart_, 1);
    }

    Y_UNIT_TEST(testGetSession) {
        MetricaSessionProvider sessionProvider(sessionIdPersistentPart_, sessionIdTemporaryPart_);
        const int testCount = 10;
        auto session = sessionProvider.getAndIncrementSession();
        const uint64_t sessionId = session.id;
        const uint64_t sessionStartTime = session.startTime;
        for (int i = 2; i <= testCount; i++) {
            auto currentSession = sessionProvider.getAndIncrementSession();
            // On every getAndIncrementSession call eventNumber has to be increased by one
            UNIT_ASSERT_VALUES_EQUAL(currentSession.eventNumber, i);
            // On getAndIncrementSession session id and start time has not to be changed
            UNIT_ASSERT_VALUES_EQUAL(currentSession.id, sessionId);
            UNIT_ASSERT_VALUES_EQUAL(currentSession.startTime, sessionStartTime);
            // Check persistent and temporary parts has been properly saved
            assertValueInFile(sessionIdPersistentPart_, 1);
            assertValueInFile(sessionIdTemporaryPart_, 1);
        }
    }

    Y_UNIT_TEST(testGenerateNewSession) {
        MetricaSessionProvider sessionProvider(sessionIdPersistentPart_, sessionIdTemporaryPart_);
        // init session for first time
        sessionProvider.generateNewSession();
        const int testCount = 10;
        for (uint64_t i = 2; i <= testCount; i++) {
            auto session = sessionProvider.generateNewSession();
            // On generateNewSession eventNumber has to be reset to initial value (1)
            UNIT_ASSERT_VALUES_EQUAL(session.eventNumber, 1);
            // On generateNewSession persistent part has to be increased by one and temporary reset to initial (1)
            // Session id is combination of two parts (first 4 bytes is persistent part, last 4 is temporary)
            UNIT_ASSERT_VALUES_EQUAL(session.id, (i << 32) | 1);
            // Check persistent and temporary parts has been properly saved
            assertValueInFile(sessionIdPersistentPart_, i);
            assertValueInFile(sessionIdTemporaryPart_, 1);
        }
    }

    Y_UNIT_TEST(testImitateCrash) {
        const int testCount = 10;
        for (uint64_t i = 1; i <= testCount; i++) {
            // If yiod crash, temporary part will not be deleted
            // Imitate crash by creating new instance of MetricaSessionProvider
            MetricaSessionProvider sessionProvider(sessionIdPersistentPart_, sessionIdTemporaryPart_);
            auto session = sessionProvider.getAndIncrementSession();
            // eventNumber has to return to initial state on creating new sessionProvider
            UNIT_ASSERT_VALUES_EQUAL(session.eventNumber, 1);
            // Temporary part counts crashes, so on creating new sessionProvider it has to be increased by one
            // Persistent part has to remain on initial state (1)
            // Session id is combination of two parts (first 4 bytes is persistent part, last 4 is temporary)
            UNIT_ASSERT_VALUES_EQUAL(session.id, (1ULL << 32) | i);
            // Check persistent and temporary parts has been properly saved
            assertValueInFile(sessionIdPersistentPart_, 1);
            assertValueInFile(sessionIdTemporaryPart_, i);
        }
    }

    Y_UNIT_TEST(testImitateReboot) {
        const int testCount = 10;
        for (uint64_t i = 1; i <= testCount; i++) {
            // Imitate reboot by removing temporary part
            std::remove(sessionIdTemporaryPart_.c_str());
            MetricaSessionProvider sessionProvider(sessionIdPersistentPart_, sessionIdTemporaryPart_);
            auto session = sessionProvider.getAndIncrementSession();
            // eventNumber has to return to initial state on creating new sessionProvider after reboot
            UNIT_ASSERT_VALUES_EQUAL(session.eventNumber, 1);
            // Persistent part counts reboot, it has to increase by one if temporary part is not exists
            // If temporary part isn't exists, it has to return to initial state (1)
            // Session id is combination of two parts (first 4 bytes is persistent part, last 4 is temporary)
            UNIT_ASSERT_VALUES_EQUAL(session.id, (i << 32) | 1);
            // Check persistent and temporary parts has been properly saved
            assertValueInFile(sessionIdPersistentPart_, i);
            assertValueInFile(sessionIdTemporaryPart_, 1);
        }
    }
}
