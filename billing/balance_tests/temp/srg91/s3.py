# -*- coding: utf-8 -*-

import urllib
from btestlib import shared
# 86400


url = shared.push_raw_data_to_s3_and_get_url('srg91.jpg', urllib.urlopen('https://center.yandex-team.ru/api/v1/user/srg91/photo_21389/300.jpg').read())
print(url)
# print(shared.get_data_from_s3('srg91_test_item'))
# bucket = shared.s3storage_shared().bucket
# pass
# vim:ts=4:sts=4:sw=4:tw=79:et:
