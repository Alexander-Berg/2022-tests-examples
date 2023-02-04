# coding: utf-8

import datetime

import pytest
from hamcrest import equal_to

from btestlib.matchers import contains_dicts_with_entries
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils
from balance import balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils
from btestlib.constants import Services
from temp.igogor.balance_objects import Contexts, Products, Firms, Paysyses, PersonTypes, Currencies, Regions

dt = datetime.datetime.now()

DIRECT_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1)

DIRECT_YANDEX_FIRM_FISH_NON_RESIDENT = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1,
                                                                            person_type=PersonTypes.YT,
                                                                            paysys=Paysyses.BANK_YT_RUB)

MARKET_YANDEX_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                                               firm=Firms.YANDEX_1)
MARKET_MARKET_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.MARKET, product=Products.MARKET,
                                                               firm=Firms.MARKET_111)
DIRECT_BEL_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_FISH,
                                                            firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                            paysys=Paysyses.BANK_BY_UR_BYN)
DIRECT_KZ_FIRM_FISH = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_FISH,
                                                           firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                           paysys=Paysyses.BANK_KZ_UR_TG)

DIRECT_YANDEX_FIRM_RUB = Contexts.DIRECT_FISH_RUB_CONTEXT.new(firm=Firms.YANDEX_1, product=Products.DIRECT_RUB,
                                                              region=Regions.RU, currency=Currencies.RUB)

DIRECT_BEL_FIRM_BYN = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_BYN,
                                                           firm=Firms.REKLAMA_BEL_27, person_type=PersonTypes.BYU,
                                                           paysys=Paysyses.BANK_BY_UR_BYN, region=Regions.BY,
                                                           currency=Currencies.BYN)

DIRECT_KZ_FIRM_KZU = Contexts.DIRECT_FISH_RUB_CONTEXT.new(service=Services.DIRECT, product=Products.DIRECT_KZT_QUASI,
                                                          firm=Firms.KZ_25, person_type=PersonTypes.KZU,
                                                          paysys=Paysyses.BANK_KZ_UR_TG, region=Regions.KZ,
                                                          currency=Currencies.KZT)

ENOUGH_LIMIT_VALUE = 100

product_list = [2132
    , 970
    , 1347
    , 2453
    , 10000021
    , 1280
    , 2284
    , 2283
    , 2285
    , 1392
    , 1313
    , 1447
    , 538
    , 883
    , 1029
    , 870
    , 2030
    , 567
    , 1011
    , 920
    , 357
    , 1773
    , 1826
    , 1703
    , 1448
    , 338
    , 1583
    , 1297
    , 805
    , 852
    , 2036
    , 627
    , 728
    , 502
    , 1582
    , 694
    , 1452
    , 2533
    , 2523
    , 814
    , 1053
    , 1644
    , 2137
    , 711
    , 2524
    , 2526
    , 720
    , 703
    , 551
    , 2520
    , 2521
    , 2528
    , 2532
    , 1455
    , 1295
    , 433
    , 691
    , 658
    , 757
    , 1638
    , 1296
    , 1542
    , 1043
    , 992
    , 989
    , 472
    , 1800
    , 1827
    , 1521
    , 873
    , 530
    , 233
    , 787
    , 884
    , 682
    , 1007
    , 957
    , 830
    , 857
    , 1677
    , 1406
    , 1676
    , 944
    , 2268
    , 2269
    , 1349
    , 2050
    , 1851
    , 2008
    , 2015
    , 2029
    , 2047
    , 995
    , 1181
    , 499
    , 1093
    , 1231
    , 293
    , 251
    , 1096
    , 1059
    , 491
    , 341
    , 604
    , 557
    , 144
    , 490
    , 1318
    , 1832
    , 575
    , 1854
    , 1018
    , 1245
    , 1400
    , 2142
    , 1260
    , 1652
    , 1130
    , 784
    , 1901
    , 1945
    , 2049
    , 796
    , 1937
    , 1849
    , 1852
    , 1027
    , 797
    , 1398
    , 1520
    , 1539
    , 1379
    , 1801
    , 1576
    , 1823
    , 960
    , 1654
    , 1824
    , 895
    , 1031
    , 134
    , 1391
    , 465
    , 466
    , 1058
    , 1253
    , 469
    , 635
    , 737
    , 340
    , 1933
    , 1806
    , 2074
    , 2044
    , 1705
    , 2063
    , 1374
    , 1372
    , 265
    , 587
    , 269
    , 1001
    , 510
    , 1197
    , 880
    , 947
    , 578
    , 936
    , 1198
    , 1538
    , 1413
    , 568
    , 988
    , 1247
    , 876
    , 1298
    , 779
    , 753
    , 1113
    , 323
    , 713
    , 543
    , 350
    , 140
    , 792
    , 436
    , 1592
    , 1511
    , 556
    , 106
    , 321
    , 762
    , 815
    , 1248
    , 892
    , 863
    , 539
    , 276
    , 1176
    , 775
    , 1030
    , 991
    , 319
    , 709
    , 246
    , 1120
    , 913
    , 659
    , 671
    , 654
    , 710
    , 566
    , 1825
    , 1706
    , 1783
    , 1725
    , 1647
    , 1837
    , 2071
    , 927
    , 954
    , 1373
    , 1377
    , 1005
    , 1550
    , 2450
    , 2451
    , 724
    , 909
    , 2446
    , 2452
    , 2454
    , 2455
    , 641
    , 1012
    , 2443
    , 2442
    , 2445
    , 2447
    , 2448
    , 2449
    , 2007
    , 1758
    , 2135
    , 1724
    , 2246
    , 1478
    , 2115
    , 2079
    , 2028
    , 1711
    , 2131
    , 1821
    , 2105
    , 266
    , 561
    , 242
    , 473
    , 861
    , 335
    , 1088
    , 388
    , 638
    , 460
    , 355
    , 2221
    , 2573
    , 2575
    , 2576
    , 2574
    , 2581
    , 2582
    , 2595
    , 833
    , 2432
    , 2434
    , 2436
    , 2437
    , 2435
    , 2438
    , 2193
    , 739
    , 918
    , 1903
    , 1659
    , 1895
    , 1729
    , 2026
    , 345
    , 248
    , 1117
    , 2594
    , 2599
    , 2600
    , 875
    , 653
    , 816
    , 938
    , 939
    , 878
    , 1158
    , 1164
    , 817
    , 1414
    , 494
    , 840
    , 1707
    , 1185
    , 2108
    , 1915
    , 1766
    , 997
    , 1137
    , 1739
    , 663
    , 1648
    , 777
    , 520
    , 928
    , 243
    , 2183
    , 2184
    , 1653
    , 766
    , 337
    , 126
    , 849
    , 759
    , 941
    , 2457
    , 1123
    , 929
    , 919
    , 839
    , 1243
    , 678
    , 1099
    , 306
    , 1286
    , 1378
    , 344
    , 1584
    , 288
    , 1597
    , 2387
    , 537
    , 1409
    , 1040
    , 1051
    , 1008
    , 1512
    , 505
    , 1098
    , 1513
    , 1728
    , 2101
    , 1865
    , 1368
    , 1342
    , 1211
    , 874
    , 1222
    , 708
    , 515
    , 312
    , 820
    , 1417
    , 2100
    , 1282
    , 1284
    , 651
    , 281
    , 1101
    , 893
    , 958
    , 1700
    , 1210
    , 2181
    , 1294
    , 1508
    , 2247
    , 1399
    , 585
    , 1065
    , 2162
    , 1249
    , 2134
    , 885
    , 275
    , 21
    , 137
    , 2401
    , 2402
    , 2583
    , 2584
    , 1208
    , 1898
    , 1554
    , 993
    , 1396
    , 700
    , 1010
    , 1474
    , 1277
    , 937
    , 702
    , 2078
    , 2052
    , 1394
    , 656
    , 819
    , 1430
    , 640
    , 813
    , 1025
    , 1950
    , 1216
    , 1403
    , 1154
    , 1755
    , 2082
    , 1736
    , 1708
    , 1737
    , 2072
    , 1071
    , 1348
    , 1290
    , 1713
    , 795
    , 1278
    , 1069
    , 396
    , 1701
    , 670
    , 1577
    , 1153
    , 680
    , 2053
    , 2379
    , 2380
    , 2381
    , 1281
    , 2382
    , 1646
    , 2377
    , 879
    , 1050
    , 804
    , 224
    , 1160
    , 1817
    , 1060
    , 1346
    , 1390
    , 1775
    , 1338
    , 1145
    , 1671
    , 1150
    , 1151
    , 1492
    , 500
    , 968
    , 1129
    , 1244
    , 1252
    , 2070
    , 1567
    , 1109
    , 1003
    , 554
    , 1057
    , 1006
    , 802
    , 417
    , 908
    , 1187
    , 1125
    , 1268
    , 2110
    , 1385
    , 959
    , 1421
    , 1458
    , 1026
    , 1880
    , 278
    , 1450
    , 1820
    , 1740
    , 1756
    , 1839
    , 1702
    , 1957
    , 1293
    , 1366
    , 2109
    , 2094
    , 1516
                ]


@pytest.mark.parametrize('params', [
    ({'overdraft_given_to': DIRECT_YANDEX_FIRM_FISH, 'overdraft_taken_by': DIRECT_YANDEX_FIRM_FISH}
     # {'overdraft_given_to': DIRECT_BEL_FIRM_FISH, 'overdraft_taken_by': DIRECT_BEL_FIRM_FISH},
     )])
@pytest.mark.parametrize('overdraft_value', [
    'enough_limit',
    # 'non_enough',
    # 'null'
])
@pytest.mark.parametrize('product', product_list)
def test_overdraft_usage_medium_exp(params, overdraft_value, product):
    # for product in product_list:
    client_id = steps.ClientSteps.create()
    overdraft_given_to_context = params['overdraft_given_to']
    if overdraft_value != 'null':
        if overdraft_value == 'enough_limit':
            limit = 100
        else:
            limit = 50

        steps.OverdraftSteps.set_force_overdraft(client_id, overdraft_given_to_context.service.id, limit,
                                                 overdraft_given_to_context.firm.id)
        actual_limit = db.balance().get_overdraft_limit_by_firm(client_id, overdraft_given_to_context.service.id,
                                                                overdraft_given_to_context.firm.id)
        assert limit == actual_limit

        overdraft_taken_by_context = params['overdraft_taken_by']
        person_id = steps.PersonSteps.create(client_id, overdraft_taken_by_context.person_type.code)
        service_order_id = steps.OrderSteps.next_id(overdraft_taken_by_context.service.id)
        steps.OrderSteps.create(client_id, service_order_id, service_id=overdraft_taken_by_context.service.id,
                                product_id=product)

        orders_list = [
            {'ServiceID': overdraft_taken_by_context.service.id, 'ServiceOrderID': service_order_id, 'Qty': 100,
             'BeginDT': dt}]
        request_id = steps.RequestSteps.create(client_id, orders_list,
                                               additional_params={'InvoiceDesireDT': dt})
        try:
            invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, overdraft_taken_by_context.paysys.id,
                                                         credit=0, overdraft=1, contract_id=None)

            assert overdraft_value == 'enough_limit' and overdraft_given_to_context == overdraft_taken_by_context
        except Exception, exc:
            utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to(u'NOT_ENOUGH_OVERDRAFT_LIMIT'))
            assert overdraft_value != 'enough_limit' or overdraft_given_to_context != overdraft_taken_by_context
