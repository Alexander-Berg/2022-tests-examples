#!/usr/bin/env python

import unittest

from apikey import TestRouterApiKey
from avoid_tolls import RouterApiAvoidTollsTest
from departure_time import TestRouterApiDepartureTime
from mode import RouterApiModeTest
from query_limits import TestRouterApiQueryLimit
from route_not_found import TestRouterApiRouteNotFound
from test_generic import TestRouterApiGeneric
from waypoints import TestRouterWaypoints

__all__ = [
    TestRouterApiGeneric, TestRouterApiKey, TestRouterApiQueryLimit,
    TestRouterApiRouteNotFound, TestRouterApiDepartureTime,
    RouterApiModeTest, TestRouterWaypoints,
    RouterApiAvoidTollsTest]


def main():
    unittest.main()


if __name__ == "__main__":
    main()
