import pytest

import infra.callisto.controllers.utils.funcs as funcs
import infra.callisto.controllers.sdk.prod_ruler.acceptance as acceptance
import infra.callisto.controllers.sdk.prod_ruler.deploy as deploy_ruler


class HackedAcceptanceTable(acceptance.AcceptanceTableSource):
    def __init__(self, rows):
        self._rows = rows

    def _load_rows(self):
        return self._rows


def test_acceptance_table():
    state_1 = '20190120-015224'
    state_2 = '20200101-015224'
    tbl = HackedAcceptanceTable([
        {'Timestamp': 1, 'State': state_1, 'IsAccepted': True, 'IsSkipped': False},
        {'Timestamp': 2, 'State': state_1, 'IsAccepted': False, 'IsSkipped': True},
    ])
    assert funcs.yt_state_to_timestamp(state_1) not in tbl.load().accepted
    assert funcs.yt_state_to_timestamp(state_1) in tbl.load().skipped

    tbl = HackedAcceptanceTable([
        {'Timestamp': 1, 'State': state_1, 'IsAccepted': True, 'IsSkipped': False},
        {'Timestamp': 2, 'State': state_1, 'IsAccepted': False, 'IsSkipped': True},
        {'Timestamp': 3, 'State': state_1, 'IsAccepted': True, 'IsSkipped': False},
        {'Timestamp': 4, 'State': state_2, 'IsAccepted': True, 'IsSkipped': False},  # garbage
    ])
    assert funcs.yt_state_to_timestamp(state_1) in tbl.load().accepted
    assert funcs.yt_state_to_timestamp(state_1) not in tbl.load().skipped

    acceptance.AcceptanceStaticSource(skipped={1, 2}, accepted={3, 4}).load()
    with pytest.raises(ValueError):
        acceptance.AcceptanceStaticSource(skipped={2, 3}, accepted={3, 4})


def test_deploy_on_prod():
    all_timestamps = [6, 5, 4, 3, 2, 1]
    skipped = []
    max_ts = 2
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 1, max_ts) == {1, 2}
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 2, max_ts) == {2, 3}
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 3, max_ts) == {3, 4}
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 4, max_ts) == {4, 5}
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 5, max_ts) == {5, 6}
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 6, max_ts) == {5, 6}

    max_ts = 3
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 1, max_ts) == {1, 2, 3}
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 2, max_ts) == {2, 3, 4}
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 3, max_ts) == {3, 4, 5}
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 4, max_ts) == {4, 5, 6}
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 5, max_ts) == {4, 5, 6}
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 6, max_ts) == {4, 5, 6}

    max_ts = 6
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 1, max_ts) == {1, 2, 3, 4, 5, 6}
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 3, max_ts) == {1, 2, 3, 4, 5, 6}
    assert deploy_ruler.to_deploy_on_prod(all_timestamps, skipped, 6, max_ts) == {1, 2, 3, 4, 5, 6}


def test_deploy_on_acceptance():
    all_timestamps = [6, 5, 4, 3, 2, 1]
    skipped = []
    max_ts = 1
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 1, max_ts) == {2}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 2, max_ts) == {3}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 3, max_ts) == {4}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 4, max_ts) == {5}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 5, max_ts) == {6}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 6, max_ts) == {6}

    max_ts = 2
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 1, max_ts) == {2, 3}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 2, max_ts) == {3, 4}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 3, max_ts) == {4, 5}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 4, max_ts) == {5, 6}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 5, max_ts) == {6}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 6, max_ts) == {6}

    max_ts = 3
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 1, max_ts) == {2, 3, 4}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 2, max_ts) == {3, 4, 5}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 3, max_ts) == {4, 5, 6}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 4, max_ts) == {5, 6}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 5, max_ts) == {6}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 6, max_ts) == {6}

    max_ts = 6
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 1, max_ts) == {2, 3, 4, 5, 6}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 2, max_ts) == {3, 4, 5, 6}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 3, max_ts) == {4, 5, 6}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 4, max_ts) == {5, 6}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 5, max_ts) == {6}
    assert deploy_ruler.to_deploy_on_acceptance(all_timestamps, skipped, 6, max_ts) == {6}


def test_search_target_on_acceptance():
    assert deploy_ruler.DeployRuler.search_target_on_acceptance([3, 2], 0) == 2
    assert deploy_ruler.DeployRuler.search_target_on_acceptance([3, 2], 1) == 2
    assert deploy_ruler.DeployRuler.search_target_on_acceptance([3, 2], 2) == 3
    assert deploy_ruler.DeployRuler.search_target_on_acceptance([3, 2], 3) == 3
