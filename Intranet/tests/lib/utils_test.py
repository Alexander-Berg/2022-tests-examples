from unittest.mock import patch

from intranet.vconf.tests.lib.mocks import participants_hydrator_mock
from intranet.vconf.src.lib.utils import round_duration, hydrate_participants


def test_round_duration():
    assert round_duration('0') == 30
    assert round_duration(30) == 60
    assert round_duration(35) == 60
    assert round_duration(10) == 30
    assert round_duration(1000) == 720
    assert round_duration(60) == 120


@patch('intranet.vconf.src.lib.utils.ParticipantsHydrator', participants_hydrator_mock)
def test_hydrate_participants():
    data = [
        {'type': '1805', 'id': 35},
        {'type': '4892', 'id': 33}
    ]
    res = hydrate_participants(data)
    assert [i.data for i in res] == data
