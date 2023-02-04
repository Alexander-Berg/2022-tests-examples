from lacmus2.toiler.selector import SelectorVtypeProcessor

from ..redis_test import BaseRedisTestCase


class SelectorPostprocessTestCase(BaseRedisTestCase):
    def test(self):
        proc = SelectorVtypeProcessor(self.storage)
        proc.postprocess_value(
            {'vtype': 'svtype1', 'key': 'skey1'}, ['h1', 'h5'],
        )
        self.assertEquals(
            sorted(self.redis.sscan_iter('s2hh\0svtype1\0skey1')),
            ['h1', 'h5']
        )

        proc.postprocess_value({'vtype': 'svtype1', 'key': 'skey1'}, [])
        self.assertEquals(
            sorted(self.redis.sscan_iter('s2hh\0svtype1\0skey1')), []
        )
