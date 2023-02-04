# -*- coding: utf-8 -*-
import pytest
import time
from collections import defaultdict

import balancer.test.plugin.context as mod_ctx

from configs import RrobinConfig, RrobinNamedConfig, RrobinCorrowaConfig

from balancer.test.util import asserts
from balancer.test.util.predef import http


class RrobinContext(object):
    WEIGHTS_REREAD_TIMEOUT = 5

    def __init__(self):
        super(RrobinContext, self).__init__()
        self.__weights_file = self.manager.fs.create_file('weights')  # FIXME: BALANCER-830

    @property
    def weights_file(self):
        return self.__weights_file

    @classmethod
    def __wait_reread(cls):
        time.sleep(cls.WEIGHTS_REREAD_TIMEOUT)

    def __write_weights_file(self, contents):
        self.manager.fs.rewrite(self.weights_file, contents)

    def write_weights_file(self, contents):
        self.__write_weights_file(contents)
        self.__wait_reread()  # make sure the file was re-read

    def __erase_weights_file(self):
        self.manager.fs.remove(self.weights_file)

    def erase_weights_file(self):
        self.__erase_weights_file()
        self.__wait_reread()

    def rr_start_balancer(self, has_weights_file=False, weights_file_contents='', config=RrobinConfig, **balancer_kwargs):
        balancer_kwargs['weights_file'] = self.weights_file

        if has_weights_file:
            self.__write_weights_file(weights_file_contents)
        else:
            self.__erase_weights_file()

        balancer = self.start_balancer(config(**balancer_kwargs))
        if has_weights_file:
            self.__wait_reread()
        return balancer


rr_ctx = mod_ctx.create_fixture(RrobinContext)


def is_sublist(left, right):
    """
    Check if left is a sublist of right
    """
    len_diff = len(right) - len(left)
    if len_diff < 0:
        return False
    for start in range(len_diff + 1):
        if left == right[start:start + len(left)]:
            return True
    return False


def request_series(rr_ctx, expected_contents, requests=None, requests_len=None):
    """
    This is a main testing method: making series of requests and matching
    corresponging responses. Use it to test how weights choosing works:
    the sequence of requests should correpsond to the sequence of responses,
    if weights were choosed carefully.
    @param expected_contents - list of bodies of responses
    @param requests - list of requests, if it is None,
    then list of http.request.get() of length of responses list is used.
    """

    if requests is None:
        if requests_len is None:
            requests_len = len(expected_contents)
        requests = [http.request.get()] * requests_len

    contents = list()
    for req in requests:
        resp = rr_ctx.perform_request(req)
        asserts.status(resp, 200)
        contents.append(resp.data.content)

    assert is_sublist(contents, expected_contents)


def standard_run(rr_ctx):
    """
    Running a series of tests for unmodified config and empty file. Make
    sure the balancer is already started.
    """
    requests_len = 7
    answers = ['id 0', 'id 0', 'id 1'] * 3
    request_series(rr_ctx, answers, requests_len=requests_len)


def base_weights_test(rr_ctx, weights_file_content, runs, config=RrobinConfig):
    rr_ctx.rr_start_balancer(has_weights_file=True, weights_file_contents=weights_file_content, config=config)
    results = defaultdict(int)
    for _ in xrange(0, runs):
        response = rr_ctx.perform_request(http.request.get())
        asserts.status(response, 200)
        results[response.data.content] += 1

    return list(results.values())


def test_start(rr_ctx):
    """
    Testing that balancer starts and a single request is processed.
    """
    rr_ctx.rr_start_balancer()
    request_series(rr_ctx, ['id 0'])


def test_weights_simple(rr_ctx):
    """
    SEPE-5939
    Series of requests to check that weights mechanism works.
    """
    rr_ctx.rr_start_balancer()
    standard_run(rr_ctx)


def test_weights_empty_file(rr_ctx):
    """
    SEPE-5939
    Series of tests with an empty weights file
    """
    rr_ctx.rr_start_balancer(has_weights_file=True)
    standard_run(rr_ctx)


def test_weights_nonempty_file_start(rr_ctx):
    """
    SEPE-5939
    Series of tests with reversed default weights
    """
    weights_file_content = '2.0\n4.0'
    rr_ctx.rr_start_balancer(has_weights_file=True,
                             weights_file_contents=weights_file_content)
    requests_len = 8
    answers = ['id 1', 'id 1', 'id 0'] * 3 + ['id 1']
    request_series(rr_ctx, answers, requests_len=requests_len)


def test_weights_nonempty_file_nonstart(rr_ctx):
    """
    SEPE-5939
    Series of tests with reversed default weights
    """
    rr_ctx.rr_start_balancer(has_weights_file=False)
    weights_file_content = '2.0\n4.0'
    rr_ctx.write_weights_file(weights_file_content)
    requests_len = 8
    answers = ['id 1', 'id 1', 'id 0'] * 3 + ['id 1']
    request_series(rr_ctx, answers, requests_len=requests_len)


def test_weights_file_reset(rr_ctx):
    """
    SEPE-5939
    Series of tests with reversed default weights and removing the file
    later.
    """
    weights_file_content = '1.0\n2.0'
    rr_ctx.rr_start_balancer(has_weights_file=True,
                             weights_file_contents=weights_file_content)
    requests_len = 8
    answers = ['id 1', 'id 1', 'id 0'] * 3 + ['id 1']
    request_series(rr_ctx, answers, requests_len=requests_len)
    rr_ctx.write_weights_file('')
    standard_run(rr_ctx)


def test_weights_file_with_backend_switch(rr_ctx):
    """
    BALANCER-2938
    Start with the first backend disabled. Then switch.
    """
    weights_file_content = '0.0\n1.0'
    rr_ctx.rr_start_balancer(
        has_weights_file=True,
        weights_file_contents=weights_file_content)

    requests_len = 7
    answers = ['id 1'] * requests_len
    request_series(rr_ctx, answers, requests_len=requests_len)
    rr_ctx.write_weights_file('1.0\n0.0')
    answers = ['id 0'] * requests_len
    request_series(rr_ctx, answers, requests_len=requests_len)

    time.sleep(2)

    unistat = rr_ctx.get_unistat()
    assert unistat['rr-disabling_all_backends_update_count_summ'] == 0


def test_weights_file_with_all_zeroes(rr_ctx):
    """
    BALANCER-2938
    Rewrite weights file with only zeroes inside and make sure that
    in this case original weight is restored for not updated backend.
    """
    weights_file_content = '1.0\n2.0'
    rr_ctx.rr_start_balancer(
        has_weights_file=True,
        weights_file_contents=weights_file_content)

    requests_len = 7
    answers = ['id 1', 'id 1', 'id 0'] * 3
    request_series(rr_ctx, answers, requests_len=requests_len)

    rr_ctx.write_weights_file('0.0\n')
    answers = ['id 1'] * requests_len
    request_series(rr_ctx, answers, requests_len=requests_len)

    time.sleep(2)

    unistat = rr_ctx.get_unistat()
    assert unistat['rr-disabling_all_backends_update_count_summ'] == 0


def test_weights_file_ignore_update_disabling_all_backends(rr_ctx):
    """
    BALANCER-2938
    If an update removing all backends occurred, we just skip it.
    """
    weights_file_content = '2.0\n1.0\n0.0'
    rr_ctx.rr_start_balancer(
        has_weights_file=True,
        weights_file_contents=weights_file_content)

    requests_len = 7
    answers = ['id 0', 'id 0', 'id 1'] * 3
    request_series(rr_ctx, answers, requests_len=requests_len)

    rr_ctx.write_weights_file('0.0\n0.0')
    request_series(rr_ctx, answers, requests_len=requests_len)

    time.sleep(2)

    unistat = rr_ctx.get_unistat()
    assert unistat['rr-disabling_all_backends_update_count_summ'] == 1

    errorlog = rr_ctx.manager.fs.read_file(rr_ctx.balancer.config.errorlog)
    assert 'UpdateWeights Error because it disables all backends for weights_file: 0.0\n0.0' in errorlog


def test_weights_file_remove_restore_default_state(rr_ctx):
    """
    BALANCER-2938
    If the weight file is removed, default weights are restored.
    """
    rr_ctx.rr_start_balancer(has_weights_file=True)

    requests_len = 7
    answers = ['id 1', 'id 0', 'id 0'] * 3
    request_series(rr_ctx, answers, requests_len=requests_len)

    rr_ctx.write_weights_file('1.0\n0.0\n2.0')
    answers = ['id 2', 'id 2', 'id 0'] * 3
    request_series(rr_ctx, answers, requests_len=requests_len)

    rr_ctx.erase_weights_file()
    answers = ['id 1', 'id 0', 'id 0'] * 3
    request_series(rr_ctx, answers, requests_len=requests_len)

    time.sleep(2)

    unistat = rr_ctx.get_unistat()
    assert unistat['rr-disabling_all_backends_update_count_summ'] == 0


def test_weights_file_less_lines(rr_ctx):
    """
    SEPE-5939
    Series of tests with weights file containing less records then the
    number of backends in balancer's config. Expected behaviour is
    round-robining weights for backedns.
    """
    weights_file_content = '1.0'  # all backedns are equal
    counts = base_weights_test(rr_ctx, weights_file_content, 30)
    assert len(counts) == 2 and abs(max(counts) - min(counts)) <= 1  # all backends were equal


def test_weights_file_more_lines(rr_ctx):
    """
    SEPE-5939
    Series of tests with weights file containing more records then the
    number of backends in balancer's config. Expected behaviour is
    ignoring excess weights.
    """
    weights_file_content = '1.0\n1.0\n0.0\n3.0'  # ignore 8 for two-backend conf
    counts = base_weights_test(rr_ctx, weights_file_content, 32)
    assert len(counts) == 2 and abs(max(counts) - min(counts)) <= 1  # all backends were equal


def test_negative_weight(rr_ctx):
    """
    SEPE-5939
    Negative weight should disable the backend
    """
    weights_file_content = '1.0\n-1.0'
    counts = base_weights_test(rr_ctx, weights_file_content, 10)
    assert len(counts) == 1


def test_invalid_weights_file(rr_ctx):
    """
    BALANCER-1643
    If weights_file is envalid, balancer writes a record in errorlog.
    """
    weights_file_content = '1.01.0'
    base_weights_test(rr_ctx, weights_file_content, 10)

    time.sleep(3)

    errorlog = rr_ctx.manager.fs.read_file(rr_ctx.balancer.config.errorlog)
    assert 'UpdateWeights Error parsing weight for weights_file:' in errorlog


def test_named_weights(rr_ctx):
    """
    SEPE-7936
    Weights file with named entities
    """
    weights_file_content = 'first,1.0\nsecond,1.0'
    counts = base_weights_test(rr_ctx, weights_file_content, 30, RrobinNamedConfig)
    assert len(counts) == 2 and abs(max(counts) - min(counts)) <= 1  # all backends were equal


def test_named_negative_weights(rr_ctx):
    """
    SEPE-7936
    Weights file with named entities. Negative weights should work too
    """
    weights_file_content = 'first,1.0\nsecond,-1.0'
    counts = base_weights_test(rr_ctx, weights_file_content, 10, RrobinNamedConfig)
    assert len(counts) == 1


def test_named_weights_reordered(rr_ctx):
    """
    SEPE-7936
    Reordered entities in weights file
    """
    weights_file_content = 'second,-1.0\nfirst,1.0'
    counts = base_weights_test(rr_ctx, weights_file_content, 10, RrobinNamedConfig)
    assert len(counts) == 1


def test_named_weights_restore(rr_ctx):
    """
    SEPE-7936
    Weights file with named entities. The ones that had not been named
    should restore their weights
    """
    weights_file_content = 'second,2.0'
    counts = base_weights_test(rr_ctx, weights_file_content, 30, RrobinNamedConfig)
    assert len(counts) == 2 and abs(max(counts) - min(counts)) <= 1  # all backends were equal


def test_named_weights_unknown_name(rr_ctx):
    """
    SEPE-7936
    Weights file with named entities. Unknown name in weights file
    should be ignored
    """
    weights_file_content = 'third,10.0\nfirst,1.0\nsecond,1.0'
    counts = base_weights_test(rr_ctx, weights_file_content, 30, RrobinNamedConfig)
    assert len(counts) == 2 and abs(max(counts) - min(counts)) <= 1  # all backends were equal


def base_test_all_switched_off(rr_ctx, weights_pair):
    """
    BALANCER-234
    Balancer should not fail if all rr's backends are switched off
    """
    rr_ctx.rr_start_balancer(config=RrobinNamedConfig, first_weight=weights_pair[0], second_weight=weights_pair[1])
    request_series(rr_ctx, ['on_error', 'on_error'])


def test_zero_weights_start(rr_ctx):
    """
    BALANCER-234
    Balancer should not fail if all rr's backends are switched off.
    Switching off with zero weights.
    """
    base_test_all_switched_off(rr_ctx, (0, 0))


def test_negative_weights_start(rr_ctx):
    """
    BALANCER-234
    Balancer should not fail if all rr's backends are switched off.
    Switching off with zero weights.
    """
    base_test_all_switched_off(rr_ctx, (-1, -2))


def base_test_all_switched_off_file(rr_ctx, weights_pair):
    """
    BALANCER-234
    Balancer doesn't apply weights disabling all backends.
    After deleting file with weights, balancer's state should
    not change.
    """
    rr_ctx.rr_start_balancer(
        config=RrobinNamedConfig,
        has_weights_file=True,
        weights_file_contents='first,%d\nsecond,%d' % weights_pair,
    )
    standard_run(rr_ctx)
    rr_ctx.erase_weights_file()
    standard_run(rr_ctx)


def test_all_switched_off_zero_file(rr_ctx):
    base_test_all_switched_off_file(rr_ctx, (0, 0))


def test_all_switched_off_negative_file(rr_ctx):
    base_test_all_switched_off_file(rr_ctx, (-1, -2))


def run_randomization(rr_ctx, index=None):
    if index is None:
        request = http.request.get()
    else:
        request = http.request.get('/{}'.format(index))

    initial_requests = []
    for _ in xrange(10):
        response = rr_ctx.perform_request(request)
        initial_requests.append(response.data.content)

    for _ in xrange(20):
        response = rr_ctx.perform_request(request)
        asserts.status(response, 200)

    after_randomization_requests = []
    for _ in xrange(10):
        response = rr_ctx.perform_request(request)
        after_randomization_requests.append(response.data.content)

    return initial_requests, after_randomization_requests


RR_COUNT = 20


def run_randomization_mult(rr_ctx):
    first_counter = [0] * 10
    second_counter = [0] * 10
    for i in range(RR_COUNT):
        reqs, _ = run_randomization(rr_ctx, i)
        for j, req in enumerate(reqs):
            if req == 'id 0':
                first_counter[j] += 1
            elif req == 'id 1':
                second_counter[j] += 1
            else:
                assert False, 'unreachable!'
    return first_counter, second_counter


def check_random(rr_ctx):
    first_counter, second_counter = run_randomization_mult(rr_ctx)
    for j, value in enumerate(first_counter):
        assert value != 0, 'fail on step {}'.format(j)
    for j, value in enumerate(second_counter):
        assert value != 0, 'fail on step {}'.format(j)


@pytest.fixture(params=[(None, None), (None, True), (True, True)], ids=['balancer1-corrowa', 'balancer2-corrowa', 'balancer2-ris'])
def mode(request):
    return request.param


def test_initial_state_no_randomization(rr_ctx, mode):
    """
    BALANCER-1027
    Checking that randomization does not take place if corrowa is zero,
    """
    corrowa = 0
    use_randomize_initial_state, use_balancer2 = mode
    rr_ctx.rr_start_balancer(
        config=RrobinNamedConfig,
        corrowa=corrowa,
        use_randomize_initial_state=use_randomize_initial_state,
        use_balancer2=use_balancer2
    )

    initial_requests, after_randomization_requests = run_randomization(rr_ctx)

    assert is_sublist(initial_requests, ['id 0', 'id 0', 'id 1'] * 4)
    assert initial_requests == after_randomization_requests


def test_convergence_after_randomization(rr_ctx, mode):
    """
    BALANCER-1027
    After randomization algorithm must converge in at most corrowa * number of backends requests
    """
    corrowa = 15
    use_randomize_initial_state, use_balancer2 = mode
    rr_ctx.rr_start_balancer(
        config=RrobinNamedConfig,
        corrowa=corrowa,
        use_randomize_initial_state=use_randomize_initial_state,
        use_balancer2=use_balancer2
    )

    initial_requests, after_randomization_requests = run_randomization(rr_ctx)

    if use_randomize_initial_state:  # unlike corrowa, there is no disbalance at start
        assert is_sublist(initial_requests, ['id 0', 'id 0', 'id 1'] * 4)

    assert is_sublist(after_randomization_requests, ['id 0', 'id 0', 'id 1'] * 4)


def test_initial_state_randomization(rr_ctx, mode):
    """
    BALANCER-1027
    Checking that randomization takes place if corrowa is not zero,
    """
    corrowa = 30
    use_randomize_initial_state, use_balancer2 = mode
    rr_ctx.rr_start_balancer(
        config=RrobinCorrowaConfig,
        corrowa=corrowa,
        use_randomize_initial_state=use_randomize_initial_state,
        use_balancer2=use_balancer2,
        rr_count=RR_COUNT,
    )

    check_random(rr_ctx)


def test_weights_file_state_randomization(rr_ctx, mode):
    """
    BALANCER-1027
    Checking that randomization takes place if corrowa is not zero,
    Randomization applies after weights file is changed
    """
    corrowa = 30
    use_randomize_initial_state, use_balancer2 = mode
    rr_ctx.rr_start_balancer(
        config=RrobinCorrowaConfig,
        corrowa=corrowa,
        has_weights_file=True,
        use_randomize_initial_state=use_randomize_initial_state,
        use_balancer2=use_balancer2,
        rr_count=RR_COUNT,
    )

    run_randomization_mult(rr_ctx)

    rr_ctx.write_weights_file('first,1.0\nsecond,2.0\n')

    check_random(rr_ctx)


def test_weights_file_erasure_state_randomization(rr_ctx, mode):
    """
    BALANCER-1027
    Checking that randomization takes place if corrowa is not zero,
    Randomization applies after weights file is erased.
    """
    corrowa = 30
    weights = 'first,1.0\nsecond,2.0\n'
    use_randomize_initial_state, use_balancer2 = mode
    rr_ctx.rr_start_balancer(
        config=RrobinCorrowaConfig,
        corrowa=corrowa,
        has_weights_file=True,
        weights_file_contents=weights,
        use_randomize_initial_state=use_randomize_initial_state,
        use_balancer2=use_balancer2,
        rr_count=RR_COUNT,
    )

    run_randomization_mult(rr_ctx)

    rr_ctx.erase_weights_file()

    check_random(rr_ctx)
