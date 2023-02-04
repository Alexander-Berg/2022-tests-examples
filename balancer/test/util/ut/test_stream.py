# -*- coding: utf-8 -*-
import pytest
from balancer.test.util.stream import io


DATA = 'Black Sabbath'


@pytest.mark.parametrize('length', [0, 1, len(DATA)])
def test_string_stream_recv_ok_len(length):
    ss = io.stream.StringStream(DATA)
    assert ss.recv(length) == DATA[:length]


@pytest.mark.parametrize('length', [-1, -100500])
def test_string_stream_recv_neg_len(length):
    ss = io.stream.StringStream(DATA)
    assert ss.recv(length) == DATA


def test_string_strem_recv_all():
    ss = io.stream.StringStream(DATA)
    assert ss.recv() == DATA


def test_string_stream_recv_quiet():
    ss = io.stream.StringStream(DATA)
    assert ss.recv_quiet() == DATA


def test_string_stream_has_data():
    ss = io.stream.StringStream(DATA)
    assert ss.has_data()
    ss.recv(len(DATA) - 1)
    assert ss.has_data()


def test_string_stream_no_data():
    ss = io.stream.StringStream(DATA)
    ss.recv(len(DATA))
    assert not ss.has_data()


def test_string_stream_recv_more():
    ss = io.stream.StringStream(DATA)
    with pytest.raises(io.stream.EndOfStream):
        ss.recv(len(DATA) + 1)


def test_string_stream_data():
    first_len = 3
    second_len = len(DATA) - first_len - 1
    ss = io.stream.StringStream(DATA)
    ss.recv(first_len)
    assert ss.data == DATA[:first_len]
    ss.recv(second_len)
    assert ss.data == DATA[:first_len + second_len]


def test_string_stream_clean():
    first_len = 3
    second_len = len(DATA) - first_len - 1
    ss = io.stream.StringStream(DATA)
    ss.recv(first_len)
    assert ss.data == DATA[:first_len]
    ss.clean()
    assert ss.data == ''
    ss.recv(second_len)
    assert ss.data == DATA[first_len:first_len + second_len]
