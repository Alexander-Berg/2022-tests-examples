# encoding: utf-8
import pytest

from infra.yasm.gateway.lib.tags.request import RequestKey
from infra.yasm.gateway.lib.tags.errors import TagsError


class DynamieCase(object):
    def __init__(self, dynamic, requests):
        self.requests = [dynamic] + requests  # Запросы, которые должны трактоваться как одинаковые
        self.dynamic = dynamic  # Запрос к которому они все должны приводиться

    def assert_all_requests_are_equal(self):
        set_to_check_hash_and_eq = {RequestKey.from_string(self.dynamic)}
        for req in self.requests:
            req_key = RequestKey.from_string(req)
            assert req_key.to_dynamic_string() == self.dynamic
            assert req_key in set_to_check_hash_and_eq


TEST_INVALID_UNDERSCORED_REQUESTS_CASES = [
    ["mmeta", "self"], [], (), {"mmeta": "self"}, {}, None, 10, 20.0, -10, "",

    "mmeta|",
    "mmeta;",
    u"ммета",
    "_self",
    "mmeta____yandsearch",
    "mmeta_custom_self",
    "mmeta_se|lf",
    "mmeta_prod_web_main_asm_yandsearch",
    "mmeta_prod_self",
    "mmeta_yandsearch",
]
TEST_VALID_UNDERSCORED_REQUESTS_CASES = [
    u"mmeta_prod_web-main_ams_yandsearch",
    "mmeta_prod_web-main_ams_yandsearch",
    "mmeta_self",
    u"mmeta_self",
]
TEST_UNIFICATION_CASES = [
    DynamieCase(  # Большой селф
        'itype=mmeta',
        ['mmeta_self', 'itype=mmeta;tier=*', 'itype=mmeta;tier=self',
         'itype=mmeta;ctype=*', 'itype=mmeta;ctype=*;geo=man,*']),
    DynamieCase(  # Маленький селф
        'itype=mmeta;ctype=prod;geo=msk;prj=web',
        ['mmeta_prod_web_msk_self',
         'itype=mmeta;ctype=prod;prj=web;geo=msk',
         'itype=mmeta;geo=msk;prj=web;ctype=prod',
         'itype=mmeta;geo=msk;prj=web;ctype=prod;tier=self',
         'itype=mmeta;geo=msk;prj=web;ctype=prod;tier=*',
         'itype=mmeta;geo=msk;prj=web;ctype=prod;tier=*,yandsearch',
         ]),
    DynamieCase(  # Полный тэг
        'itype=mmeta;ctype=prod;geo=msk;prj=web;tier=yandsearch',
        ['mmeta_prod_web_msk_yandsearch',
         'itype=mmeta;ctype=prod;prj=web;geo=msk;tier=yandsearch',
         'itype=mmeta;tier=yandsearch;geo=msk;prj=web;ctype=prod',
         ]),
    DynamieCase(  # Неполный тэг
        'itype=mmeta;geo=msk;prj=web',
        ['prj=web;geo=msk;itype=mmeta',  # переставленные ключи
         'itype=mmeta;geo=msk;ctype=*;prj=web',  # лишний wildcard
         'itype=mmeta;geo=msk;ctype=prod,*;prj=web',  # лишний wildcard
         ]),
    DynamieCase(  # Тэг с несколькими значениями
        'itype=mmeta;ctype=prestable,testing;geo=msk;prj=web',
        ['prj=web;ctype=testing,prestable;itype=mmeta;geo=msk',  # переставленные ключи и значения
         ]),
    DynamieCase(  # Тэг со звёздочками
        'itype=mmeta;ctype=prod;geo=m*;prj=web',
        ['prj=web;ctype=prod;geo=m*;itype=mmeta',  # переставленные ключи и значения
         ]),
    DynamieCase(  # Пять тэгов, но один из них "недефолтный"
        'itype=mmeta;collection=images;ctype=prod;geo=msk;prj=web',
        ['itype=mmeta;ctype=prod;prj=web;geo=msk;collection=images',
         'itype=mmeta;ctype=prod;prj=web;geo=msk;collection=images;tier=*',
         'itype=mmeta;ctype=prod;prj=web;geo=msk;collection=images;tier=self',
         'itype=mmeta;ctype=prod;prj=web;geo=msk;collection=images;tier=self,yandsearch',
         'itype=mmeta;ctype=prod;prj=web;geo=msk;collection=images;submodule=tass,*',  # А здесь даже шесть тэгов
         ]),
]

TEST_INVALID_DYNAMIC_REQUESTS_CASES = [
    ["mmeta", "self"], [], (), {"mmeta": "self"}, {}, None, 10, 20.0, -10, "",
    "itype=mmeta,upper",
    "ctype=prod;geo=am_s;prj=web-main;tier=yandsearch",
    "ctype=prod;geo=ams;prj=web-main;tier=yandsearch",
    "itype=mmeta,upper;ctype=prod;geo=ams;prj=web-main;tier=yandsearch",
    "itype=;ctype=prod;geo=ams;prj=web-main;tier=yandsearch",
    "itype=ctype=prod;geo=ams;prj=web-main;tier=yandsearch",
    "itype=mmeta;ctype=;geo=ams;prj=web-main;tier=yandsearch",
    "itype=mmeta;=prod;geo=ams;prj=web-main;tier=yandsearch",
    "itype=mmeta;prod;geoams;prj=web-main;tier=yandsearch",
    "itype=mmeta;prod;geo ams;prj=web-main;tier=yandsearch",
    "itype=m*;prod;geo ams;prj=web-main;tier=yandsearch",
    "itype=mmeta;geo=ams;geo=ams",
    "itype=mmeta;geo=ams;geo=man",
    "itype=mmeta;geo=ams;geo=*",
    "itype=mmeta;itype=common;geo=ams",
    "itype=mmeta;itype=mmeta;geo=ams",
]

TEST_VALID_DYNAMIC_REQUESTS_CASES = [
    "itype=mmeta",
    u"itype=mmeta;ctype=prod;geo=ams;prj=web-main;tier=yandsearch",
    "itype=mmeta;ctype=prod;geo=ams;prj=web-main;tier=yandsearch",
    "itype=mmeta;ctype=prestable,prod;geo=ams,msk;prj=web-main;tier=yandsearch",
    "itype=mmeta;ctype=prestable,prod;tier=yandsearch",
    "itype=mmeta;ctype=prestable,prod",
    "itype=mmeta;geo=ams,msk;prj=web-main;tier=yandsearch",
    "itype=mmeta;geo=msk,ams;prj=web-main;tier=yandsearch",
    "itype=mmeta;ctype=prod,prestable;geo=ams,msk;prj=web-main;tier=yandsearch",
    "itype=mmeta;ctype=p*;geo=ams,msk;prj=web-main;tier=yandsearch",
    "itype=mmeta;ctype=p*;geo=*m*;prj=web-main",
    "itype=mmeta;ctype=*;geo=*m*;prj=web-main",

    "itype=mmeta;ctype=prestable, prod",
    "itype=mmeta; ctype=prestable,prod",
    "itype=mmeta; ctype=prestable, prod",
]

TEST_REQUEST_TO_DICT = [
    ("itype=mmeta", {"itype": ["mmeta"]}),
    ("mmeta_self", {"itype": ["mmeta"]}),

    ("mmeta_prod_web-main_ams_yandsearch",
     {"itype": ["mmeta"], "ctype": ["prod"], "geo": ["ams"], "prj": ["web-main"], "tier": ["yandsearch"]}),

    ("mmeta_prod_web-main_ams_self",
     {"itype": ["mmeta"], "ctype": ["prod"], "geo": ["ams"], "prj": ["web-main"]}),

    ("itype=mmeta;ctype=prestable,prod;prj=web-main",
     {"itype": ["mmeta"], "ctype": ["prestable", "prod"], "prj": ["web-main"]}),

    ("itype=mmeta;ctype=p*,t*;geo=*m*;tier=*",
     {"itype": ["mmeta"], "ctype": ["p*", "t*"], "geo": ["*m*"]}),
]


TEST_VALID_AND_SORTED_DYNAMIC_REQUESTS_CASES = [
    "itype=mmeta",
    "itype=mmeta;geo=sas",
    "itype=mmeta;geo=ams,msk;prj=web-main;tier=yandsearch",
    "itype=mmeta;geo=ams,msk,sas;prj=web-main;tier=yandsearch",
]


TEST_DYNAMIC_UNDERSCORED_IDENTITY_CASES = [
    ("itype=mmeta", "mmeta_self"),
]


TEST_DYNAMIC_STRING_FROM_DICT_CASES = [
    ({"itype": ["base"], "ctype": ["prod", "prestable"]}, "itype=base;ctype=prestable,prod"),
    ({"itype": "mmeta"}, "itype=mmeta"),
    ({"itype": ["mmeta"], "geo": "sas"}, "itype=mmeta;geo=sas")
]

TEST_INVALID_DYNAMIC_STRING_FROM_DICT_CASES = [
    ["mmeta", "self"], [], (), None, 10, 20.0, -10, ""
]


@pytest.mark.parametrize("case", TEST_INVALID_UNDERSCORED_REQUESTS_CASES)
def test_invalid_underscored_request(case):
    with pytest.raises(TagsError) as ex:
        RequestKey.from_string(case)

    assert repr(ex.value)
    assert str(ex.value)


@pytest.mark.parametrize("case", TEST_VALID_UNDERSCORED_REQUESTS_CASES)
def test_valid_underscored_request(case):
    request = RequestKey.from_string(case)
    assert request == RequestKey.from_dict(request.to_dict())


@pytest.mark.parametrize("case", TEST_UNIFICATION_CASES)
def test_unification(case):
    case.assert_all_requests_are_equal()


@pytest.mark.parametrize("case", TEST_INVALID_DYNAMIC_REQUESTS_CASES)
def test_invalid_dynamic_request(case):
    with pytest.raises(TagsError) as ex:
        RequestKey.from_string(case)

    assert repr(ex.value)
    assert str(ex.value)


@pytest.mark.parametrize("case", TEST_VALID_DYNAMIC_REQUESTS_CASES)
def test_valid_dynamic_request(case):
    assert RequestKey.from_string(case)
    assert RequestKey.from_string(RequestKey.from_string(case).to_dynamic_string())
    assert repr(RequestKey.from_string(case))


@pytest.mark.parametrize("case", TEST_REQUEST_TO_DICT)
def test_request_to_dict(case):
    src, trg = case
    assert RequestKey.from_string(src).to_dict() == trg


@pytest.mark.parametrize("case", TEST_VALID_AND_SORTED_DYNAMIC_REQUESTS_CASES)
def test_valid_and_sorted_dynamic_request(case):
    assert RequestKey.from_string(case).to_dynamic_string() == case


@pytest.mark.parametrize("case", TEST_DYNAMIC_UNDERSCORED_IDENTITY_CASES)
def test_dynamic_underscored_identity(case):
    dynamic_request = RequestKey.from_string(case[0])
    underscored_request = RequestKey.from_string(case[1])

    assert dynamic_request.to_dynamic_string() == underscored_request.to_dynamic_string()


@pytest.mark.parametrize("case", TEST_DYNAMIC_STRING_FROM_DICT_CASES)
def test_dynamic_string_from_dict(case):
    tag_dict, result = case

    assert RequestKey.from_dict(tag_dict).to_dynamic_string() == result


@pytest.mark.parametrize("case", TEST_INVALID_DYNAMIC_STRING_FROM_DICT_CASES)
def test_invalid_dynamic_string_from_dict(case):
    with pytest.raises(TagsError) as ex:
        assert RequestKey.from_dict(case).to_dynamic_string()

    assert repr(ex.value)
    assert str(ex.value)
