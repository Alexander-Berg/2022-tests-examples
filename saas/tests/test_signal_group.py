import unittest

from faker import Faker

from saas.library.python.yasm import YasmSignalGroup, YasmSignal


class TestYasmSignalGroup(unittest.TestCase):
    fake = Faker()

    def test_str(self):
        self.assertEqual(
            str(YasmSignalGroup(
                YasmSignal('my_signal_hgram', itype='common'),
                YasmSignal('my_signal_hgram', itype='common', ctype='prod', prj='my_prj')
            )),
            'YasmSignalGroup([YasmSignal("my_signal_hgram", "common", ctype="prod", prj="my_prj"),YasmSignal("my_signal_hgram", "common", **{})])'
        )
