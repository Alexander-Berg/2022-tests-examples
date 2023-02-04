import mock
import pytest

from yt.wrapper import ypath_join

import maps.pylibs.yandex_environment.environment as yenv

from maps.pylibs.yt.lib import PythonCmd
from maps.analyzer.pylibs.envkit.yt import (
    get_context, sandbox_task_url,
    SVN_REVISION_ATTRIBUTE_NAME, PROCESS_ATTRIBUTE_NAME,
    SANDBOX_TASK_ATTRIBUTE_NAME, copy_attribute,
)
import maps.analyzer.pylibs.envkit.graph as graph
import maps.analyzer.pylibs.envkit.config as config

from .common import with_env


def test_pools():
    def run(env, test_case, mapper, expected):
        with with_env(env):
            old_tentative = config.TENTATIVE_POOL_TREES
            old_pool = config.YAMAPS_POOL
            old_tag = config.YAMAPS_TAG
            config.TENTATIVE_POOL_TREES = True
            config.YAMAPS_POOL = test_case.pool
            config.YAMAPS_TAG = test_case.yamaps

            with mock.patch('yt.wrapper.client.Yt.run_map') as run_map:
                with get_context() as ctx:
                    ctx.run_map(mapper, '//src', '//dst')
                run_map.assert_called_once_with(
                    MatchCmdClass(mapper),
                    '//src',
                    '//dst',
                    local_files=[],
                    yt_files=MatchAny(),
                    spec=expected,
                )

            config.TENTATIVE_POOL_TREES = old_tentative
            config.YAMAPS_POOL = old_pool
            config.YAMAPS_TAG = old_tag

    # shouldn't use pool if not allowed, but may set `yamaps` tag if required by using graph files
    run(yenv.Environment.DEVELOPMENT, TestCase(yamaps=True, pool=False), graph_mapper(), ExpectedGraphOpSpec(yamaps=True, pool=False))
    run(yenv.Environment.DEVELOPMENT, TestCase(yamaps=True, pool=False), no_graph_mapper(), ExpectedNonGraphOpSpec(yamaps=True, pool=False))
    run(yenv.Environment.DEVELOPMENT, TestCase(yamaps=False, pool=False), graph_mapper(), ExpectedGraphOpSpec(yamaps=True, pool=False))
    run(yenv.Environment.DEVELOPMENT, TestCase(yamaps=False, pool=False), no_graph_mapper(), ExpectedNonGraphOpSpec(yamaps=False, pool=False))
    # can be run with no pool if not required
    run(yenv.Environment.DEVELOPMENT, TestCase(yamaps=True, pool=True), graph_mapper(), ExpectedGraphOpSpec(yamaps=True, pool=True))
    run(yenv.Environment.DEVELOPMENT, TestCase(yamaps=True, pool=True), no_graph_mapper(), ExpectedNonGraphOpSpec(yamaps=True, pool=True))
    run(yenv.Environment.DEVELOPMENT, TestCase(yamaps=False, pool=True), graph_mapper(), ExpectedGraphOpSpec(yamaps=True, pool=True))
    run(yenv.Environment.DEVELOPMENT, TestCase(yamaps=False, pool=True), no_graph_mapper(), ExpectedNonGraphOpSpec(yamaps=False, pool=False))


def test_context(local_yt):
    config.SVN_REVISION = '123456'
    config.SANDBOX_TASK_ID = '555666'
    config.YT_TITLE_PREFIX = 'some-task'

    with get_context() as ytc:
        t = ytc.create_temp_table()
        assert ytc.get(ypath_join(t, '@' + SVN_REVISION_ATTRIBUTE_NAME)) == config.SVN_REVISION
        assert ytc.get(ypath_join(t, '@' + SANDBOX_TASK_ATTRIBUTE_NAME)) == sandbox_task_url(config.SANDBOX_TASK_ID)
        assert ytc.get(ypath_join(t, '@' + PROCESS_ATTRIBUTE_NAME)) == config.YT_TITLE_PREFIX
        ytc.write_table(t, [{'foo': 'bar'}])
        t2 = ytc.create_temp_table()
        op = ytc.run_map('cat', t, t2, format='json', title='cat', sync=False)
        spec = op.get_attributes(['spec'])
        assert spec['spec']['title'] == config.YT_TITLE_PREFIX + ':cat'
        assert spec['spec']['description'] == {
            SVN_REVISION_ATTRIBUTE_NAME: config.SVN_REVISION,
            SANDBOX_TASK_ATTRIBUTE_NAME: sandbox_task_url(config.SANDBOX_TASK_ID),
        }


def test_copy_attribute(local_yt):
    with get_context() as ytc:
        source = ytc.create_temp_table()
        target = ytc.create_temp_table()
        ytc.set(ypath_join(source, '@foo'), 'bar')
        copy_attribute(ytc, source, target, 'foo')
        assert ytc.get(ypath_join(target, '@foo')) == 'bar'
        with pytest.raises(AssertionError):
            copy_attribute(ytc, source, target, 'non-existent-attr')
        with pytest.raises(AssertionError):
            copy_attribute(ytc, source, '//non-existent-table', 'foo')


class TestCase(object):
    def __init__(self, yamaps=False, pool=False):
        self.yamaps = yamaps
        self.pool = pool


class ExpectedGraphOpSpec(object):
    def __init__(self, yamaps=None, pool=None):
        self.yamaps = yamaps
        self.pool = pool

    def __eq__(self, other):
        return (
            (other.get('pool_trees') == ['yamaps']) == ('include_memory_mapped_files' in other.get('mapper', {})) and
            ('tentative_pool_trees' not in other)
        )


class ExpectedNonGraphOpSpec(object):
    def __init__(self, yamaps=None, pool=None):
        self.yamaps = yamaps
        self.pool = pool

    def __eq__(self, other):
        return (
            ('yamaps' in other.get('pool_trees', [])) == self.yamaps and
            ('pool' in other.get('scheduling_options_per_pool_tree', {}).get('yamaps', {})) == self.pool and
            ('pool' not in other) and
            ('tentative_pool_trees' in other) and
            ('include_memory_mapped_files' not in other.get('mapper', {}))
        )


class MatchCmdClass(object):
    def __init__(self, cmd):
        self.cmd = cmd

    def __eq__(self, wrapped):
        return isinstance(wrapped.origin, self.cmd.cmd_class)


class MatchAny(object):
    def __eq__(self, other):
        return True


class MyCmd(object):
    def __init__(self, file=None):
        self.file = file


def graph_mapper():
    return PythonCmd(
        MyCmd,
        file=graph.param('data.mms.2'),
    )


def no_graph_mapper():
    return PythonCmd(MyCmd)
