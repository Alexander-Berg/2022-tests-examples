from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType
from big_profile import get_big_bigb_profile, TS, validate_big_profile
import pytest
from ads.bsyeti.libs import py_testlib
import sys
from yatest.common import work_path
import google.protobuf.text_format as text_format
from ads.bsyeti.eagle.data.proto.data_config_pb2 import TClientConfigProto
from yabs.server.proto.keywords import keywords_data_pb2
from yabs.proto.user_profile_pb2 import Profile
from collections import defaultdict
from collections import Counter


def parse_keywords_data():
    # keyword_id -> EValueType as int
    # group -> [keywords as int]
    # all_keywords
    d = keywords_data_pb2.EKeyword.DESCRIPTOR.values_by_number
    kw_to_type = {}
    kw_to_groups = {}
    all_keywords = []
    for k, v in d.items():
        options = v.GetOptions().ListFields()[0][1]
        if options.HasField("ValueType"):
            all_keywords.append(k)
        kw_to_type[k] = options.ValueType
        kw_to_groups[k] = options.DataGroup

    group_to_kws = defaultdict(list)
    for k, v in kw_to_groups.items():
        for i in v:
            group_to_kws[i].append(k)

    return kw_to_type, group_to_kws, all_keywords


def parse_user_profile():
    # field_name -> [keyword_id]
    d = Profile.DESCRIPTOR
    name_to_id = defaultdict(list)
    for field in d.fields:
        try:
            ids = field.GetOptions().ListFields()[0][1]
            for i in ids:
                name_to_id[field.name].append(i)
        except:
            # no keywords
            continue
    return name_to_id


def parse_clients():
    with open(work_path("clients_generated.pb"), "r") as f_p:
        proto_config = text_format.Parse(f_p.read(), TClientConfigProto())
        client_to_keywords = {}
        client_to_keyword_sets = {}
        for i in proto_config.Clients:
            client_to_keywords[i.Name] = list(i.Keywords)
            if i.HasField("InheritKeywordSet"):
                client_to_keyword_sets[i.Name] = i.InheritKeywordSet
            else:
                client_to_keyword_sets[i.Name] = None
        return client_to_keywords, client_to_keyword_sets


@pytest.fixture(scope="module")
def profiles(test_environment_with_kv_saas):
    bigb_uid = test_environment_with_kv_saas.new_uid()

    first_icookie = test_environment_with_kv_saas.new_uid()
    second_icookie = test_environment_with_kv_saas.new_uid()

    kw_to_type, _, _ = parse_keywords_data()

    big_profile = get_big_bigb_profile(kw_to_type)
    validate_big_profile(big_profile)

    test_environment_with_kv_saas.profiles.add(
        {
            "y{id1}".format(id1=bigb_uid): big_profile,
        }
    )

    test_environment_with_kv_saas.vulture.add(
        {
            "y{id}".format(id=bigb_uid): {
                "KeyRecord": {"user_id": "{id}".format(id=bigb_uid), "id_type": 1},
                "ValueRecords": [
                    {"user_id": "{id}".format(id=first_icookie), "id_type": 1, "crypta_graph_distance": 1},
                    {"user_id": "{id}".format(id=second_icookie), "id_type": 1, "crypta_graph_distance": 1},
                    {
                        "user_id": "00000000-0000-0000-0000-000000000001",
                        "id_type": 9,
                        "crypta_graph_distance": 2,
                        "is_indevice": True,
                    },
                    {
                        "user_id": "00000000-0000-0000-0000-000000000002",
                        "id_type": 8,
                        "crypta_graph_distance": 1,
                        "is_indevice": True,
                    },
                    {"user_id": "y777777", "id_type": 2, "crypta_graph_distance": 123},
                ],
            }
        }
    )

    return bigb_uid, first_icookie, second_icookie


def get_clients_infos():

    # returns dict client_name -> list of expected_keywords for this client
    client_to_keywords, client_to_keyword_sets = parse_clients()
    _, group_to_kws, all_keywords = parse_keywords_data()

    clients = client_to_keywords.keys()
    ans = {}
    for client in clients:
        expected_keywords = client_to_keywords[client]
        keyword_sets = client_to_keyword_sets[client]
        if keyword_sets is not None:
            if keyword_sets == 0:
                # add all possible keywords with at least 1 data group
                expected_keywords += all_keywords
                pass
            else:
                expected_keywords += list(group_to_kws[keyword_sets])
        expected_keywords = list(set(expected_keywords))
        ans[client] = expected_keywords
    return ans


def test_apphost(test_environment_with_kv_saas, profiles):
    name_to_id = parse_user_profile()

    bigb_uid, first_icookie, second_icookie = profiles
    first_icookie_key = "y{id}".format(id=first_icookie)
    second_icookie_key = "y{id}".format(id=second_icookie)

    exp_json = exp_json = {
        "Crypta": {"RtSocdem": {"UseRTMR": True}},
        "EagleSettings": {"LoadSettings": {"MaxSecondaryLTSearchProfilesCount": 3}},
    }

    number_of_failed_requests = 0
    clients_infos = get_clients_infos()
    stats = defaultdict(int)
    c = Counter()
    for client, expected_keywords in clients_infos.items():
        try:
            ids = {"bigb-uid": bigb_uid}
            if keywords_data_pb2.EKeyword.KW_CURRENT_SEARCH_QUERY in expected_keywords:
                ids["search-query"] = "купить гараж"

            if (
                keywords_data_pb2.EKeyword.KW_LT_SEARCH_PROFILE in expected_keywords
                or keywords_data_pb2.EKeyword.KW_LT_ADV_PROFILE in expected_keywords
            ):
                with test_environment_with_kv_saas.kv_saas_server(
                    data={
                        first_icookie_key: b'2\0\0\0\0\0\0\0(\xB5/\xFD 2u\1\0t\2*0\x12.\x1A,\n\x12"\x10http://yandex.ruxednay.ru\x12\28\1\1\0\2361\xC7',
                        second_icookie_key: b',\0\0\0\0\0\0\0(\xB5/\xFD ,E\1\0\x14\2**\x12(\x1A&\n\x0F"\rhttp://abc.rudef.ru\x12\28\1\1\0\xC9\x98M',
                    },
                    send_empty_response=True,
                ):
                    result = test_environment_with_kv_saas.apphost_request(
                        client=client, ids=ids, test_time=TS, glue_type=GlueType.VULTURE_CRYPTA, exp_json=exp_json
                    )
            else:
                result = test_environment_with_kv_saas.request(
                    client=client, ids=ids, test_time=TS, glue_type=GlueType.VULTURE_CRYPTA, exp_json=exp_json
                )

        except AssertionError:
            print(f"Error during request to {client}\n", file=sys.stderr)
            number_of_failed_requests += 1
            continue

        data = result.answer
        ans = py_testlib.TBigbPublicProto(data).to_dict()

        response_keywords = []
        for item in ans["items"]:
            response_keywords.append(item["keyword_id"])
        for k, v in ans.items():
            if k not in ["current_search_query", "user_identifiers"] and v:
                response_keywords.extend(name_to_id[k])
        # current_search_query
        if len(ans["current_search_query"]["query_text"]) > 0:
            response_keywords.append(keywords_data_pb2.EKeyword.KW_CURRENT_SEARCH_QUERY)
        # user_identifiers
        if len(ans["user_identifiers"]["InDeviceGaid"]) > 0:
            response_keywords.append(keywords_data_pb2.EKeyword.KW_INDEVICE_IDENTIFIERS)

        response_keywords = set(response_keywords)
        expected_keywords = set(expected_keywords)

        # т.к. KI_EXCEPT_APPS_ON_CPI(395) и KI_INSTALLED_MOBILE_APPS(541) - 2 киворда значащих одно и то же (application),
        # то наличие хотя бы одного из них = наличие 395
        if 541 in response_keywords:
            response_keywords.remove(541)
            response_keywords.add(395)
        if 541 in expected_keywords:
            expected_keywords.remove(541)
            expected_keywords.add(395)

        IGNORE_KEYWORDS = [
            -1,  # special value for useritems and createtime
            0,
            17,
            211,
            313,  # from KeywordSet (popular_apps.cpp)
        ]
        for ignore in IGNORE_KEYWORDS:
            if ignore in response_keywords:
                response_keywords.remove(ignore)
            if ignore in expected_keywords:
                expected_keywords.remove(ignore)

        # because of illegal access
        if client == "so" and keywords_data_pb2.EKeyword.KW_VISIT_GOAL not in response_keywords:
            response_keywords.add(keywords_data_pb2.EKeyword.KW_VISIT_GOAL)

        for kw in response_keywords:
            stats[kw] += 1

        if response_keywords != expected_keywords:
            number_of_failed_requests += 1
            print(file=sys.stderr)
            print(client, file=sys.stderr)
            print(ans, file=sys.stderr)
            print(file=sys.stderr)

            print(expected_keywords, file=sys.stderr)
            print(response_keywords, file=sys.stderr)
            for j in list(set(expected_keywords) - set(response_keywords)):
                c[j] += 1
            print(f"Only expected {set(expected_keywords) - set(response_keywords)}", file=sys.stderr)
            print(f"Only response {set(response_keywords) - set(expected_keywords)}", file=sys.stderr)
            print(file=sys.stderr)

    assert number_of_failed_requests == 0
