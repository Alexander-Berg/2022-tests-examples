from __future__ import unicode_literals

import utils


def test_evaluation(cwd, ctl_environment, ctl, request):
    p = utils.must_start_instancectl(ctl, request, ctl_environment)

    assert utils.wait_condition_is_true(cwd.join('test_one.txt').exists, 2, 0.1)

    with cwd.join('test_one.txt').open() as fd:
        lines = fd.readlines()
        assert len(lines) == 1
        assert ' '.join(lines[0].split()[2:]) == 'test_one.txt 27 ZZZ'

    assert utils.wait_condition_is_true(cwd.join('test_two.txt').exists, 2, 0.1)

    with cwd.join('test_two.txt').open() as fd:
        lines = fd.readlines()
        assert len(lines) == 1

        expected_number = 1543 + 1 + int(ctl_environment['BSCONFIG_IPORT'])

        assert ' '.join(lines[0].split()[2:]) == 'test_two.txt {} ZZZ 27'.format(expected_number)

    utils.must_stop_instancectl(ctl, process=p)
