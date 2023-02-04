# coding: utf-8
import pytest

import btestlib.shared as shared


def first_long_action(shared_data, before):
    with shared.SharedBlock(shared_data=shared_data, before=before, block_name='first') as block:
        block.validate()
        print 'first long action'


def second_long_action(shared_data, before):
    with shared.SharedBlock(shared_data=shared_data, before=before, block_name='second') as block:
        block.validate()
        print 'second long action'


@pytest.mark.shared(block='first')
def test_first_1(shared_data):
    with shared.SharedBefore(shared_data=shared_data, cache_vars=['x']) as before:
        before.validate()
        print 'inside shared before'
        x = 1

    first_long_action(shared_data=shared_data, before=before)

    print 'after long action x={}'.format(x)

# @pytest.mark.shared(block='first')
# def test_first_2(shared_data):
#     with shared.SharedBefore(shared_data=shared_data, cache_vars=['x']) as before:
#         before.validate()
#         print 'inside shared before'
#         x = 2
#
#     first_long_action(shared_data=shared_data, before=before)
#
#     print 'after long action x={}'.format(x)
#
#
# @pytest.mark.shared(block='second')
# def test_second_1(shared_data):
#     with shared.SharedBefore(shared_data=shared_data, cache_vars=['x']) as before:
#         before.validate()
#         print 'inside shared before'
#         x = 3
#
#     second_long_action(shared_data=shared_data, before=before)
#
#     print 'after long action x={}'.format(x)
#
#
# @pytest.mark.shared(block='second')
# def test_second_2(shared_data):
#     with shared.SharedBefore(shared_data=shared_data, cache_vars=['x']) as before:
#         before.validate()
#         print 'inside shared before'
#         x = 4
#
#     second_long_action(shared_data=shared_data, before=before)
#
#     print 'after long action x={}'.format(x)
#
#
# def test_not_shared():
#     print 'inside test not shared'
