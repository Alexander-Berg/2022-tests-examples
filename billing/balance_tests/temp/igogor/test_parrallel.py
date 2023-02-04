# coding: utf-8
import pprint
import time

import btestlib.reporter as reporter

# temp change strikes again

def test_first_test():
    print id(reporter.options())
    pprint.pprint(reporter.options())
    with reporter.reporting(level=reporter.Level.AUTO_ONE_LINE):
        for _ in range(10):
            time.sleep(1)
            pprint.pprint(reporter.options())

    assert False


def test_second_test():
    print id(reporter.options())
    time.sleep(2)
    pprint.pprint(reporter.options())
    with reporter.reporting(level=reporter.Level.MANUAL_ONLY):
        for _ in range(5):
            time.sleep(1)
            pprint.pprint(reporter.options())

    assert False


def test_third_test():
    print id(reporter.options())
    assert False


def test_fourth_test():
    print id(reporter.options())
    assert False
