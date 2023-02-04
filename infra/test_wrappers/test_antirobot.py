# coding: utf-8
import mock

from infra.awacs.proto import modules_pb2
from awacs.wrappers.config import Config
from awacs.wrappers.main import Antirobot, Accesslog, Balancer2, Regexp
from awacs.wrappers.base import Holder
from awtest.wrappers import get_validation_exception


def test_antirobot_validate():
    pb = modules_pb2.AntirobotModule()
    pb.nested.regexp.SetInParent()

    antirobot = Antirobot(pb)

    e = get_validation_exception(antirobot.validate)
    e.match('checker.*is required')

    pb.checker.SetInParent()
    antirobot.update_pb(pb)

    with mock.patch.object(antirobot.checker, 'validate') as validate_checker:
        with mock.patch.object(antirobot.nested.module, 'validate') as validate_nested:
            antirobot.validate()
    assert validate_checker.called
    assert validate_nested.called


def test_antirobot_to_config():
    # test with nested module
    pb = modules_pb2.AntirobotModule()
    pb.nested.regexp.SetInParent()
    pb.checker.balancer2.SetInParent()

    antirobot = Antirobot(pb)
    checker_config = Config({
        'some_module': Config({'param': 'value'}),
    })
    regexp_config = Config({
        'section_1': Config(),
        'section_2': Config(),
    })
    with mock.patch.object(antirobot.checker, 'to_config', return_value=checker_config):
        with mock.patch.object(antirobot.nested.module, 'to_config', return_value=regexp_config):
            antirobot_config = antirobot.to_config()
    antirobot_config.to_lua()

    def check_antirobot_config(c):
        t = c.table
        assert t['cut_request'] == antirobot.DEFAULT_CUT_REQUEST
        assert t['no_cut_request_file'] == antirobot.DEFAULT_NO_CUT_REQUEST_FILE
        assert t['cut_request_bytes'] == antirobot.DEFAULT_CUT_REQUEST_BYTES
        assert t['file_switch'] == antirobot.DEFAULT_FILE_SWITCH
        assert t['checker'] == checker_config
        assert t['module'].table['regexp'] == regexp_config
        assert len(t) == 6

    check_antirobot_config(antirobot_config)

    # test chained
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.modules.add()
    pb.accesslog.SetInParent()

    pb = holder_pb.modules.add()
    pb.antirobot.checker.balancer2.SetInParent()

    pb = holder_pb.modules.add()
    pb.regexp.SetInParent()

    holder = Holder(holder_pb)
    assert len(holder.chain.modules) == 3

    accesslog_config = Config({'log': './hello.txt'})
    module_0 = holder.chain.modules[0].module
    module_1 = holder.chain.modules[1].module
    module_2 = holder.chain.modules[2].module
    assert isinstance(module_0, Accesslog)
    assert isinstance(module_1, Antirobot)
    assert isinstance(module_1.checker.module, Balancer2)
    assert isinstance(module_2, Regexp)
    with mock.patch.object(module_0, 'to_config', return_value=accesslog_config):
        with mock.patch.object(module_1.checker, 'to_config', return_value=checker_config):
            with mock.patch.object(module_2, 'to_config', return_value=regexp_config):
                chain_config = holder.to_config()

    accesslog_config = chain_config.table['accesslog']
    assert set(accesslog_config.table.keys()) == {'log', 'antirobot'}
    antirobot_config = accesslog_config.table['antirobot']
    check_antirobot_config(antirobot_config)
