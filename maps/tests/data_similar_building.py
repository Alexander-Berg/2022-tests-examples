AD_NM = [
    {'ad_id': 4001, 'lang': b'ru', 'script': None, 'name_type': 0, 'name': 'Москва'.encode('utf-8')},
    {'ad_id': 4002, 'lang': b'ru', 'script': None, 'name_type': 0, 'name': 'Владивосток'.encode('utf-8')},
]

ADDR = [
    {'addr_id': 55864409, 'ad_id': None, 'node_id': 2001, 'rd_id': 3001},
    {'addr_id': 55864438, 'ad_id': None, 'node_id': 2002, 'rd_id': 3002},
    {'addr_id': 10020, 'ad_id': None, 'node_id': 2003, 'rd_id': 3001},
    {'addr_id': 10021, 'ad_id': None, 'node_id': 2004, 'rd_id': 3002},
]

ADDR_NM = [
    {'addr_id': 55864438, 'lang': b'ru', 'script': None, 'name_type': 0, 'name': '200к321'.encode('utf-8')},
    {'addr_id': 55864409, 'lang': b'ru', 'script': None, 'name_type': 0, 'name': '200к123'.encode('utf-8')},
    {'addr_id': 10020, 'lang': b'ru', 'script': None, 'name_type': 0, 'name': '15'.encode('utf-8')},
    {'addr_id': 10021, 'lang': b'ru', 'script': None, 'name_type': 0, 'name': '16'.encode('utf-8')},
]

BLD = [
    {'bld_id': 1689062597, 'height': 36},
    {'bld_id': 3399097075, 'height': 36},
    {'bld_id': 116460008, 'height': 50},
    {'bld_id': 116454149, 'height': 50},
]

BLD_ADDR = [
    {'addr_id': 55864409, 'bld_id': 1689062597},
    {'addr_id': 55864438, 'bld_id': 3399097075},
    {'addr_id': 10020, 'bld_id': 116460008},
    {'addr_id': 10021, 'bld_id': 116454149},
]

BLD_GEOM = [
    {
        'bld_id': 1689062597, 'xmax': 91.458348369, 'xmin': 91.458007442, 'ymax': 53.742269159, 'ymin': 53.74191056,
        'shape': (
            b'0103000020E610000001000000130000000EC33A9A52DD564004CAD695F7DE4A40B0051DAA52DD5640B784BB34F7DE4A408D7C8'
            b'97151DD5640E7E0DBECF6DE4A4012ACA7DF50DD56409239C968FADE4A4018B2E55550DD56405F791E49FADE4A40BC2D72FE4FDD5'
            b'64070C9CA5FFCDE4A40AF7F16CC51DD5640C0E5EEC9FCDE4A40163EAE4F51DD5640E8888BC2FFDE4A4091A2DD0751DD564077490'
            b'9B2FFDE4A409A0BA8AC50DD5640AFF7A6DF01DF4A403A8EC92954DD5640025E01AD02DF4A406AA330AB54DD56406B72D595FFDE4'
            b'A407D1A673B55DD5640654DFCB6FFDE4A40EBC1659455DD5640CC37E296FDDE4A40A2F936D953DD5640EDBBFC30FDDE4A409BEF4'
            b'55354DD56407ABCBE46FADE4A4071ADD0E954DD5640F7405B69FADE4A40A0E56C4655DD56408DB22833F8DE4A400EC33A9A52DD5'
            b'64004CAD695F7DE4A40'
        )
    },
    {
        'bld_id': 3399097075, 'xmax': 91.458168661, 'xmin': 91.457827734, 'ymax': 53.742785126, 'ymin': 53.742426531,
        'shape': (
            b'0103000020E6100000010000001300000028C47AA84FDD564010131E7E08DF4A40CA065DB84FDD5640C3CD021D08DF4A40A77DC'
            b'97F4EDD5640F32923D507DF4A402BADE7ED4DDD5640DC5C0E510BDF4A4032B325644DDD5640AA9C63310BDF4A40D62EB20C4DDD5'
            b'640F9C60D480DDF4A40C98056DA4EDD564049E331B20DDF4A40303FEE5D4EDD5640B060CCAA10DF4A40AAA31D164EDD56403F214'
            b'A9A10DF4A40B30CE8BA4DDD5640B5A9E5C712DF4A40538F093851DD56400810409513DF4A4083A470B951DD5640334A167E10DF4'
            b'A40961BA74952DD56402D253D9F10DF4A4004C3A5A252DD56405535257F0EDF4A40BCFA76E750DD564076B93F190EDF4A40B4F08'
            b'56151DD5640C5DF032F0BDF4A408AAE10F851DD56404264A0510BDF4A40BAE6AC5452DD564099FB6F1B09DF4A4028C47AA84FDD5'
            b'64010131E7E08DF4A40'
        )
    },
    {
        'bld_id': 116460008, 'xmax': 27.500704775, 'xmin': 27.500012339, 'ymax': 53.896996455, 'ymin': 53.896408086,
        'shape': (
            b'0103000020E61000000100000005000000EEDC9AC522803B40B79E0A80BDF24A40FA9903CF00803B40B4A016D4CEF24A4089E79A'
            b'390C803B402D6DA3C7D0F24A40779329302E803B40306B9773BFF24A40EEDC9AC522803B40B79E0A80BDF24A40'
        )
    },
    {
        'bld_id': 116454149, 'xmax': 27.513181258, 'xmin': 27.51212396, 'ymax': 53.893376552, 'ymin': 53.893090243,
        'shape': (
            b'0103000020E61000000100000005000000B1B24B8E1A833B40CCD94F8754F24A40063130285C833B4037EDB0295AF24A40B707D0'
            b'D85F833B40511B566A56F24A40EB6BF83E1E833B40E607F5C750F24A40B1B24B8E1A833B40CCD94F8754F24A40'
        )
    },
]

ENTRANCE_FLAT_RANGE = [
    {'ft_id': 1001, 'flat_first': b'1', 'flat_last': b'9', 'is_exact': True},
    {'ft_id': 1002, 'flat_first': b'4', 'flat_last': b'5', 'is_exact': False},
    {'ft_id': 1003, 'flat_first': b'1', 'flat_last': b'9', 'is_exact': True},
    {'ft_id': 1004, 'flat_first': b'4', 'flat_last': b'5', 'is_exact': False},
]

FT_ADDR = [
    {'addr_id': 55864409, 'ft_id': 1001, 'role': 3},
    {'addr_id': 55864438, 'ft_id': 1002, 'role': 3},
    {'addr_id': 10020, 'ft_id': 1003, 'role': 3},
    {'addr_id': 10021, 'ft_id': 1004, 'role': 3},
]

FT_NM = [
    {'ft_id': 1001, 'script': None, 'name': b'1'},
    {'ft_id': 1002, 'script': None, 'name': b'1'},
    {'ft_id': 1003, 'script': None, 'name': b'1'},
    {'ft_id': 1004, 'script': None, 'name': b'1'},
]

NODE = [
    {'node_id': 2001, 'x': 10.1, 'y': 20.1},
    {'node_id': 2002, 'x': 10.2, 'y': 20.2},
    {'node_id': 2003, 'x': 10.4, 'y': 20.5},
    {'node_id': 2004, 'x': 10.5, 'y': 20.6},
]

RD_AD = [
    {'rd_id': 3001, 'ad_id': 4001},
    {'rd_id': 3002, 'ad_id': 4002},
]

RD_NM = [
    {'rd_id': 3001, 'lang': b'ru', 'script': None, 'name_type': 0, 'name': 'Продольная'.encode('utf-8')},
    {'rd_id': 3002, 'lang': b'ru', 'script': None, 'name_type': 0, 'name': 'Поперечная'.encode('utf-8')},
]

HYPOTHESES_DATA = [
    {
        'type': 'geoq-flats-similar-building',
        'lat': 20.2,
        'lon': 10.2,
        'text': (
            'Этот дом похож на дом по адресу [Москва, Продольная, 200к123](https://n.maps.yandex.ru/#!/objects/1689062597), '
            'в котором есть полностью размеченные подъезды.\n'
            'Если дома одинаковые, то разметку можно повторить.\n'
            'Полностью размеченные подъезды в доме по адресу "Москва, Продольная, 200к123":\n'
            'Подъезд 1: квартиры [1-9]\n'
            'Известные неточные квартиры в текущем доме:\n'
            'Подъезд 1: квартиры [4-5]'
        ),
    },
]
