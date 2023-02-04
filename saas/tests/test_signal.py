import unittest

from faker import Faker

from saas.library.python.yasm import YasmSignal
from saas.library.python.yasm import YasmSignalIterator


class TestYasmSignal(unittest.TestCase):
    fake = Faker()

    def test_str(self):
        self.assertEqual(str(YasmSignal('my_signal_hgram', itype='common')), 'itype=common:my_signal_hgram')
        self.assertEqual(str(YasmSignal('my_signal_hgram', itype='common', prj='my_prj')), 'itype=common;prj=my_prj:my_signal_hgram')
        self.assertEqual(str(YasmSignal('my_signal_hgram', itype='common', ctype='prod', prj='my_prj')), 'ctype=prod;itype=common;prj=my_prj:my_signal_hgram')

    def test_repr(self):
        self.assertEqual(YasmSignal('my_signal_hgram', itype='common').__repr__(), 'YasmSignal("my_signal_hgram", "common", **{})')
        self.assertEqual(
            YasmSignal('my_signal_hgram', itype='common', ctype='prod', prj='my_prj').__repr__(),
            'YasmSignal("my_signal_hgram", "common", ctype="prod", prj="my_prj")'
        )

    def test_eq(self):
        signal_names = self.fake.words(nb=2, unique=True)
        signal_itypes = self.fake.words(nb=2, unique=True)
        tags_0 = self.fake.pydict()
        tags_1 = self.fake.pydict()

        self.assertTrue(YasmSignal(signal_names[0], signal_itypes[0], **tags_0) == YasmSignal(signal_names[0], signal_itypes[0], **tags_0))
        self.assertTrue(YasmSignal(signal_names[1], signal_itypes[1], **tags_1) == YasmSignal(signal_names[1], signal_itypes[1], **tags_1))

        self.assertFalse(YasmSignal(signal_names[0], signal_itypes[0], **tags_0) == YasmSignal(signal_names[0], signal_itypes[0], **tags_1))
        self.assertFalse(YasmSignal(signal_names[0], signal_itypes[0], **tags_0) == YasmSignal(signal_names[0], signal_itypes[1], **tags_0))
        self.assertFalse(YasmSignal(signal_names[0], signal_itypes[0], **tags_0) == YasmSignal(signal_names[1], signal_itypes[0], **tags_0))

        self.assertFalse(YasmSignal(signal_names[1], signal_itypes[1], **tags_1) == YasmSignal(signal_names[1], signal_itypes[1], **tags_0))
        self.assertFalse(YasmSignal(signal_names[1], signal_itypes[1], **tags_1) == YasmSignal(signal_names[1], signal_itypes[0], **tags_1))
        self.assertFalse(YasmSignal(signal_names[1], signal_itypes[1], **tags_1) == YasmSignal(signal_names[0], signal_itypes[1], **tags_1))

        self.assertFalse(YasmSignal(signal_names[1], signal_itypes[1], **tags_1) == YasmSignal(signal_names[0], signal_itypes[0], **tags_0))

    def test_order(self):
        self.assertGreater(YasmSignal('bbb', itype='common'), YasmSignal('aaa', itype='common'))
        self.assertGreater(YasmSignal('aaa', itype='ccc'), YasmSignal('aaa', itype='bbb'))
        self.assertGreater(YasmSignal('aaa', itype='aaa', d='f'), YasmSignal('aaa', itype='aaa', d='e'))


class TestYasmSignalIterator(unittest.TestCase):
    @staticmethod
    def simple_generator(*args, **kwargs):
        for ts, val in [(1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6)]:
            yield ts, val

    def test_mean(self):
        true_iterator = [el[1] for el in self.simple_generator()]
        iterator = YasmSignalIterator(self.simple_generator)
        self.assertEqual(iterator.mean(), float(sum(list(true_iterator)))/len(list(true_iterator)))

    def test_max(self):
        true_iterator = [el[1] for el in self.simple_generator()]
        iterator = YasmSignalIterator(self.simple_generator)
        self.assertEqual(iterator.max(), max(list(true_iterator)))

    def test_call(self):
        true_iterator = self.simple_generator()
        iterator = YasmSignalIterator(self.simple_generator)
        self.assertEqual(list(iterator()), list(true_iterator))
