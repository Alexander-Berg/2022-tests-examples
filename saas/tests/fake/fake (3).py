# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import string

from faker.providers import BaseProvider
from saas.library.python.common_functions import DC_LOCATIONS, GEO_LOCATIONS


class Provider(BaseProvider):
    def random_string(self, length):
        choices = self.random_choices(string.ascii_lowercase + string.digits, length)
        return ''.join(choices)

    def random_hexadecimal_string(self, length):
        return '{1:0>{0}x}'.format(length, self.generator.random.getrandbits(length * 4))

    def random_dc(self):
        return self.random_element(DC_LOCATIONS)

    def random_geo(self):
        return self.random_element(GEO_LOCATIONS)

    def get_shard(self, shards_cnt, idx=None):
        max_ = 65533
        range_ = int(max_ / shards_cnt)

        if idx is not None:
            s = range_ * idx
            e = s + range_ if idx != shards_cnt - 1 else max_
            return s, e

        shards = []
        for i in range(shards_cnt):
            s = range_ * i
            e = s + range_
            shard = s, e
            shards.append(shard)

        shards[-1] = shards[0], max_
        return self.random_choices(shards)
