/* endpoint_test.cc
   Jeremy Barnes, 31 January 2011
   Copyright (c) 2011 Datacratic.  All rights reserved.

   Tests for the endpoints.
*/

#include <boost/test/unit_test.hpp>
#include <yandex_io/external_libs/datacratic/soa/service/http_endpoint.h>
#include <yandex_io/external_libs/datacratic/soa/service/passive_endpoint.h>
#include <sys/socket.h>
#include <yandex_io/external_libs/datacratic/jml/utils/guard.h>
#include "watchdog.h"

using namespace std;
using namespace ML;
using namespace Datacratic;

#if 0 // zeromq aborts when it can't get an fd
BOOST_AUTO_TEST_CASE( test_passive_endpoint_create_no_fds )
{
    Watchdog watchdog;
    FDExhauster exhaust_fds;

    PassiveEndpointT<SocketTransport> endpoint;

    {
        BOOST_CHECK_THROW(endpoint.init(), ML::Exception);
    }

    endpoint.shutdown();
}
#endif

#if 0 // we need more than one FD for each connection now; test unreliable
BOOST_AUTO_TEST_CASE( test_active_endpoint_create_no_fds )
{
    BOOST_REQUIRE_EQUAL(TransportBase::created, TransportBase::destroyed);
    BOOST_REQUIRE_EQUAL(ConnectionHandler::created,
                        ConnectionHandler::destroyed);

    Watchdog watchdog;

    ActiveEndpointT<SocketTransport> connector("connector");
    connector.init(9997, "localhost", 0, 1, true, false /* throw on error */);

    {
        FDExhauster exhaust_fds;

        cerr << "doing connection error" << endl;

        doTestConnectionError(connector, "Too many open files");

        cerr << "done connection error" << endl;
    }

    connector.shutdown();

    BOOST_CHECK_EQUAL(connector.numActiveConnections(), 0);
    BOOST_CHECK_EQUAL(connector.numInactiveConnections(), 0);
    BOOST_CHECK_EQUAL(connector.threadsActive(), 0);
    BOOST_CHECK_EQUAL(TransportBase::created, TransportBase::destroyed);
    BOOST_CHECK_EQUAL(ConnectionHandler::created,
                      ConnectionHandler::destroyed);
}
#endif

#if 0 // no way yet to make work be done until sem acquired
BOOST_AUTO_TEST_CASE( test_active_endpoint_no_threads )
{
    Watchdog watchdog;

    ActiveEndpointT<SocketTransport> connector;
    connector.init(9997, "localhost", 0, 0, true);

    doTestConnectionError(connector, "not connected");
}
#endif

BOOST_AUTO_TEST_CASE(test_passive_endpoint)
{
    BOOST_REQUIRE_EQUAL(TransportBase::created, TransportBase::destroyed);
    BOOST_REQUIRE_EQUAL(ConnectionHandler::created,
                        ConnectionHandler::destroyed);

    Watchdog watchdog(5.0);

    string connectionError;

    PassiveEndpointT<SocketTransport> acceptor("acceptor");
    int port = acceptor.init();

    BOOST_CHECK_NE(port, -1);

    acceptor.shutdown();

    BOOST_CHECK_EQUAL(TransportBase::created, TransportBase::destroyed);
    BOOST_CHECK_EQUAL(ConnectionHandler::created,
                      ConnectionHandler::destroyed);
}
