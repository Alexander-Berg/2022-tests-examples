from datetime import datetime
from typing import Dict
import logging
import sys

import pytest


log = logging.getLogger('apikeys')
mh = logging.StreamHandler(stream=sys.stderr)
mh.setLevel(logging.DEBUG)
mh.setFormatter(logging.Formatter("%(asctime)s - %(levelname)s %(name)s: %(message)s"))
log.addHandler(mh)


@pytest.fixture()
def fake_statistic_getter():

    class FakeStatisticGetterProxy:

        empty_stat = {
            "value": 0,
            "voice_unit": 0,
            "tts_unit": 0,
            "ner_unit": 0,
            "total": 0,
            "hits": 0
        }

        def __init__(self, statistic_array):
            self.statistic_array: Dict[datetime, dict] = statistic_array

        def __call__(self, state, date_from, date_to=None):
            assert isinstance(date_from, datetime)
            date_to = date_to or state['now']
            stat = self.empty_stat.copy()
            for statistic_date, statistic_item in list(self.statistic_array.items()):
                if date_from <= statistic_date < date_to:
                    for unit, value in list(statistic_item.items()):
                        stat[unit] += value
            return stat
    return FakeStatisticGetterProxy
