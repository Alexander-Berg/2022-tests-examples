# -*- coding: utf-8 -*-

from tests.base import BalanceTest

from balance.completions_fetcher.partner_entity_completions import (
    CompletionBase, CompUniversal, KEY_TEMPLATE, VAL_TEMPLATE)

from balance.mapper.tarification_entity import EntityCompletions

from datetime import datetime
from StringIO import StringIO
from decimal import Decimal as D

from pprint import pprint


CODES = [
    'addappter2_retail', 'advertisement', 'api_market',
    'avia_rs', 'cloud_mp', 'd_installs', 'direct_rs', 'downloads', 'ingame_purchases',
    'partner_tag_products', 'rs_market', 'rs_market_cpa', 'rtb_distr', 'serphits', 'taxi_distr',
    'video_distr',
]

dt = datetime(2019, 1, 20)
MILLION = 10**6


fake_source = {}
fake_source['activations'] = (
"""{"data":["""
"""{"dt": "2019-01-20", "page_id": 158324, "clid": "2257150", "currency_id": 2, "vid": "10", "activations": "472"}, """
"""{"dt": "2019-01-20", "page_id": 158324, "clid": "2257150", "currency_id": 2, "vid": "11", "activations": "55"}, """
"""{"dt": "2019-01-20", "page_id": 158324, "clid": "2257150", "currency_id": 2, "vid": "15", "activations": "20"}, """
"""{"dt": "2019-01-20", "page_id": 158324, "clid": "2257150", "currency_id": 2, "vid": "3", "activations": "8"}, """
"""{"dt": "2019-01-20", "page_id": 158324, "clid": "2257150", "currency_id": 2, "vid": "5", "activations": "1"}, """
"""{"dt": "2019-01-20", "page_id": 158324, "clid": "2257150", "currency_id": 2, "vid": "8", "activations": "3"}, """
"""{"dt": "2019-01-20", "page_id": 158324, "clid": "2257150", "currency_id": 2, "vid": "9", "activations": "29"}, """
"""{"dt": "2019-01-20", "page_id": 158340, "clid": "2257166", "currency_id": 2, "vid": "-1", "activations": "19"}, """
"""{"dt": "2019-01-20", "page_id": 158484, "clid": "2257310", "currency_id": 2, "vid": "-1", "activations": "3"}, """
"""{"dt": "2019-01-20", "page_id": 158516, "clid": "2257342", "currency_id": 2, "vid": "-1", "activations": "451"}"""
"""],"result":"ok"}"""
)
fake_source['addapter_dev_com'] = (
"""2019-01-20;2106502;2106550;534.00;654.00;6
2019-01-20;777888999;127774;6.00;4.00;2
2019-01-20;87452;123;54.21;56.50;1
2019-01-20;2276700;123;32.04;43.55;1
2019-01-20;2075745;123;45.00;65.90;1
2019-01-20;25421;123;21.21;33.33;1
2019-01-20;8754;123;24.51;25.41;1
2019-01-20;87565442;123;22.22;33.33;1
2019-01-20;521;123;22.22;33.33;1
2019-01-20;4874;123;54.21;66.66;1"""
)
fake_source['addappter2_retail'] = (
"""2016-07-20;2155515;324
2016-07-20;2155516;194
2016-07-27;2155515;467
2016-07-27;2155516;295"""
)
fake_source['addapter_ret_ds'] = fake_source['addapter_dev_com']
fake_source['addapter_ret_com'] = fake_source['addapter_dev_com']
fake_source['addapter_dev_ds'] = fake_source['addapter_dev_com']
fake_source['api_market'] = (
"""139092;;2019-01-20;577461;0;0;0;0
151198;;2019-01-20;67293;66;5.45;545;205
2294597;;2019-01-20;15;0;0;0;2
141487;;2019-01-20;873;1;1.52;152;5
2289306;;2019-01-20;5779;0;0;0;195
152593;;2019-01-20;182;0;0;0;2
152543;;2019-01-20;2135554;36;12.21;1221;185
219512;;2019-01-20;5233;5;1.3;130;280
170108;;2019-01-20;4520291;180;52.09;5209;840
2291287;;2019-01-20;120915;0;0;0;0"""
)
fake_source['avia_rs'] = (
"""clicks=17	shows=17	national_version=com	bucks=63.864835	client_id=34879676
clicks=7	shows=7	clid=j:null	national_version=kz	bucks=10.005718	client_id=13361436
clicks=1	shows=1	clid=2328169-637	national_version=kz	bucks=1.429388	client_id=1612907
clicks=1	shows=1	clid=2323977-673	national_version=kz	bucks=1.429388	client_id=13361436
clicks=1	shows=1	clid=2323977-671	national_version=kz	bucks=1.429388	client_id=13361436
clicks=4	shows=4	clid=2279278-100	national_version=kz	bucks=5.717553	client_id=8215909
clicks=1	shows=1	clid=2262097-252	national_version=kz	bucks=1.429388	client_id=6056368
clicks=1	shows=1	clid=2256434-306	national_version=kz	bucks=1.429388	client_id=13361436
clicks=3	shows=3	clid=2256434-306	national_version=kz	bucks=4.288165	client_id=8215909"""
)
fake_source['bk'] = (
"""20190120000000	99758	542	1	0	2164	7	2.9028	2902800	1651
20190120000000	99758	542	6	0	56	0	0.0000	0	1651
20190120000000	99758	542	9	0	1	0	0.0000	0	1651
20190120000000	90114	542	1	0	654	0	0.0000	0	395
20190120000000	90114	542	6	0	17	0	0.0000	0	395
20190120000000	90122	542	1	0	129	4	1.8959	1895866	38
20190120000000	90122	542	6	0	1	0	0.0000	0	38
20190120000000	186038	542	6	0	61	1	0.3107	310733	1182
20190120000000	186038	542	9	0	2	0	0.0000	0	1182
20190120000000	186095	542	1	0	6925	44	17.1317	17131733	2471
20190120000000	81877	909	6	0	188	0	0	0	0
20190120000000	59502	932	6	0	1	0	0	0	0
20190120000000	78869	938	6	0	12	0	0	0	0
20190120000000	243744	938	6	0	3	0	0	0	0
20190120000000	150591	942	6	0	137	0	0	0	0
20190120000000	243744	949	6	0	235	0	0	0	0
20190120000000	180864	1184	6	0	1	0	0	0	0
#End"""
)
fake_source['bug_bounty'] = (
"""client_id=536072	currency=RUB	money=4000.01
client_id=536290	currency=USD	money=26.19
client_id=537629	currency=USD	money=13.41
client_id=538442	currency=RUB	money=5233.19"""
)
fake_source['cloud_mp'] = (
'''client_id=106229494	total=5	page_id=11101
client_id=106229494	total=10
client_id=106229493	total=50	page_id=11102'''
)
fake_source['d_installs'] = (
r"""{"values": [
{"path_override_by_dictionary": "1996812","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,
"install_new": 12.0,"path__lvl": 4,"path": "\tR\tYandex software on third party websites\tpunto.browser.yandex.ru\tUnique installation\t1996812\t"},
{"path_override_by_dictionary": "Unique installation","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,
 "install_new": 12.0,"path__lvl": 3,"path": "\tR\tYandex software on third party websites\tpunto.browser.yandex.ru\tUnique installation\t"},
{"path_override_by_dictionary": "punto.browser.yandex.ru","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,
 "install_new": 12.0,"path__lvl": 2,"path": "\tR\tYandex software on third party websites\tpunto.browser.yandex.ru\t"},
{"path_override_by_dictionary": "393","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,
 "install_new": 7.0,"path__lvl": 5,"path": "\tR\tCommercial bundling\tsavefrom.net\tUnique installation\t2157766\t393\t"},
{"path_override_by_dictionary": "392","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,"install_new": 736.0,
 "path__lvl": 5,"path": "\tR\tCommercial bundling\tsavefrom.net\tUnique installation\t2157766\t392\t"},
{"path_override_by_dictionary": "390","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,"install_new": 1.0,
 "path__lvl": 5,"path": "\tR\tCommercial bundling\tsavefrom.net\tUnique installation\t2157766\t390\t"},
{"path_override_by_dictionary": "388","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,"install_new": 5.0,
 "path__lvl": 5,"path": "\tR\tCommercial bundling\tsavefrom.net\tUnique installation\t2157766\t388\t"},
{"path_override_by_dictionary": "2271202","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,
 "install_new": 1.0,"path__lvl": 4,"path": "\tR\tCommercial bundling\tbm.zona.ru\tUnique installation\t2271202\t"},
{"path_override_by_dictionary": "3","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,"install_new": 1.0,
 "path__lvl": 5,"path": "\tR\tCommercial bundling\tbm.zona.ru\tUnique installation\t2224484\t3\t"},
{"path_override_by_dictionary": "2","fielddate": "2019-01-20 00:00:00","fielddate__ms": 1503176400000,"install_new": 1.0,
 "path__lvl": 5,"path": "\tR\tCommercial bundling\tbm.zona.ru\tUnique installation\t2224484\t2\t"}
]}"""
)
fake_source['downloads'] = fake_source['bk']
fake_source['dsp'] = (
"""2019-01-20	128934	2	0	2	2563049	1	0	0	0	12	0	0	0	0	0
2019-01-20	112158	2	0	0	2563113	0	0	0	0	24	0	0	0	0	0
2019-01-20	185291	4	0	0	2563081	0	0	0	0	7	0	0	0	0	0
2019-01-20	188382	227	0	100003014	1	1488	34257590	16272435	610	1488	40427646	0	0	621	7
2019-01-20	142545	104	0	499000	2563049	71	26946000	13473000	27	73	39563000	0	0	0	0
2019-01-20	189903	87	0	100001439	1	155	8508100	4041357	70	2850	10039976	0	0	202	2
2019-01-20	188382	191	0	100003434	10	4544	0	0	0	4544	0	0	0	0	0
2019-01-20	208444	1	0	13565962	2563049	0	0	0	0	1	0	0	0	0	0
2019-01-20	192531	2	0	0	2563081	0	0	0	0	54	0	0	0	0	0
2019-01-20	224669	5	0	0	2563081	0	0	0	0	19	0	0	0	0	0
#end"""
)
fake_source['dsp_rtb'] = fake_source['dsp']
fake_source['partner_rtb'] = fake_source['dsp']

fake_source['rs_market'] = (
"""2297463;225;2019-01-20;0;0;0;0;1
1985209;105;2019-01-20;0;0;0;0;1
1787308;;2019-01-20;58886;119;35.25;3525;788
2051477;;2019-01-20;0;0;0;0;2
2175745;;2019-01-20;0;0;0;0;8
2282513;;2019-01-20;0;0;0;0;4
459909;;2019-01-20;38;0;0;0;0
2210393;;2019-01-20;260102;495;187.37;18737;196
2291166;;2019-01-20;444;0;0;0;4
2136398;;2019-01-20;14079;34;11.82;1182;46"""
)
fake_source['rs_market_cpa'] = (
"""2256021;;2019-01-20;36;0;0;
2270534;;2019-01-20;20;0;0;
2285121;;2019-01-20;1;0;0;
2295143;;2019-01-20;1;0;0;
2271525;;2019-01-20;2297;6;73.2582;
2277535;;2019-01-20;0;2;13.120125;
2290309;225;2019-01-20;1;0;0;
1955451;;2019-01-20;1666;2;10.99039167;
2066562;;2019-01-20;1900;2;16.014785;
2039513;;2019-01-20;3546;2;8.76033333;"""
)
fake_source['rtb_distr'] = (
"""2019-01-20	153638	6	2267211	-1	169	0	0	1	3	19360	19360	1	3	22852	0	0	1	0	2	225
2019-01-20	153638	2	2263185	-1	995	0	0	1	117	4290600	4290600	80	117	5063375	0	0	75	1	2	225
2019-01-20	185103	14	2289243	-1	208	0	0	1	1	0	0	0	1	0	0	0	0	0	2	225
2019-01-20	185103	22	2263191	-1	225	0	0	10	69064	0	0	0	69064	0	0	0	0	0	2	225
2019-01-20	153638	12	2263191	-1	168	0	0	1	176	4131380	4131380	88	176	4875617	0	0	76	1	2	977
2019-01-20	153638	2	2266536	-1	209	0	0	1	9	203250	203250	5	9	239864	0	0	5	0	2	977
2019-01-20	153638	3	2263191	-1	977	0	0	1	10692	83014920	83014920	2136	10692	97970373	0	0	2114	14	2	977
2019-01-20	153638	6	2266536	-1	225	0	0	1	78852	6191256060	6191256060	51713	78852	7305991760	0	0	45568	717	2	977
2019-01-20	153638	12	2266536	-1	134	0	0	1	17	273030	273030	12	17	322245	0	0	11	0	2	977
2019-01-20	153638	1	2278292	-1	137	0	0	1	3	0	0	0	3	0	0	0	0	0	2	225
2019-01-20	49688	21	2064708	-1	225	0	0	2	0	0	0	0	1	0	0	0	0	0	2	225
2019-01-20	49688	32	2100783	3	225	0	0	2	0	0	0	0	1	0	0	0	0	0	2	225
2019-01-20	49688	32	2052594	1	225	0	0	1	1	58660	58660	1	1	69227	0	0	2	0	2	225
2019-01-20	153638	12	2263191	-1	94	0	0	1	9	825490	825490	8	9	974142	0	0	8	0	2	225
2019-01-20	153638	1	2266102	-1	977	0	0	1	153	253700	253700	3	153	299385	0	0	3	0	2	225
2019-01-20	153638	6	2279175	-1	20574	0	0	1	13	677510	677510	7	13	799499	0	0	6	0	2	225
2019-01-20	153638	3	2281101	-1	159	0	0	1	72	59860	59860	20	72	70778	0	0	20	0	2	977
2019-01-20	153638	6	2267211	-1	206	0	0	1	15	1613380	1613380	11	15	1903853	0	0	10	0	2	225
2019-01-20	153638	6	2278292	-1	117	0	0	1	5	79370	79370	3	5	93668	0	0	3	0	2	225
2019-01-20	153638	6	2278336	-1	96	0	0	1	0	55390	55390	1	0	65370	0	0	1	0	2	225
2019-01-20	153638	1	2278336	-1	118	0	0	1	124	969230	969230	25	124	1143815	0	0	25	0	2	225
2019-01-20	153638	6	2267211	-1	187	0	0	1	16	713890	713890	10	16	842440	0	0	10	0	2	171"""
)
fake_source['serphits'] = (
"""20190120000000	63	2286651	84
20190120000000	227826	2041437	135
20190120000000	90	2149692	1
20190120000000	172351	2230936	6
20190120000000	227826	124995	13
20190120000000	172351	2226561	1783
20190120000000	172353	2084462	26
20190120000000	129	2261875	4
20190120000000	63	1969470	7
20190120000000	243663	2257594	36
#End"""
)
fake_source['partner_tag_products'] = (
"""20190120	20	2	542	1	887	32	2394652		2	225
20190120	20	2	742	6	83	0	0		0	225
20190120	20	63	542	1	18	1	15733		2	225
20190120	20	63	742	6	147	0	0		0	255
20190120	20	113	742	6	2	0	0		0	977
20190120	20	129	542	1	37	0	0		2	977
20190120	20	129	742	6	6	0	0		0	977
20190120	20	227826	542	1	9	0	0		2	225
20190120	20	227826	742	6	1	0	0		0	225
20190120	63	1972088	1181	6	6	0	0		0	225
20190120	63	1972089	542	1	1	0	0		2	225
20190120	63	1972090	542	1	5	2	910601		1	977
20190120	63	1972090	542	1	2	0	0		2	171
20190120	63	1972091	542	1	2	0	0		1	171
20190120	63	1972099	1181	6	1	0	0		0	171
20190120	230863	1075242778	542	1	6	0	0		2	977
20190120	230863	1075242794	542	1	5	0	0		2	225
20190120	230863	1075242810	542	1	2	0	0		2	225
20190120	140543	1077231629	542	1	1	0	0		2	225
20190120	140543	1077231633	542	1	1	0	0		2	171
20190120	5597	7602305	542	1	6	0	0		2	225
20190120	5597	7602308	542	1	4	0	0		2	977
20190120	216651	67108955	542	1	7	8	9		2	225
#End"""
)
fake_source['direct_rs'] = fake_source['partner_tag_products']
fake_source['partner_stat_id'] = fake_source['partner_tag_products']
fake_source['taxi_aggr'] = (
"""client_id=34901168	commission_currency=RUB	commission_value=4338.84	type=order	coupon_value=0.0	payment_method=cash
client_id=34901168	commission_currency=RUB	commission_value=39.5	type=order	coupon_value=0.0	payment_method=corporate
client_id=34905406	commission_currency=RUB	commission_value=64.3	type=order	coupon_value=0.0	payment_method=card
client_id=37193959	commission_currency=RUB	commission_value=1022.2	type=order	coupon_value=0.0	payment_method=card
client_id=37193959	commission_currency=RUB	commission_value=3426.8	type=order	coupon_value=0.0	payment_method=cash
client_id=37193959	commission_currency=RUB	commission_value=37.5	type=order	coupon_value=0.0	payment_method=corporate
client_id=37194208	commission_currency=RUB	commission_value=344.747627	type=order	coupon_value=0.0	payment_method=card
client_id=38172961	commission_currency=RUB	commission_value=6429.281249	type=order	coupon_value=0.0	payment_method=card
client_id=38172961	commission_currency=RUB	commission_value=6068.910057	type=order	coupon_value=0.0	payment_method=cash
client_id=38172961	commission_currency=RUB	commission_value=309.006561	type=order	coupon_value=0.0	payment_method=corporate"""
)
fake_source['taxi_distr'] = (
"""utc_dt=2019-01-20	clid=2319588	product_id=13001	quantity=5
utc_dt=2019-01-20	clid=2319588	vid=3	product_id=13001	quantity=2
utc_dt=2019-01-20	clid=2319588	product_id=13002	commission=1901.90	cost=10010.00	quantity=11"""
)
fake_source['taxi_medium'] = (
"""count=3522	clid=2040463	commission_value=2300.402857
count=1319	clid=2046579	commission_value=711.366497
count=23	clid=2046818	commission_value=4.683405
count=1	clid=2058507	commission_value=0.842541
count=236	clid=2059749	commission_value=100.111212
count=4	clid=2061400	commission_value=4.807665
count=3	clid=2104423	commission_value=0.277805
count=2	clid=2120168	commission_value=0.749922
count=1150	clid=2190366	commission_value=526.806281
count=2	clid=2221170	commission_value=0.658667
count=3	clid=2222875	commission_value=7.742447
count=199	clid=2228586	commission_value=76.750744"""
)
fake_source['video_distr'] = (
"""2019-01-20	49688	16	2242347	-1	225	0	0	2563180	0	3000	0	0	4	0	0	0	0	0	5	225
2019-01-20	49688	13	2242347	-1	225	33	2	2	1	0	0	0	1	0	0	0	0	0	3	149
2019-01-20	49688	113	2242347	-1	225	0	0	1	0	0	0	0	33	0	0	0	0	0	2	171
2019-01-20	260290	1	2321787	-1	225	16	2	2563117	2	237660	166362	1	2	2376600	0	0	0	0	5	208
2019-01-20	260290	1	2321787	-1	225	0	0	1	0	0	0	0	8	0	0	0	0	0	6	186
2019-01-20	231296	7	2321787	-1	225	16	2	2563117	2	167370	117159	1	2	1673700	0	0	0	0	5	225
2019-01-20	260290	1	2321787	-1	225	16	2	2563117	2	237660	166362	1	2	2376600	0	0	0	0	5	171"""
)
fake_source['zen_distr'] = (
'''name=lava_non_exclusive	money=153.11	currency=USD	clid=2296334	billing_period=2019-01-20
name=huawei_zenapp_preload	money=59.52	currency=USD	clid=2327439	billing_period=2019-01-20
name=samsung_ntplink_mobile	money=2649.95	currency=RUB	clid=2337300	billing_period=2019-01-20
name=zte	money=0.	currency=EUR	clid=2296341	billing_period=2019-01-20
name=sony	money=5.14	currency=USD	clid=2330903	billing_period=2019-01-20
'''
)
fake_source['ingame_purchases'] = (
"""client_id=2250285	product_type=in-app purchase	amount=68.915	currency=RUB	game_id=97418	consume_token=04afc7ee-82a1-4eab-8c13-8fdcf99bdd57
client_id=2250285	product_type=in-app purchase	amount=171.5875	currency=RUB	game_id=97418	consume_token=150f6f0f-28a6-4da9-a03f-d077369cdf2a
client_id=61692391	product_type=in-app purchase	amount=24.12025	currency=RUB	game_id=99348	consume_token=51180fa5-a9c4-4757-9e18-6589ef4b7334
client_id=61692391	product_type=in-app purchase	amount=24.02225	currency=RUB	game_id=99348	consume_token=5eb49a3d-68c0-439a-b728-754b61ef54e2
client_id=61692391	product_type=in-app purchase	amount=48.53475	currency=RUB	game_id=99348	consume_token=634b11db-380e-417b-8b51-08eb68e0e112
client_id=71981905	product_type=in-app purchase	amount=17.15875	currency=RUB	game_id=98825	consume_token=80c51cc9-f4da-4077-a5a1-b39a6ce19a4e
client_id=71981905	product_type=in-app purchase	amount=17.15875	currency=RUB	game_id=98825	consume_token=81f950e2-93ae-42fb-8c47-cca9121b4a1c
client_id=61692391	product_type=in-app purchase	amount=14.27525	currency=RUB	game_id=97271	consume_token=9bcd31f0-5608-4b93-806f-3ccd8b2a7c11
client_id=69552663	product_type=in-app purchase	amount=29.04275	currency=RUB	game_id=98781	consume_token=9c51e23c-5b82-4245-b43c-2d337f94e114"""
)
fake_source['advertisement'] = (
"""client_id=2250285	product_type=advertisement	amount=68.915	currency=RUB	game_id=97418	consume_token=04afc7ee-82a1-4eab-8c13-8fdcf99bdd57
client_id=2250285	product_type=advertisement	amount=171.5875	currency=RUB	game_id=97418	consume_token=150f6f0f-28a6-4da9-a03f-d077369cdf2a
client_id=61692391	product_type=advertisement	amount=24.12025	currency=RUB	game_id=99348	consume_token=51180fa5-a9c4-4757-9e18-6589ef4b7334
client_id=61692391	product_type=advertisement	amount=24.02225	currency=RUB	game_id=99348	consume_token=5eb49a3d-68c0-439a-b728-754b61ef54e2
client_id=61692391	product_type=advertisement	amount=48.53475	currency=RUB	game_id=99348	consume_token=634b11db-380e-417b-8b51-08eb68e0e112
client_id=71981905	product_type=advertisement	amount=17.15875	currency=RUB	game_id=98825	consume_token=80c51cc9-f4da-4077-a5a1-b39a6ce19a4e
client_id=71981905	product_type=advertisement	amount=17.15875	currency=RUB	game_id=98825	consume_token=81f950e2-93ae-42fb-8c47-cca9121b4a1c
client_id=61692391	product_type=advertisement	amount=14.27525	currency=RUB	game_id=97271	consume_token=9bcd31f0-5608-4b93-806f-3ccd8b2a7c11
client_id=69552663	product_type=advertisement	amount=29.04275	currency=RUB	game_id=98781	consume_token=9c51e23c-5b82-4245-b43c-2d337f94e114"""
)

expected = {}
expected['activations'] = {
    (3010, 2257150, 10): (3010, 472),
    (3010, 2257150, 11): (3010, 55),
    (3010, 2257150, 15): (3010, 20),
    (3010, 2257150, 3): (3010, 8),
    (3010, 2257150, 5): (3010, 1),
    (3010, 2257150, 8): (3010, 3),
    (3010, 2257150, 9): (3010, 29),
    (3010, 2257166, None): (3010, 19),
    (3010, 2257310, None): (3010, 3),
    (3010, 2257342, None): (3010, 451),
}
expected['addappter2_retail'] = {
    (20001, 2155515): (20001, D('791')),
    (20001, 2155516): (20001, D('489'))
}

expected['addapter_dev_ds'] = {
    (4011, 777888999): (4011, 6, 2),
    (4011, 87452): (4011, D('54.21'), 1),
    (4011, 8754): (4011, D('24.51'), 1),
    (4011, 4874): (4011, D('54.21'), 1),
    (4011, 2276700): (4011, D('32.04'), 1),
    (4011, 87565442): (4011, D('22.22'), 1),
    (4011, 521): (4011, D('22.22'), 1),
    (4011, 2106502): (4011, 534, 6),
    (4011, 25421): (4011, D('21.21'), 1),
    (4011, 2075745): (4011, 45, 1),
}
expected['addapter_dev_com'] = {
    (4012, 87565442): (4012, D('22.22'), 1),
    (4012, 2276700): (4012, D('32.04'), 1),
    (4012, 2075745): (4012, 45, 1),
    (4012, 25421): (4012, D('21.21'), 1),
    (4012, 777888999): (4012, 6, 2),
    (4012, 2106502): (4012, 534, 6),
    (4012, 8754): (4012, D('24.51'), 1),
    (4012, 4874): (4012, D('54.21'), 1),
    (4012, 87452): (4012, D('54.21'), 1),
    (4012, 521): (4012, D('22.22'), 1),
}
expected['addapter_ret_com'] = {
    (4010, 123): (4010, D('358.01'), 8),
    (4010, 2106550): (4010, 654, 6),
    (4010, 127774): (4010, 4, 2),
}
expected['addapter_ret_ds'] = {
    (4009, 127774): (4009, 4, 2),
    (4009, 2106550): (4009, 654, 6),
    (4009, 123): (4009, D('358.01'), 8),
}
expected['api_market'] = {
    (2070, 139092): (2070, 577461, 0, 0),
    (2070, 141487): (2070, 873, 1, D('1.52')),
    (2070, 2294597): (2070, 15, 0, 0),
    (2070, 152543): (2070, 2135554, 36, D('12.21')),
    (2070, 219512): (2070, 5233, 5, D('1.3')),
    (2070, 152593): (2070, 182, 0, 0),
    (2070, 2289306): (2070, 5779, 0, 0),
    (2070, 170108): (2070, 4520291, 180, D('52.09')),
    (2070, 151198): (2070, 67293, 66, D('5.45')),
    (2070, 2291287): (2070, 120915, 0, 0),
}
expected['avia_rs'] = {
    (10007, 2256434, 306): (10007, 4, 4, 5717553),
    (10007, 2262097, 252): (10007, 1, 1, 1429388),
    (10007, 2279278, 100): (10007, 4, 4, 5717553),
    (10007, 2323977, 671): (10007, 1, 1, 1429388),
    (10007, 2323977, 673): (10007, 1, 1, 1429388),
    (10007, 2328169, 637): (10007, 1, 1, 1429388)
}
expected['bk'] = {
    (542, 186095): (542, 6925, 44, D('17.1317'), 2471),
    (542, 99758): (542, 2221, 7, D('2.9028'), 4953),
    (542, 186038): (542, 63, 1, D('0.3107'), 2364),
    (542, 90122): (542, 130, 4, D('1.8959'), 76),
    (542, 90114): (542, 671, 0, 0, 790),
}
expected['bug_bounty'] = {
    (20701, 536072, 643): (20701, D('4000.01')),
    (20701, 536290, 840): (20701, D('26.19')),
    (20701, 537629, 840): (20701, D('13.41')),
    (20701, 538442, 643): (20701, D('5233.19')),
}
expected['cloud_mp'] = {
    (11101, 106229494): (11101, D('15')),
    (11102, 106229493): (11102, D('50')),
}
expected['d_installs'] = {
    (10001, 2157766, 388): (10001, 5),
    (10001, 2157766, 393): (10001, 7),
    (10001, 2224484, 2): (10001, 1),
    (10001, 1996812, None): (10001, 12),
    (10001, 2157766, 392): (10001, 736),
    (10001, 2157766, 390): (10001, 1),
    (10001, 2271202, None): (10001, 1),
    (10001, 2224484, 3): (10001, 1),
}
expected['direct_rs'] = {
    (10000, 1972089, None, 1, 225): (10000, 1, 0, 0),
    (10000, 1972090, None, 1, 171): (10000, 2, 0, 0),
    (10000, 1972090, None, 1, 225): (10000, 5, 2, 910601),
    (10000, 1972091, None, 1, 171): (10000, 2, 0, 0)
}
expected['downloads'] = {
    (909, 81877): (909, 188),
    (932, 59502): (932, 1),
    (938, 78869): (938, 12),
    (938, 243744): (938, 3),
    (942, 150591): (942, 137),
    (949, 243744): (949, 235),
    (1184, 180864): (1184, 1),
}
expected['dsp_rtb'] = {
    (100006, 2881904, 1): (100006, 680, 1643, 42765690),
    (100006, 2881904, 10): (100006, 0, 4544, 0),
    (100006, 2901618, 2563049): (100006, 27, 72, 26946000),
    (100006, 7207570, 2563113): (100006, 0, 0, 0),
    (100006, 2901617, 2563081): (100006, 0, 0, 0),
}
expected['partner_rtb'] = {
    (100002, 192531, 2, 2563081, 2901617, 0, 0): (100002, 0, 0, 0),
    (100002, 142545, 104, 2563049, 2901618, 0, 499000): (100002, 27, 71, 13473000),
    (100002, 208444, 1, 2563049, 2901618, 0, 13565962): (100002, 0, 0, 0),
    (100002, 112158, 2, 2563113, 7207570, 0, 0): (100002, 0, 0, 0),
    (100002, 188382, 191, 10, 2881904, 0, 100003434): (100002, 0, 4544, 0),
    (100002, 185291, 4, 2563081, 2901617, 0, 0): (100002, 0, 0, 0),
    (100002, 189903, 87, 1, 2881904, 0, 100001439): (100002, 70, 155, 4041357),
    (100002, 224669, 5, 2563081, 2901617, 0, 0): (100002, 0, 0, 0),
    (100002, 128934, 2, 2563049, 2901618, 0, 2): (100002, 0, 1, 0),
    (100002, 188382, 227, 1, 2881904, 0, 100003014): (100002, 610, 1488, 16272435)
}
expected['partner_stat_id'] = {
    (2090, 7602305, 5597, 1): (2090, 6, 0, 0),
    (2090, 129, 20, 6): (2090, 6, 0, 0),
    (2090, 2, 20, 6): (2090, 83, 0, 0),
    (2090, 1075242810, 230863, 1): (2090, 2, 0, 0),
    (2090, 1075242794, 230863, 1): (2090, 5, 0, 0),
    (2090, 113, 20, 6): (2090, 2, 0, 0),
    (2090, 63, 20, 6): (2090, 147, 0, 0),
    (2090, 227826, 20, 6): (2090, 1, 0, 0),
    (2090, 1972099, 63, 6): (2090, 1, 0, 0),
    (2090, 1972088, 63, 6): (2090, 6, 0, 0),
    (2090, 7602308, 5597, 1): (2090, 4, 0, 0),
    (2090, 1077231633, 140543, 1): (2090, 1, 0, 0),
    (2090, 1077231629, 140543, 1): (2090, 1, 0, 0),
    (2090, 1075242778, 230863, 1): (2090, 6, 0, 0)
}
expected['partner_tag_products'] = {
    (632, 2, 1): (632, D(887), 32, 2394652),
    (632, 227826, 1): (632, 9, 0, 0),
    (632, 129, 1): (632, 37, 0, 0),
    (632, 63, 1): (632, 18, 1, 15733)
}
expected['rs_market'] = {
    (10003, 2282513, None): (10003, 0, 0, 4, 0),
    (10003, 2136398, None): (10003, 14079, 34, 46, 11820000),
    (10003, 459909, None): (10003, 38, 0, 0, 0),
    (10003, 2210393, None): (10003, 260102, 495, 196, 187370000),
    (10003, 1985209, 105): (10003, 0, 0, 1, 0),
    (10003, 2297463, 225): (10003, 0, 0, 1, 0),
    (10003, 1787308, None): (10003, 58886, 119, 788, 35250000),
    (10003, 2175745, None): (10003, 0, 0, 8, 0),
    (10003, 2291166, None): (10003, 444, 0, 4, 0),
    (10003, 2051477, None): (10003, 0, 0, 2, 0)
}
expected['rs_market_cpa'] = {
    (10004, 2285121, None): (10004, 1, 0, 0, 0),
    (10004, 2270534, None): (10004, 20, 0, 0, 0),
    (10004, 2066562, None): (10004, 1900, 2, 16014785, 0),
    (10004, 2290309, 225): (10004, 1, 0, 0, 0),
    (10004, 2256021, None): (10004, 36, 0, 0, 0),
    (10004, 2277535, None): (10004, 0, 2, 13120125, 0),
    (10004, 1955451, None): (10004, 1666, 2, D('10990391.67'), 0),
    (10004, 2039513, None): (10004, 3546, 2, D('8760333.33'), 0),
    (10004, 2295143, None): (10004, 1, 0, 0, 0),
    (10004, 2271525, None): (10004, 2297, 6, 73258200, 0)
}
expected['rtb_distr'] = {
    (10000, 2263185, None, None, 225): (10000, 75, 1, 171624),
    (10000, 2263191, None, None, 225): (10000, 2198, 15, D('3518871.6')),
    (10000, 2266102, None, None, 225): (10000, 3, 0, 10148),
    (10000, 2266536, None, None, 225): (10000, 45584, 717, D('247669293.6')),
    (10000, 2267211, None, None, 225): (10000, 11, 0, D('65309.6')),
    (10000, 2267211, None, None, 171): (10000, 10, 0, D('28555.6')),
    (10000, 2278292, None, None, 225): (10000, 3, 0, D('3174.8')),
    (10000, 2278336, None, None, 225): (10000, 26, 0, D('40984.8')),
    (10000, 2279175, None, None, 225): (10000, 6, 0, D('27100.4')),
    (10000, 2281101, None, None, 225): (10000, 20, 0, D('2394.4'))
}
expected['serphits'] = {
    (100005, 124995): (100005, 13),
    (100005, 1969470): (100005, 7),
    (100005, 2041437): (100005, 135),
    (100005, 2084462): (100005, 26),
    (100005, 2149692): (100005, 1),
    (100005, 2226561): (100005, 1783),
    (100005, 2230936): (100005, 6),
    (100005, 2257594): (100005, 36),
    (100005, 2261875): (100005, 4),
    (100005, 2286651): (100005, 84),
}
expected['taxi_distr'] = {
    (13001, 2319588, None, None, None, None, None): (13001, 5, 0, None, None),
    (13001, 2319588,    3, None, None, None, None): (13001, 2, 0, None, None),
    (13002, 2319588, None, None, None, None, None): (13002, 11, 76076000, None, None),
}
expected['taxi_medium'] = {
    (10002, 2040463): (10002, 3522, 2300402857),
    (10002, 2046579): (10002, 1319, 711366497),
    (10002, 2046818): (10002, 23, 4683405),
    (10002, 2058507): (10002, 1, 842541),
    (10002, 2059749): (10002, 236, 100111212),
    (10002, 2061400): (10002, 4, 4807665),
    (10002, 2104423): (10002, 3, 277805),
    (10002, 2120168): (10002, 2, 749922),
    (10002, 2190366): (10002, 1150, 526806281),
    (10002, 2221170): (10002, 2, 658667),
    (10002, 2222875): (10002, 3, 7742447),
    (10002, 2228586): (10002, 199, 76750744),
}
expected['video_distr'] = {
    (13003, 2242347, None): (13003, 0, 0, 120),
    (13003, 2321787, None): (13003, 0, 0, D('25707.6'))
}
expected['zen_distr'] = {
    (10100, 2296334, 840): (10100, D('153.11')),
    (10100, 2327439, 840): (10100, D('59.52')),
    (10100, 2337300, 643): (10100, D('2649.95')),
    (10100, 2296341, 840): (10100, D('0')),
    (10100, 2330903, 840): (10100, D('5.14')),
}
expected['ingame_purchases'] = {
    (67701, 2250285, 643, 97418): (67701, D('240.5025')),
    (67701, 61692391, 643, 99348): (67701, D('96.67725')),
    (67701, 71981905, 643, 98825): (67701, D('34.3175')),
    (67701, 61692391, 643, 97271): (67701, D('14.27525')),
    (67701, 69552663, 643, 98781): (67701, D('29.04275')),
}
expected['advertisement'] = {
    (113201, 2250285, 643, 97418): (113201, D('240.5025')),
    (113201, 61692391, 643, 99348): (113201, D('96.67725')),
    (113201, 71981905, 643, 98825): (113201, D('34.3175')),
    (113201, 61692391, 643, 97271): (113201, D('14.27525')),
    (113201, 69552663, 643, 98781): (113201, D('29.04275')),
}


class HasNoTestData(Exception):
    pass


class FakeEntityCache(object):

    def __init__(self):
        self.cache = None

    def bulk_get(self, key_df):
        res_df = key_df
        res_df['entity_id'] = [1000 + i for i in range(len(key_df))]
        self.cache = res_df
        return res_df

    def format_actual_values(self, act_val, short_key_len, short_val_len):
        df = self.cache
        res = {}
        for (eid, value_tuple) in act_val.items():

            key_tuple = tuple(next(df[df.entity_id == eid][KEY_TEMPLATE].itertuples(index=False)))
            key_tuple = key_tuple[:short_key_len + 1]
            key_tuple = tuple((v if v != -1 else None)
                              for v in key_tuple)
            value_tuple = value_tuple[:short_val_len + 1]
            res[key_tuple] = value_tuple
        return res


def prepare_class(cls):

    if not issubclass(cls, (CompletionBase, CompUniversal)):
        raise TypeError

    if cls._cfg_name not in fake_source:
        raise HasNoTestData('Has no test data for %s' % cls._cfg_name)

    def fake_http(*args, **kwargs):
        return StringIO(fake_source[cls._cfg_name])

    cls.get_http = fake_http
    return cls


def fix_object(obj):
    obj.entity_cache = FakeEntityCache()


class Rollback(Exception):
    pass


class TestCompletions(BalanceTest):

    def test_general(self):
        app = self.app
        print(app.cfg_path)
        for name, cls in CompletionBase._comp_getters.items():
            if name not in CODES:
                continue
            try:
                print(name)
                ses = app.real_new_session()
                with ses.begin():
                    cls = prepare_class(cls)
                    obj = cls(ses, dt)
                    fix_object(obj)
                    obj.cluster_get()
                    # Тестовая запись в БД произведена
                    # Теперь прочитаем её результат

                    columns = EntityCompletions.__table__.c

                    res = (ses.query(EntityCompletions)
                           .filter(columns.dt == dt)
                           .filter(columns.src_id == obj._src_id)
                           ).all()

                    actual_values = {item.entity_id:
                                     tuple(getattr(item, v) for v in ['product_id'] + list(VAL_TEMPLATE))
                                     for item in res}

                    # и откатим её
                    raise Rollback
                    # А теперь сравним то, что фактически было записано, с тем, что должно быть
            except Rollback:

                expected_values = expected.get(name, {})

                ids = actual_values.keys()
                self.assertEqual(len(ids),
                                 len(set(ids)),
                                 "Not uniq entity IDs inserts in db for %s" % name
                                 )

                keys = [field for field in obj._field_map.values()
                        if field.startswith('key_')]

                # У rtb_distr пропущен key_num_3 в целях совместимости с direct_rs,
                # поэтому нельзя просто посчитать количество ключей, беру по максимальному номеру

                short_key_len = max([int(field[8]) for field in keys]) if keys else len(KEY_TEMPLATE)

                short_val_len = sum(1 for field in obj._field_map.values()
                                    if field.startswith('val_')
                                    ) or len(VAL_TEMPLATE)

                formatter = obj.entity_cache.format_actual_values

                actual_values = formatter(actual_values, short_key_len, short_val_len)

                pprint(expected_values)
                pprint(actual_values)

                self.assertEquals(actual_values, expected_values,
                                  "Rows inserted in db differs from expected for %s" % name)
            except HasNoTestData as e:
                print(str(e))
                raise


if __name__ == '__main__':
    import unittest
    unittest.main()
