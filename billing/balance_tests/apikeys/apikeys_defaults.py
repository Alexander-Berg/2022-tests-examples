# coding: utf-8
from datetime import datetime
from collections import namedtuple

APIKEYS_SERVICE_ID = 129
ADMIN = 1120000000078419

BASE_DT = datetime.utcnow().replace(hour=5)

POSTPAYMENT_CONSUMER_UNITS = ['MonthlyStatisticRangePerDayConsumerUnit',
                              'PostpaySubscribePeriodicallyRangeConsumerUnit',
                              'DailyStatisticRangeConsumerUnit',
                              'WithFirstRangeScaledMonthlyStatisticRangePerDayConsumerUnit'
                              ]
PREPAYMENT_COUNSUMER_UNITS = ['TodayPrepayStatisticRangeConsumerUnit']

WAITER_PARAMS = namedtuple('Wait', ['time', 's_time'])(time=30, s_time=1)

APIKEYS_LOGIN_POOL = [
    # (641824523, 'apikeys-autotest-0'), (641824532, 'apikeys-autotest-1'), (641824631, 'apikeys-autotest-2'),
    # (641824736, 'apikeys-autotest-3'), (641824870, 'apikeys-autotest-4'), (641825000, 'apikeys-autotest-5'),
    # (641825108, 'apikeys-autotest-6'), (641825118, 'apikeys-autotest-7'), (641825125, 'apikeys-autotest-8'),
    # (641825139, 'apikeys-autotest-9'), (641824538, 'apikeys-autotest-10'), (641824547, 'apikeys-autotest-11'),
    # (641824555, 'apikeys-autotest-12'), (641824562, 'apikeys-autotest-13'), (641824571, 'apikeys-autotest-14'),
    # (641824577, 'apikeys-autotest-15'), (641824587, 'apikeys-autotest-16'), (641824596, 'apikeys-autotest-17'),
    # (641824602, 'apikeys-autotest-18'), (641824621, 'apikeys-autotest-19'), (641824639, 'apikeys-autotest-20'),
    # (641824651, 'apikeys-autotest-21'), (641824656, 'apikeys-autotest-22'), (641824671, 'apikeys-autotest-23'),
    # (641824679, 'apikeys-autotest-24'), (641824688, 'apikeys-autotest-25'), (641824696, 'apikeys-autotest-26'),
    # (641824709, 'apikeys-autotest-27'), (641824719, 'apikeys-autotest-28'), (641824727, 'apikeys-autotest-29'),
    # (641824742, 'apikeys-autotest-30'), (641824751, 'apikeys-autotest-31'), (641824757, 'apikeys-autotest-32'),
    # (641824770, 'apikeys-autotest-33'), (641824779, 'apikeys-autotest-34'), (641824819, 'apikeys-autotest-35'),
    # (641824832, 'apikeys-autotest-36'), (641824841, 'apikeys-autotest-37'), (641824851, 'apikeys-autotest-38'),
    # (641824860, 'apikeys-autotest-39'), (641824883, 'apikeys-autotest-40'), (641824889, 'apikeys-autotest-41'),
    # (641824895, 'apikeys-autotest-42'), (641824903, 'apikeys-autotest-43'), (641824909, 'apikeys-autotest-44'),
    # (641824922, 'apikeys-autotest-45'), (641824934, 'apikeys-autotest-46'), (641824950, 'apikeys-autotest-47'),
    # (641824958, 'apikeys-autotest-48'), (641824967, 'apikeys-autotest-49'), (641825011, 'apikeys-autotest-50'),
    # (641825016, 'apikeys-autotest-51'), (641825026, 'apikeys-autotest-52'), (641825036, 'apikeys-autotest-53'),
    # (641825046, 'apikeys-autotest-54'), (641825058, 'apikeys-autotest-55'), (641825064, 'apikeys-autotest-56'),
    # (641825070, 'apikeys-autotest-57'), (641825086, 'apikeys-autotest-58'), (641825100, 'apikeys-autotest-59'),

    # Технический пользователь
    (0, '0'),
    # Костя
     (313834749, 'torvald-test-1'),
    # (313834798, 'torvald-test-2'),
    # (313834839, 'torvald-test-3'),
    # (313834876, 'torvald-test-4'),
    # (313834913, 'torvald-test-5'),
    # Руслан
    (313834974, 'torvald-test-6'),
    (313835034, 'torvald-test-7'),
    (313835107, 'torvald-test-8'),
    (313835152, 'torvald-test-9'),
    (313835197, 'torvald-test-10'),
    # Глеб
    # (313835251, 'torvald-test-11'),
    # (313835304, 'torvald-test-12'),
    # (313835348, 'torvald-test-13'),
    # (313835400, 'torvald-test-14'),
    # (313835460, 'torvald-test-15'),
    # Запас на разные случаи
    # (313835502, 'torvald-test-16'),
    # (313835583, 'torvald-test-17'),
    # (313835678, 'torvald-test-18'),
    # (313835881, 'torvald-test-19')
]

APIKEYS_LOGIN_POOL_ERROR_TEST = [
    (642561093, 'test-api-errors'),
    (642561525, 'test-api-errors-1')
]

tokens = {u'deliverywidget': u'deliverywidget_1823d64f8d7dc650a01383cfcd1ce37ebfc4b698', u'text_rec': u'text_rec_f4850ee20d08951bae5e1230e02460cb', u'aviatickets': u'aviatickets_4b9a0a7f9a7645e7056a2fd5400a040', u'testspeechkitcloud': u'testspeechkitcloud_7147e6299273078698140f4c098e4eedd89ad5d0', u'routingmatrix': u'routingmatrix_a33cd4b55ea503baa144b05afd7059284630fdf6', u'apimaps': u'apimaps_58dc271a69a6ff92c1ca38c35db195a9', u'userapikeys': u'userapikeys_5d7a5320cf0d7f2b077e874c57c4e39198c8e5ff', u'locator': u'locator_09462db8b93dcae3c9b28e1fd35df8fc7b332f97', u'trends': u'trends_234b9cebd47969ea53f63f477343ad5a9247a10c', u'connect': u'connect_bd12b243ec7b38d23dc7a5c2f0c33083f0ea133d', u'partner': u'partner_17305dc4cfaa1120286ec3c829252c94ea2a3296', u'market': u'market_f5de92175940968a63a4e8abaa056c8b6c4ebbc5', u'testcity': u'testcity_75191bb8e50d85505b419dd0112bf6c0cdaaa594', u'raspmobile': u'raspmobile_d7f40b596cb34f6b9096dd8ceb567a99adc8b45a', u'testspeechkitmobile': u'testspeechkitmobile_cad5058d5cf684e2ab5442a77d2a52871aa825ef', u'testmarket': u'testmarket_f5de92175940968a63a4e8abaa056c8b6c4ebbc5', u'testpogoda': u'testpogoda_bdceed839af6b657a1e4684a302d83a69c0584d1', u'speechkitmobile': u'speechkitmobile_cad5058d5cf684e2ab5442a77d2a52871aa825ef', u'rasp': u'rasp_7ac1cd95e8d94cbef9e5762c10e1347a', u'realty': u'realty_e4f24b1d64119828ad584aa24eeec3ce2f68d726', u'businesschatsprod': u'businesschatsprod_049122652b69dbf54cf27c17ec5ecfcbf3f6bff2', u'ordersdistribution': u'ordersdistribution_079ad00881ce745d60be4a630e20c84b82221a38', u'testmapkit': u'testmapkit_456287b45f7f0cdd2d5bcbd1805291d4fe8c29e8', u'ydfiduplic': u'ydfiduplic_85d81eee6fe91e12e0725b4bab44769897db8260', u'safebrowsing': u'safebrowsing_1362b551bd373eaf3237b880014f1760bed0ebc8', u'courier': u'courier_f07332e1df3d27965cc228b2641994f9a810c070', u'city': u'city_75191bb8e50d85505b419dd0112bf6c0cdaaa594', u'auto': u'auto_d5f605989f9d1299b1661753b22b15ce3b7edb56', u'yapayment': u'yapayment_fa75cbc617ea2bc5fafb57cdff2c2f99088efa6b', u'speechkitjsapi': u'speechkitjsapi_f7a2fe0af6e463003bc2070c215d038140c6f0ec', u'microtest': u'microtest_a1e17c676e85cf5376767a6f1211adb7', u'atom': u'atom_302dd38967912e1bade50018b54a18e5', u'staticmaps': u'staticmaps_65e431be12da959daaf4f2225900e27757c01e48', u'testapimaps': u'testapimaps_58dc271a69a6ff92c1ca38c35db195a9', u'sitesearch': u'sitesearch_182b84c9644ce246b80a503b6e1eef5c0598f071', u'adfox': u'adfox_bd12b243ec7b38d23dc7a5c2f0c33083f0ea133d', u'ydfimoder': u'ydfimoder_6c5353a0a33e128a13ee5cd9a394f966260c43d8', u'balance': u'balance_410ebf9ae9a35d40be3d6a6d586fa8c791453e94', u'ofd': u'ofd_a0740f4d743c2c8480404a429285c082e6525415', u'speechkit': u'speechkit_f364a450d75df52c15e7e783b161c0c8', u'speechkitcloud': u'speechkitcloud_7147e6299273078698140f4c098e4eedd89ad5d0', u'rabota': u'rabota_fd113dc8f4235f44f0f91fb23dea4b9c9576a0b1', u'apimapsplus': u'apimapsplus_b85e70c74ca130fdc09ddce2516e516119b39e5d', u'drive': u'drive_c5ee68839cdb188c48bcc78423caac3c4c1036a9', u'pogoda': u'pogoda_bdceed839af6b657a1e4684a302d83a69c0584d1', u'testv6': u'testv6_666', u'mapkit': u'mapkit_456287b45f7f0cdd2d5bcbd1805291d4fe8c29e8', u'compvision': u'compvision_f1905ffb44c82e17e0a465212bb74913db160230', u'test_service': u'test_service_c8b0fd77ba77fb905e76bc7630c35e6259c87a9d', u'businesschats': u'businesschats_a1821970246ac5afc59d2d377da13b15a42fb15c'}
