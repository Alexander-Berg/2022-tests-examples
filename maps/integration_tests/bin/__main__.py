import unittest

from .test_buckets import S3BucketsTest
from .test_calendar_planning import CalendarPlanningApikeyTest
from .test_core import CoreTest
from .test_multiple_routing_modes import MultpleRoutingModesTest
from .test_regions_availability import RegionsAvailabilityTest
from .test_resource_consumption import ResourceConsumptionTest
from .test_smoke import SmokeTest
from .test_thread_count import ThreadCountTest


__all__ = [
    CalendarPlanningApikeyTest,
    CoreTest,
    MultpleRoutingModesTest,
    RegionsAvailabilityTest,
    ResourceConsumptionTest,
    S3BucketsTest,
    SmokeTest,
    ThreadCountTest,
]


if __name__ == "__main__":
    unittest.main()
