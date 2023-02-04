#!/usr/bin/env python

import unittest

from test_generic import TestDistanceMatrixApiGeneric
from apikey import TestDistanceMatrixApiKey
from query_limits import TestDistanceMatrixApiQueryLimit
from route_not_found import TestDistanceMatrixApiRouteNotFound
from departure_time import TestDistanceMatrixApiDepartureTime
from mode import DistanceMatrixApiModeTest
from origins_and_destinations import TestDistanceMatrixOriginsAndDestinations
from rps_limit import TestDistanceMatrixApiRpsLimit
from priority import TestDistanceMatrixApiPriority

__all__ = [
    TestDistanceMatrixApiGeneric, TestDistanceMatrixApiKey, TestDistanceMatrixApiQueryLimit,
    TestDistanceMatrixApiRouteNotFound, TestDistanceMatrixApiDepartureTime,
    DistanceMatrixApiModeTest, TestDistanceMatrixOriginsAndDestinations,
    TestDistanceMatrixApiRpsLimit, TestDistanceMatrixApiPriority]


def main():
    unittest.main()


if __name__ == "__main__":
    main()
