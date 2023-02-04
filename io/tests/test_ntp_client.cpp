#include <yandex_io/libs/ntp/ntp_client.h>
#include <yandex_io/libs/ntp/ntp_client_api.h>
#include <yandex_io/libs/ntp/ntp_packet_raw.h>

#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE_F(TestNtp, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testAddrCtorHostNameOnly)
    {
        UNIT_ASSERT_NO_EXCEPTION(quasar::NtpClient::Addr());
        UNIT_ASSERT_NO_EXCEPTION(quasar::NtpClient::Addr("localhost"));
        UNIT_ASSERT_NO_EXCEPTION(quasar::NtpClient::Addr("localhost:100"));
        UNIT_ASSERT_VALUES_EQUAL(quasar::NtpClient::Addr("localhost").host, "localhost");
        UNIT_ASSERT_VALUES_EQUAL(quasar::NtpClient::Addr("localhost").port, 123);
        UNIT_ASSERT_VALUES_EQUAL(quasar::NtpClient::Addr("localhost:555").port, 555);
        UNIT_ASSERT_EXCEPTION(quasar::NtpClient::Addr("localhost:abc"), std::exception);
        UNIT_ASSERT_EXCEPTION(quasar::NtpClient::Addr("localhost:"), std::exception);
    }

    Y_UNIT_TEST(testAddrCtorFull)
    {
        UNIT_ASSERT_NO_EXCEPTION(quasar::NtpClient::Addr("localhost", 123));
    }

    Y_UNIT_TEST(testAddrEq)
    {
        UNIT_ASSERT(quasar::NtpClient::Addr("localhost") == quasar::NtpClient::Addr("localhost"));
        UNIT_ASSERT(quasar::NtpClient::Addr("localhost:555") == quasar::NtpClient::Addr("localhost", 555));
    }

    Y_UNIT_TEST(testNtpClientParamsValidConfigs)
    {
        UNIT_ASSERT_NO_EXCEPTION(quasar::NtpClient::Params());

        UNIT_ASSERT_NO_EXCEPTION(quasar::NtpClient::Params().parseJson(Json::Value{}));

        {
            Json::Value server1 = "ntp.server1";
            Json::Value server2 = "ntp.server2:111";
            Json::Value server3;
            server3["host"] = "ntp.server3";
            server3["port"] = 999;

            Json::Value config;
            config["timeoutMs"] = 333;
            config["minMeasuringCount"] = 444;
            config["sufficientMeasuringCount"] = 555;
            config["maxMeasuringCount"] = 666;
            config["ntpServers"].append(server1);
            config["ntpServers"].append(server2);
            config["ntpServers"].append(server3);

            quasar::NtpClient::Params p;
            UNIT_ASSERT_NO_EXCEPTION(p.parseJson(config));

            UNIT_ASSERT(p.timeout == std::chrono::milliseconds{333});
            UNIT_ASSERT(p.minMeasuringCount == 444);
            UNIT_ASSERT(p.sufficientMeasuringCount == 555);
            UNIT_ASSERT(p.maxMeasuringCount == 666);
            UNIT_ASSERT(p.ntpServers.size() == 3);
            UNIT_ASSERT(p.ntpServers[0].host == "ntp.server1");
            UNIT_ASSERT(p.ntpServers[0].port == quasar::NtpClient::Addr().port);
            UNIT_ASSERT(p.ntpServers[1].host == "ntp.server2");
            UNIT_ASSERT(p.ntpServers[1].port == 111);
            UNIT_ASSERT(p.ntpServers[2].host == "ntp.server3");
            UNIT_ASSERT(p.ntpServers[2].port == 999);
        }

        {
            Json::Value partialConfig;
            partialConfig["maxMeasuringCount"] = 4;

            quasar::NtpClient::Params pDef;
            pDef.maxMeasuringCount = 4;

            quasar::NtpClient::Params p;
            UNIT_ASSERT_NO_EXCEPTION(p.parseJson(partialConfig));
            UNIT_ASSERT(p.maxMeasuringCount == 4);
            UNIT_ASSERT(p == pDef);
        }
    }

    Y_UNIT_TEST(testNtpClientParamsInvalidConfigs)
    {
        {
            quasar::NtpClient::Params p;
            Json::Value invalidConfig;
            invalidConfig["timeoutMs"] = "333";
            UNIT_ASSERT_EXCEPTION(p.parseJson(invalidConfig), std::exception);
        }

        {
            quasar::NtpClient::Params p;
            Json::Value invalidConfig;
            invalidConfig["ntpServers"][0] = "ntp.server:";
            UNIT_ASSERT_EXCEPTION(p.parseJson(invalidConfig), std::exception);
        }
    }

    Y_UNIT_TEST(testNtpClientResult)
    {
        quasar::NtpClient::NtpResult result;
        UNIT_ASSERT(result.diff.count() == 0);
        UNIT_ASSERT(result.syncTime().time_since_epoch().count() > 0);
    }

    Y_UNIT_TEST(testNtpClientSync)
    {
        std::shared_ptr<quasar::NtpClientApi> api = std::make_shared<quasar::NtpClientApi>();

        int socket_count = 0;
        api->getaddrinfo =
            [](const char* /*node*/, const char* /*service*/, const struct addrinfo* /*hints*/, struct addrinfo** res)
        {
            (*res) = new struct addrinfo;
            std::memset(*res, 0, sizeof(**res));
            (*res)->ai_addr = new struct sockaddr;
            std::memset((*res)->ai_addr, 0, sizeof(*(*res)->ai_addr));
            return 0;
        };
        api->freeaddrinfo =
            [](struct addrinfo* p)
        {
            delete p->ai_addr;
            delete p;
        };
        api->socket =
            [&](int /*domain*/, int /*type*/, int /*protocol*/)
        {
            return ++socket_count;
        };
        api->setsockopt =
            [](int /*sockfd*/, int /*level*/, int /*optname*/, const void* /*optval*/, socklen_t /*optlen*/)
        {
            return 0;
        };

        api->close = [](int /*fd*/) {};
        api->inet_ntop =
            [](int /*af*/, const void* /*src*/, char* buf, socklen_t /*size*/) {
                if (buf) {
                    strcpy(buf, "0.0.0.0");
                }
                return buf;
            };
        api->sendto =
            [](int /*sockfd*/, const void* /*buf*/, size_t /*len*/, int /*flags*/, const struct sockaddr* /*dest_addr*/, socklen_t /*adrlen*/)
        {
            return 0;
        };

        api->recvfrom =
            [](int /*sockfd*/, void* buf, size_t sz, int /*flags*/, struct sockaddr* /*src_addr*/, socklen_t* /*addrlen*/)
        {
            UNIT_ASSERT(sz == sizeof(quasar::NtpPacketRaw));
            quasar::NtpPacketRaw& raw = *reinterpret_cast<quasar::NtpPacketRaw*>(buf);
            raw.flags = 0x24;
            raw.stratum = 0x2;
            raw.poll = 0x3;
            raw.precision = 0xe8;
            raw.root_delay = 0xca070000;
            raw.root_dispersion = 0xbb060000;
            raw.referenceId[0] = 0x59;
            raw.referenceId[1] = 0x6d;
            raw.referenceId[2] = 0xfb;
            raw.referenceId[3] = 0x16;
            raw.ref_ts_sec = 0x7f4c07e2;
            raw.ref_ts_frac = 0xc772e297;
            raw.origin_ts_sec = 0x0;
            raw.origin_ts_frac = 0x0;
            raw.recv_ts_sec = 0xf5107e2;
            raw.recv_ts_frac = 0x7794b517;
            raw.trans_ts_sec = 0xf5107e2;
            raw.trans_ts_frac = 0x7215b917;
            return sz;
        };
        api->poll =
            [](struct pollfd* /*fds*/, nfds_t count, int /*timeout*/)
        {
            return static_cast<int>(count);
        };

        quasar::NtpClient ntpClient(quasar::NtpClient::Params{}, api);

        quasar::NtpClient::NtpResult ntpResult;

        auto steady0 = std::chrono::steady_clock::now();
        UNIT_ASSERT_NO_EXCEPTION(ntpResult = ntpClient.sync(quasar::NtpClient::SyncMode::DEFAULT));

        auto syncTime = ntpResult.syncTime();
        auto steady1 = std::chrono::steady_clock::now();

        auto originalServerTime = std::chrono::microseconds{1583141519655606};
        auto expectedServerTime = originalServerTime + std::chrono::duration_cast<std::chrono::microseconds>(steady1 - steady0);

        auto eus = std::chrono::duration_cast<std::chrono::seconds>(expectedServerTime).count();
        auto sus = std::chrono::duration_cast<std::chrono::seconds>(syncTime.time_since_epoch()).count();
        UNIT_ASSERT(std::abs(eus - sus) < 10);
    }
}
