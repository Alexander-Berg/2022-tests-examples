#! /usr/bin/env python2

from contextlib import contextmanager
import mock
import unittest

import configobj
import validate

from hbfagent import config
from hbfagent.util import join_lines


@contextmanager
def patch_opens(*args):
    with mock.patch("os.path.isfile") as isfile, mock.patch("__builtin__.open", mock.mock_open(), create=True) as open:
        isfile.return_value = True
        open.return_value.readlines.side_effect = args
        yield


class TestConfig(unittest.TestCase):
    config_lines = [
        "[main]",
        "a = dva",
        "x = 2"
    ]

    config_lines_errors = [
        "[main]",
        "a = tri",
        "x = tri",
        "z = tri"
    ]

    config_lines_with_comments = [
        "# comment",
        "[main]  # comment",
        "a = dva  # comment",
        "# comment",
        "x = 2"
    ]

    configspec_lines = [
        "[main]",
        "a = string(default=odin)",
        "x = integer(default=1)"
    ]

    config_default = join_lines(
        "[main]",
        "a = odin",
        "x = 1"
    )

    def setUp(self):
        with patch_opens(self.configspec_lines, self.config_lines):
            self.config = config.Config("test.conf", "test.configspec")

    def tearDown(self):
        self.config.delete()

    def test_init(self):
        self.assertIsInstance(self.config.obj, configobj.ConfigObj)

    def test_load_config(self):
        with patch_opens(self.configspec_lines, self.config_lines):
            obj, errors = self.config._load_config()
            self.assertIsInstance(obj, configobj.ConfigObj)
            self.assertEqual(errors, [])

    def test_validation_errors(self):
        with patch_opens(self.config_lines, self.configspec_lines):
            obj = configobj.ConfigObj("test.conf",
                                      configspec="test.configspec")
            result = obj.validate(validate.Validator(), preserve_errors=True)
            errors = config.Config._validation_errors(obj, result)
            self.assertEqual(errors, [])

    def test_validation_errors_with_errors(self):
        with patch_opens(self.config_lines_errors, self.configspec_lines):
            obj = configobj.ConfigObj("test.conf",
                                      configspec="test.configspec")
            result = obj.validate(validate.Validator(), preserve_errors=True)
            errors = config.Config._validation_errors(obj, result)
            self.assertEqual(len(errors), 1)

    def test_extra_values(self):
        with patch_opens(self.config_lines, self.configspec_lines):
            obj = configobj.ConfigObj("test.conf",
                                      configspec="test.configspec")
            obj.validate(validate.Validator(), preserve_errors=True)
            errors = config.Config._extra_values(obj)
            self.assertEqual(errors, [])

    def test_extra_values_with_errors(self):
        with patch_opens(self.config_lines_errors, self.configspec_lines):
            obj = configobj.ConfigObj("test.conf",
                                      configspec="test.configspec")
            obj.validate(validate.Validator(), preserve_errors=True)
            errors = config.Config._extra_values(obj)
            self.assertEqual(len(errors), 1)

    def test_reload(self):
        with patch_opens(self.configspec_lines, self.config_lines):
            cur_obj_id = id(self.config.obj)
            errors = self.config.reload()
            new_obj_id = id(self.config.obj)
            self.assertEqual(errors, [])
            self.assertNotEqual(cur_obj_id, new_obj_id)

    def test_reload_with_errors(self):
        with patch_opens(self.config_lines_errors, self.configspec_lines):
            cur_obj_id = id(self.config.obj)
            errors = self.config.reload()
            new_obj_id = id(self.config.obj)
            self.assertNotEqual(errors, [])
            self.assertEqual(cur_obj_id, new_obj_id)

    def test_remove_comments(self):
        with patch_opens(self.config_lines_with_comments):
            obj = configobj.ConfigObj("test.conf")
            config.Config.remove_comments(obj)
            obj.filename = None
            config_str = "\n".join(obj.write()) + "\n"
            self.assertEqual(join_lines(*self.config_lines), config_str)

    def test_dump_default(self):
        with patch_opens(self.configspec_lines), mock.patch("sys.stdout") as stdout:
            errors = config.Config.dump_default("test.configspec")
            self.assertEqual(errors, [])
            stdout.write.assert_called_once_with(self.config_default)

    def test_str(self):
        self.assertEqual(str(self.config), join_lines(*self.config_lines))

    def test_getitem(self):
        self.assertEqual(self.config["main"]["a"], "dva")
        self.assertEqual(self.config["main"]["x"], 2)

    def test_overrides_ok(self):
        args = ['http_api.port=6667', 'log_level=DEBUG', 'server_options=output, debug']
        ov = config.overrides_from_args(args)
        self.assertEqual(ov, {
            ('http_api', 'port'): '6667',
            ('main', 'log_level'): 'DEBUG',
            ('main', 'server_options'): 'output, debug',
        })

    def test_overrides_error(self):
        f = config.overrides_from_args

        def no_value():
            f(['http_api.port'])

        def no_section():
            f(['.port=1111'])

        def no_key():
            f(['http_api.=1111'])

        for err in [no_value, no_section, no_key]:
            self.assertRaises(config.ConfigError, err)

    def test_overrides_empty_default_list(self):
        self.config.delete()
        config_ = []
        spec = [
            '[main]',
            'server_ips = string(default="2a02:6b8:0:3400:0:5c3:0:3, 93.158.157.206")',
        ]
        overrides = {}
        c = config.Config(config=config_, configspec=spec, overrides=overrides)
        self.assertEqual(c['main']['server_ips'], "2a02:6b8:0:3400:0:5c3:0:3, 93.158.157.206")

    def test_overrides_non_empty_default_list(self):
        self.config.delete()
        config_ = [
            '[main]',
            'server_ips = some, ips',
        ]
        spec = [
            '[main]',
            'server_ips = string(default="2a02:6b8:0:3400:0:5c3:0:3, 93.158.157.206")',
            '[lxd_ips]',
            'timeout = string(default="3, 4")',
        ]
        overrides = {
            ('main', 'server_ips'): '1, 2',
        }
        c = config.Config(config=config_, configspec=spec, overrides=overrides)
        self.assertEqual(c['main']['server_ips'], '1, 2')
        self.assertEqual(c['lxd_ips']['timeout'], '3, 4')
        self.assertIsInstance(c['lxd_ips'], dict)


if __name__ == "__main__":
    unittest.main()
