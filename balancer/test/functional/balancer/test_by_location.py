# -*- coding: utf-8 -*-
import time
from collections import defaultdict
from math import ceil
import pytest

import balancer.test.plugin.context as mod_ctx
from balancer.test.util.predef import http
from balancer.test.util import asserts

from configs import ByLocationConfig, ByLocation2LvlConfig


class ByLocationContext(object):
    WEIGHTS_REREAD_TIMEOUT = 5

    def __init__(self):
        super(ByLocationContext, self).__init__()
        self.__weights_file = self.manager.fs.create_file('weights')
        self.__location_file = self.manager.fs.create_file('location')
        self.__quorums_file = self.manager.fs.create_file('quorums')

    @property
    def location_file(self):
        return self.__location_file

    @property
    def weights_file(self):
        return self.__weights_file

    @property
    def quorums_file(self):
        return self.__quorums_file

    @classmethod
    def __wait_reread(cls):
        time.sleep(cls.WEIGHTS_REREAD_TIMEOUT)

    def __write_weights_file(self, contents):
        self.manager.fs.rewrite(self.weights_file, contents)

    def __write_location_file(self, contents):
        self.manager.fs.rewrite(self.location_file, contents)

    def __write_quorums_file(self, contents):
        self.manager.fs.rewrite(self.quorums_file, contents)

    def write_weights_file(self, contents):
        self.__write_weights_file(contents)
        self.__wait_reread()  # make sure the file was re-read

    def write_location_file(self, contents):
        self.__write_location_file(contents)
        self.__wait_reread()  # make sure the file was re-read

    def write_quorums_file(self, contents):
        self.__write_quorums_file(contents)
        self.__wait_reread()  # make sure the file was re-read

    def __erase_weights_file(self):
        self.manager.fs.remove(self.weights_file)

    def __erase_location_file(self):
        self.manager.fs.remove(self.location_file)

    def __erase_quorums_file(self):
        self.manager.fs.remove(self.quorums_file)

    def erase_weights_file(self):
        self.__erase_weights_file()
        self.__wait_reread()

    def erase_location_file(self):
        self.__erase_location_file()
        self.__wait_reread()

    def erase_quorums_file(self):
        self.__erase_quorums_file()
        self.__wait_reread()

    def by_location_start_balancer(
        self,
        weights_file_contents=None,
        location_file_contents=None,
        quorums_file_contents=None,
        config=ByLocationConfig,
        **balancer_kwargs
    ):
        balancer_kwargs['weights_file'] = self.weights_file
        balancer_kwargs['preferred_location_switch'] = self.location_file
        balancer_kwargs['quorums_file'] = self.quorums_file
        has_weights_file = weights_file_contents is not None
        has_location_file = location_file_contents is not None
        has_quorums_file = quorums_file_contents is not None

        if has_weights_file:
            self.__write_weights_file(weights_file_contents)
        else:
            self.__erase_weights_file()

        if has_location_file:
            self.__write_location_file(location_file_contents)
        else:
            self.__erase_location_file()

        if has_quorums_file:
            self.__write_quorums_file(quorums_file_contents)
        else:
            self.__erase_quorums_file()

        balancer = self.start_balancer(config(**balancer_kwargs))
        if has_weights_file or has_location_file or has_quorums_file:
            self.__wait_reread()
        return balancer


by_loc_ctx = mod_ctx.create_fixture(ByLocationContext)


def check_weights(by_loc_ctx, expected_boundaries, count=1000, request=http.request.get()):
    answers = defaultdict(int)
    for i in xrange(count):
        resp = by_loc_ctx.perform_request(request)
        asserts.status(resp, 200)
        answers[resp.data.content] += 1
    # check distribution
    total = 0
    for k, v in expected_boundaries.items():
        got = answers[k]
        total += got
        assert got >= v[0]
        assert got <= v[1]
    assert count == total


def check_availability(by_loc_ctx, expected_nonzero, expected_answers=["id0", "id1", "id2"], count=100, required_nonzero=[]):
    answers = defaultdict(int)
    for i in xrange(count):
        resp = by_loc_ctx.perform_request(http.request.get())
        asserts.status(resp, 200)
        answers[resp.data.content] += 1
    for k in answers.keys():
        assert k in expected_answers
    total = 0
    found_nonzero = 0
    for k in expected_answers:
        got = answers[k]
        total += got
        if got > 0:
            found_nonzero += 1
    assert total == count
    assert found_nonzero == expected_nonzero
    for k in required_nonzero:
        assert answers[k] > 0


def test_start(by_loc_ctx):
    """
    Testing that balancer starts and a single request is processed.
    """
    by_loc_ctx.by_location_start_balancer()
    resp = by_loc_ctx.perform_request(http.request.get())
    asserts.status(resp, 200)
    assert resp.data.content in ['id 0', 'id 1']


def test_weights_simple(by_loc_ctx):
    """
    Testing that backend weights are respected.
    """
    by_loc_ctx.by_location_start_balancer()
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})


def test_weights_empty_file(by_loc_ctx):
    by_loc_ctx.by_location_start_balancer(weights_file_contents='')
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})


def test_weights_nonempty_file_start(by_loc_ctx):
    weights_file_content = 'id0,2.0\nid1,4.0'
    by_loc_ctx.by_location_start_balancer(weights_file_contents=weights_file_content)
    # expect 1/3 for id 0 and 2/3 for id 1
    check_weights(by_loc_ctx, {'id 1': [550, 750], 'id 0': [250, 400]})


def test_weights_nonempty_file_nonstart(by_loc_ctx):
    weights_file_content = 'id0,2.0\nid1,4.0'
    by_loc_ctx.by_location_start_balancer()
    by_loc_ctx.write_weights_file(weights_file_content)
    # expect 1/3 for id 0 and 2/3 for id 1
    check_weights(by_loc_ctx, {'id 1': [550, 750], 'id 0': [250, 400]})


def test_weights_file_reset(by_loc_ctx):
    weights_file_content = 'id0,1.0\nid1,2.0'
    by_loc_ctx.by_location_start_balancer(weights_file_contents=weights_file_content)
    # expect 1/3 for id 0 and 2/3 for id 1
    check_weights(by_loc_ctx, {'id 1': [550, 750], 'id 0': [250, 400]})

    by_loc_ctx.write_weights_file('')
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})


def test_weights_file_remove_restore_default_state(by_loc_ctx):
    by_loc_ctx.by_location_start_balancer(weights_file_contents='')
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})

    by_loc_ctx.write_weights_file('id0,1.0\nid1,0.0\nid2,2.0')
    # expect 1/3 for id 0 and 2/3 for id 2
    check_weights(by_loc_ctx, {'id 2': [550, 750], 'id 0': [250, 400]})

    by_loc_ctx.erase_weights_file()
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})


def test_weights_file_with_backend_switch(by_loc_ctx):
    weights_file_content = 'id0,0.0\nid1,1.0'
    by_loc_ctx.by_location_start_balancer(weights_file_contents=weights_file_content)
    # expect 100% for id 1
    check_weights(by_loc_ctx, {'id 1': [100, 100]}, count=100)

    by_loc_ctx.write_weights_file('id0,1.0\nid1,0.0')
    # expect 100% for id 0
    check_weights(by_loc_ctx, {'id 0': [100, 100]}, count=100)


def test_weights_file_with_all_zeros(by_loc_ctx):
    weights_file_content = 'id0,0.0\nid1,0.0'
    by_loc_ctx.by_location_start_balancer(weights_file_contents=weights_file_content)
    # expect 100% for on_error
    check_weights(by_loc_ctx, {'on_error': [100, 100]}, count=100)

    by_loc_ctx.erase_weights_file()
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})


def test_negative_weight(by_loc_ctx):
    weights_file_content = 'id0,1.0\nid1,-1.0'
    by_loc_ctx.by_location_start_balancer(weights_file_contents=weights_file_content)
    # expect 100% for id 0
    check_weights(by_loc_ctx, {'id 0': [100, 100]}, count=100)


def test_invalid_weights_file(by_loc_ctx):
    weights_file_content = 'id00.0\nid1,0.0'
    by_loc_ctx.by_location_start_balancer(weights_file_contents=weights_file_content)
    # expect 2/3 for id 0 and 1/3 for id 1 (weights from config are used)
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})

    time.sleep(3)

    errorlog = by_loc_ctx.manager.fs.read_file(by_loc_ctx.balancer.config.errorlog)
    assert 'UpdateWeights Error parsing weight for weights_file:' in errorlog


def test_preferred_location(by_loc_ctx):
    by_loc_ctx.by_location_start_balancer(preferred_location='id1')
    # expect 100% for id 1
    check_weights(by_loc_ctx, {'id 1': [100, 100]}, count=100)


def test_preferred_location_zero_weight(by_loc_ctx):
    by_loc_ctx.by_location_start_balancer(preferred_location='id2')
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})


def test_preferred_location_absent(by_loc_ctx):
    by_loc_ctx.by_location_start_balancer(preferred_location='idX2')
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})


def test_preferred_location_switch(by_loc_ctx):
    by_loc_ctx.by_location_start_balancer(preferred_location='id1')
    # expect 100% for id 1
    check_weights(by_loc_ctx, {'id 1': [100, 100]}, count=100)

    by_loc_ctx.write_location_file('')
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})

    by_loc_ctx.write_location_file('id0')
    # expect 100% for id 0
    check_weights(by_loc_ctx, {'id 0': [100, 100]}, count=100)

    by_loc_ctx.erase_location_file()
    # expect 100% for id 1
    check_weights(by_loc_ctx, {'id 1': [100, 100]}, count=100)


def test_preferred_location_switch_off_by_default(by_loc_ctx):
    by_loc_ctx.by_location_start_balancer()
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})

    by_loc_ctx.write_location_file('')
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})

    by_loc_ctx.write_location_file('id0')
    # expect 100% for id 0
    check_weights(by_loc_ctx, {'id 0': [100, 100]}, count=100)

    by_loc_ctx.erase_location_file()
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})


@pytest.mark.parametrize('amount_quorum', [None, 3, 2, 1, 0])
@pytest.mark.parametrize('quorum', [None, 1.0, 0.6, 0.3, 0.0])
def test_location_max_pessimization(by_loc_ctx, amount_quorum, quorum):
    by_loc_ctx.by_location_start_balancer(
        config=ByLocation2LvlConfig,
        root_quorum=quorum,
        root_amount_quorum=amount_quorum,
        amount_quorum1=2,
        amount_quorum2=2,
        amount_quorum3=2
    )

    expected_nonzero = 3
    if amount_quorum is not None and quorum is not None:
        expected_nonzero = max(amount_quorum, ceil(quorum*3))
    elif amount_quorum is not None:
        expected_nonzero = amount_quorum
    elif quorum is not None:
        expected_nonzero = ceil(quorum*3)

    if expected_nonzero > 0:
        check_availability(by_loc_ctx, expected_nonzero)
    else:
        check_availability(by_loc_ctx, 1, ['on_error'], required_nonzero=['on_error'])


@pytest.mark.parametrize('amount_quorum1', [2, 1])
@pytest.mark.parametrize('amount_quorum2', [2, 1])
@pytest.mark.parametrize('amount_quorum3', [2, 1])
def test_location_unavailable(by_loc_ctx, amount_quorum1, amount_quorum2, amount_quorum3):
    root_amount_quorum = 2
    by_loc_ctx.by_location_start_balancer(
        config=ByLocation2LvlConfig,
        root_amount_quorum=root_amount_quorum,
        amount_quorum1=amount_quorum1,
        amount_quorum2=amount_quorum2,
        amount_quorum3=amount_quorum3
    )
    expected_nonzero = 0
    required_nonzero = []
    if amount_quorum1 < 2:
        expected_nonzero += 1
        required_nonzero.append("id0")
    if amount_quorum2 < 2:
        expected_nonzero += 1
        required_nonzero.append("id1")
    if amount_quorum3 < 2:
        expected_nonzero += 1
        required_nonzero.append("id2")

    check_availability(
        by_loc_ctx,
        max(expected_nonzero, root_amount_quorum),
        required_nonzero=required_nonzero
    )


def test_children_quorum_override(by_loc_ctx):
    by_loc_ctx.by_location_start_balancer(
        config=ByLocation2LvlConfig,
        root_amount_quorum=0,
        amount_quorum1=2,
        amount_quorum2=2,
        amount_quorum3=2
    )

    check_availability(by_loc_ctx, 1, ['on_error'], required_nonzero=['on_error'])

    by_loc_ctx.write_quorums_file('id0,-,1')
    check_availability(by_loc_ctx, 1, ['id0'], required_nonzero=['id0'])

    by_loc_ctx.write_quorums_file('id1,-,1\nid2,-,1')
    check_availability(by_loc_ctx, 2, ['id1', 'id2'], required_nonzero=['id1', 'id2'])

    by_loc_ctx.write_quorums_file('id1,-,1')
    check_availability(by_loc_ctx, 1, ['id1'], required_nonzero=['id1'])

    by_loc_ctx.write_quorums_file('id1,-,1\nid2,-,1\nid0,-,1')
    check_availability(by_loc_ctx, 3, ['id0', 'id1', 'id2'], required_nonzero=['id0', 'id1', 'id2'])

    by_loc_ctx.erase_quorums_file()
    check_availability(by_loc_ctx, 1, ['on_error'], required_nonzero=['on_error'])

    by_loc_ctx.write_quorums_file('*,-,1')
    check_availability(by_loc_ctx, 3, ['id0', 'id1', 'id2'], required_nonzero=['id0', 'id1', 'id2'])


def test_zeroweighted_and_unavailable(by_loc_ctx):
    by_loc_ctx.by_location_start_balancer(
        config=ByLocation2LvlConfig,
        root_amount_quorum=2,
        amount_quorum1=2,
        amount_quorum2=1,
        amount_quorum3=1
    )

    check_availability(by_loc_ctx, 2, ['id1', 'id2'], required_nonzero=['id1', 'id2'])

    by_loc_ctx.write_weights_file('id2,0.0')
    check_availability(by_loc_ctx, 2, ['id1', 'id0'], required_nonzero=['id1', 'id0'])

    by_loc_ctx.write_weights_file('id2,0.0\nid0,0.0')
    check_availability(by_loc_ctx, 1, ['id1'], required_nonzero=['id1'])

    by_loc_ctx.erase_weights_file()
    check_availability(by_loc_ctx, 2, ['id1', 'id2'], required_nonzero=['id1', 'id2'])


def test_by_locations_quorum_override(by_loc_ctx):
    by_loc_ctx.by_location_start_balancer(
        config=ByLocation2LvlConfig,
        root_amount_quorum=2,
        amount_quorum1=2,
        amount_quorum2=2,
        amount_quorum3=1
    )

    check_availability(by_loc_ctx, 2, required_nonzero=['id2'])

    by_loc_ctx.write_quorums_file('root,-,1')
    check_availability(by_loc_ctx, 1, ['id2'], required_nonzero=['id2'])

    by_loc_ctx.write_quorums_file('root,-,3')
    check_availability(by_loc_ctx, 3, ['id0', 'id1', 'id2'], required_nonzero=['id0', 'id1', 'id2'])

    by_loc_ctx.erase_quorums_file()
    check_availability(by_loc_ctx, 2, required_nonzero=['id2'])


def test_unavailable_preferred_location(by_loc_ctx):
    by_loc_ctx.by_location_start_balancer(
        config=ByLocation2LvlConfig,
        root_amount_quorum=2,
        amount_quorum1=2,
        amount_quorum2=2,
        amount_quorum3=1,
        preferred_location='id0'
    )

    check_availability(by_loc_ctx, 1, ['id0'], required_nonzero=['id0'])

    by_loc_ctx.write_quorums_file('root,-,1')
    check_availability(by_loc_ctx, 1, ['id2'], required_nonzero=['id2'])

    by_loc_ctx.write_quorums_file('root,-,3')
    check_availability(by_loc_ctx, 1, ['id0'], required_nonzero=['id0'])


def test_by_name_from_header_policy(by_loc_ctx):
    by_loc_ctx.by_location_start_balancer(policy='by_name_from_header')
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})
    # expect 100% for id 0
    check_weights(by_loc_ctx, {'id 0': [100, 100]}, count=100, request=http.request.get(headers={'X-Yandex-Balancing-Hint': 'id0'}))
    # expect 100% for id 1
    check_weights(by_loc_ctx, {'id 1': [100, 100]}, count=100, request=http.request.get(headers={'X-Yandex-Balancing-Hint': 'id1'}))
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]}, request=http.request.get(headers={'X-Yandex-Balancing-Hint': 'id2'}))


def test_by_hash_policy(by_loc_ctx):
    by_loc_ctx.by_location_start_balancer(policy='by_hash')
    # expect 2/3 for id 0 and 1/3 for id 1
    check_weights(by_loc_ctx, {'id 0': [550, 750], 'id 1': [250, 400]})

    request = http.request.get(headers={'hash': 'xxx'})
    resp = by_loc_ctx.perform_request(request)
    asserts.status(resp, 200)
    expected = resp.data.content
    # expect 100% for expected
    check_weights(by_loc_ctx, {expected: [100, 100]}, count=100, request=request)
