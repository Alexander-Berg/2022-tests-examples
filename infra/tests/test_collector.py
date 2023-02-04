# coding: utf-8

import pytest

import cppcollector


@pytest.fixture
def transmitter():
    return cppcollector.ClickhouseTransmitter(
        instances_table="instances",
        signals_table="signals",
        common_table="common",
        dummy=True
    )


def test_hist_response(transmitter):
    with open("hist.msgpack", "rb") as stream:
        response = cppcollector.HserverResponse(stream.read())
    transmitter.history_extend([("SAS_KERNEL_TEST.3", "ASEARCH_KERNEL_SAS")], response)
    transmitter.dump_instances(True)
    transmitter.dump_signals(True)
    transmitter.dump_common()


def test_rt_response(transmitter):
    with open("rt.msgpack", "rb") as stream:
        response = cppcollector.HserverResponse(stream.read())
    transmitter.realtime_extend([("SAS_KERNEL_TEST.3", "ASEARCH_KERNEL_SAS")], response)
    transmitter.dump_instances(True)
    transmitter.dump_signals(True)
    transmitter.dump_common()


def test_reset(transmitter):
    transmitter.reset()


def test_rt_containers_response(transmitter):
    with open("rt_containers.msgpack", "rb") as stream:
        response = cppcollector.HserverResponse(stream.read())
    transmitter.realtime_extend([('TEST_AGGR', 'ASEARCH'), ('TEST_AGGR_COPY', 'ASEARCH')], response)
    transmitter.dump_instances(True)
    transmitter.dump_signals(True)
    transmitter.dump_common()
