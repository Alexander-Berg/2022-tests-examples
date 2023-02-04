/* endpoint_test.cc
   Jeremy Barnes, 31 January 2011
   Copyright (c) 2011 Datacratic.  All rights reserved.

   Tests for the endpoints.
*/

#include <boost/test/unit_test.hpp>

#include "watchdog.h"

#include <yandex_io/external_libs/datacratic/soa/service/http_endpoint.h>
#include <yandex_io/external_libs/datacratic/soa/service/passive_endpoint.h>

#include <util/system/byteorder.h>

#include <sys/socket.h>
#include <poll.h>

using namespace std;
using namespace ML;
using namespace Datacratic;

#include "ping_pong.h"

void runAcceptSpeedTest()
{
    string connectionError;

    PassiveEndpointT<SocketTransport> acceptor("acceptor");

    acceptor.onMakeNewHandler = [&]()
    {
        return std::static_pointer_cast<ConnectionHandler>(
            std::make_shared<PongConnectionHandler>(connectionError));
    };

    int port = acceptor.init();

    cerr << "port = " << port << endl;

    BOOST_CHECK_EQUAL(acceptor.numConnections(), 0);

    int nconnections = 100;

    Date before = Date::now();

    std::vector<int> sockets;

    /* Open all the connections */
    for (int i = 0; i < nconnections; ++i) {
        int s = socket(AF_INET, SOCK_STREAM, 0);
        if (s == -1)
            throw Exception("socket");

        // cerr << "i = " << i << " s = " << s << " sockets.size() = "
        //      << sockets.size() << endl;

        struct sockaddr_in addr = {AF_INET, HostToInet<uint16_t>(port), {INADDR_ANY}};
        // cerr << "before connect on " << s << endl;
        int res = connect(s, reinterpret_cast<const sockaddr*>(&addr),
                          sizeof(addr));
        // cerr << "after connect on " << s << endl;

        if (res == -1) {
            cerr << "connect error: " << strerror(errno) << endl;
            close(s);
        } else {
            sockets.push_back(s);
        }
    }

    /* Write to each and get a response back.  This makes sure that all are open. */
    for (unsigned i = 0; i < sockets.size(); ++i) {
        int s = sockets[i];
        int res = write(s, "hello", 5);
        BOOST_CHECK_EQUAL(res, 5);

        char buf[16];

        res = read(s, buf, 16);
        BOOST_CHECK_EQUAL(res, 4);
        if (res > 0) {
            BOOST_CHECK_EQUAL(string(buf, buf + res), "Hi!!");
        }
    }

    Date after = Date::now();

    BOOST_CHECK_LT(after.secondsSince(before), 1);

    BOOST_CHECK_EQUAL(int(sockets.size()), nconnections);

    BOOST_CHECK_EQUAL(acceptor.numConnections(), nconnections);

    acceptor.closePeer();

    for (unsigned i = 0; i < sockets.size(); ++i) {
        close(sockets[i]);
    }

    acceptor.shutdown();
}

BOOST_AUTO_TEST_CASE(test_accept_speed)
{
    BOOST_REQUIRE_EQUAL(TransportBase::created, TransportBase::destroyed);
    BOOST_REQUIRE_EQUAL(ConnectionHandler::created,
                        ConnectionHandler::destroyed);

    //    Watchdog watchdog(50.0);

    int ntests = 1;
    // ntests = 1000;  // stress test

    for (int i = 0; i < ntests; ++i) {
        runAcceptSpeedTest();
    }

    BOOST_CHECK_EQUAL(TransportBase::created, TransportBase::destroyed);
    BOOST_CHECK_EQUAL(ConnectionHandler::created,
                      ConnectionHandler::destroyed);
}
