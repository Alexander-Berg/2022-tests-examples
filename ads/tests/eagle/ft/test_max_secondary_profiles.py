import time

import pytest
from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer


def generate_query(id_, calc_time, source_uniq_index=None, is_main=None):
    query = {
        "query_id": 840000000000000000 + id_,
        "query_text": "query of profile {id_}".format(id_=id_),
        "hits": 1,
        "shows": 1,
        "synced": 1,
        "hours_mask": 128,
        "select_type": 98,
        "collector_path_bits": 131072,
        "predicted_cpc": 0,
        "unix_update_time": calc_time,
        "create_time": calc_time,
        "cat4": 200001097,
        "parent_id": 3070519761759050529,
        "referer_domain_md5": 4906751998774499120,
        "glue_cluster_id": 840000000000000000 + id_,
        "source_bit": 563499709497344,
        "predicted_vw_models_values": {"calc_timestamp": calc_time},
        "predicted_prod_rank": 3.08064,
        "is_new": True,
    }
    if source_uniq_index is not None:
        query["source_uniq_index"] = source_uniq_index
    if is_main is not None:
        query["main"] = is_main
    return query


@pytest.fixture(scope="module")
def profiles(test_environment):
    profile_count = 6  # one main and 5 secondaries
    ids = [test_environment.new_uid() for _ in range(profile_count)]
    calc_time = int(time.time())
    test_environment.profiles.add(
        {
            "y{id_}".format(id_=id_): {
                "UserItems": [{"keyword_id": 235, "update_time": calc_time, "uint_values": [1000 + id_]}],
                "Queries": [generate_query(id_, calc_time)],
            }
            for id_ in ids
        }
    )

    main_id = ids[0]
    test_environment.vulture.add(
        {
            "y{main_id}".format(main_id=main_id): {
                "KeyRecord": {"user_id": "{main_id}".format(main_id=main_id), "id_type": 1},
                "ValueRecords": [
                    {"user_id": "{id_}".format(id_=id_), "id_type": 1, "crypta_graph_distance": ind}
                    for ind, id_ in enumerate(ids)
                    if ind > 0
                ],
            }
        },
        sync=True,
    )
    return {"main_id": main_id, "all_ids": ids, "calc_time": calc_time}


def test_default_max_secondary(test_environment, profiles):
    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "MaxQueryCountBeforeSampling": 0,
                "MaxSecondaryProfilesWithQueriesCount": 0,
                "MaxSecondaryProfiles": 5,
                "ProfileIdTypeLimits": [
                    {
                        "IdType": 1,
                        "Limit": 1000,
                    }
                ],
            }
        }
    }
    calc_time = profiles["calc_time"]
    main_id = profiles["main_id"]
    ids = profiles["all_ids"]

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": main_id},
        test_time=calc_time,
        glue_type=GlueType.VULTURE_CRYPTA,
        exp_json=exp_json,
        keywords=[62, 235, 328, 564, 725],
    )

    sorted_ids = ids[:1] + [int(id_) for id_ in sorted(str(i) for i in ids[1:])]
    items = [
        {
            "keyword_id": 235,
            "update_time": calc_time,
            "uint_values": [1000 + id_],
            "main": (id_ == main_id),
            "source_uniq_index": ind,
        }
        for ind, id_ in enumerate(sorted_ids)
    ] + [{"keyword_id": 564, "update_time": calc_time, "uint_values": [1]}]
    queries = [
        generate_query(id_, calc_time, source_uniq_index=ind, is_main=(id_ == main_id))
        for ind, id_ in enumerate(sorted_ids)
    ]
    # queries[-2]["predicted_ctr"] = 666  # check test works

    check_answer(
        {"items": items, "queries": queries},
        result.answer,
        ignored_fields=["queries.predicted_cpc", "counters", "source_uniqs", "tsar_vectors"],
    )


def test_max_secondary_exceeded(test_environment, profiles):
    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "MaxQueryCountBeforeSampling": 0,
                "MaxSecondaryProfilesWithQueriesCount": 0,
                "MaxSecondaryProfiles": 3,
                "ProfileIdTypeLimits": [
                    {
                        "IdType": 1,
                        "Limit": 1000,
                    }
                ],
            }
        }
    }
    calc_time = profiles["calc_time"]
    main_id = profiles["main_id"]
    ids = profiles["all_ids"][: 1 + 3]

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": main_id},
        test_time=calc_time,
        glue_type=GlueType.VULTURE_CRYPTA,
        exp_json=exp_json,
        keywords=[62, 235, 328, 564, 725],
    )

    sorted_ids = ids[:1] + [int(id_) for id_ in sorted(str(i) for i in ids[1:])]
    items = [
        {
            "keyword_id": 235,
            "update_time": calc_time,
            "uint_values": [1000 + id_],
            "main": (id_ == main_id),
            "source_uniq_index": ind,
        }
        for ind, id_ in enumerate(sorted_ids)
    ] + [{"keyword_id": 564, "update_time": calc_time, "uint_values": [1]}]
    queries = [
        generate_query(id_, calc_time, source_uniq_index=ind, is_main=(id_ == main_id))
        for ind, id_ in enumerate(sorted_ids)
    ]

    check_answer(
        {"items": items, "queries": queries},
        result.answer,
        ignored_fields=["queries.predicted_cpc", "counters", "source_uniqs", "tsar_vectors"],
    )


def test_max_secondary_profiles_with_queries_exceeded(test_environment, profiles):
    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "MaxQueryCountBeforeSampling": 0,
                "MaxSecondaryProfilesWithQueriesCount": 3,
                "MaxSecondaryProfilesWithList": [{"Key": "Queries", "Value": 3}],
                "MaxSecondaryProfiles": 5,
                "ProfileIdTypeLimits": [
                    {
                        "IdType": 1,
                        "Limit": 1000,
                    }
                ],
            }
        }
    }
    calc_time = profiles["calc_time"]
    main_id = profiles["main_id"]
    ids = profiles["all_ids"]
    granted_queries_profiles = set(ids[: 1 + 3])

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": main_id},
        test_time=calc_time,
        glue_type=GlueType.VULTURE_CRYPTA,
        exp_json=exp_json,
        keywords=[62, 235, 328, 564, 725],
    )

    sorted_ids = ids[:1] + [int(id_) for id_ in sorted(str(i) for i in ids[1:])]
    items = [
        {
            "keyword_id": 235,
            "update_time": calc_time,
            "uint_values": [1000 + id_],
            "main": (id_ == main_id),
            "source_uniq_index": ind,
        }
        for ind, id_ in enumerate(sorted_ids)
    ] + [{"keyword_id": 564, "update_time": calc_time, "uint_values": [1]}]
    queries = [
        generate_query(id_, calc_time, source_uniq_index=ind, is_main=(id_ == main_id))
        for ind, id_ in enumerate(sorted_ids)
        if id_ in granted_queries_profiles
    ]

    check_answer(
        {"items": items, "queries": queries},
        result.answer,
        ignored_fields=["queries.predicted_cpc", "counters", "source_uniqs", "tsar_vectors"],
    )


def test_queries_exceeded(test_environment, profiles):
    exp_json = {
        "EagleSettings": {
            "LoadSettings": {
                "MaxQueryCountBeforeSampling": 3,
                "MaxSecondaryProfilesWithQueriesCount": 0,
                "MaxSecondaryProfiles": 5,
                "ProfileIdTypeLimits": [
                    {
                        "IdType": 1,
                        "Limit": 1000,
                    }
                ],
            }
        }
    }
    calc_time = profiles["calc_time"]
    main_id = profiles["main_id"]
    ids = profiles["all_ids"]
    granted_queries_profiles = set(ids[:3])

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": main_id},
        test_time=calc_time,
        glue_type=GlueType.VULTURE_CRYPTA,
        exp_json=exp_json,
        keywords=[62, 235, 328, 564, 725],
    )

    sorted_ids = ids[:1] + [int(id_) for id_ in sorted(str(i) for i in ids[1:])]
    items = [
        {
            "keyword_id": 235,
            "update_time": calc_time,
            "uint_values": [1000 + id_],
            "main": (id_ == main_id),
            "source_uniq_index": ind,
        }
        for ind, id_ in enumerate(sorted_ids)
    ] + [{"keyword_id": 564, "update_time": calc_time, "uint_values": [1]}]
    queries = [
        generate_query(id_, calc_time, source_uniq_index=ind, is_main=(id_ == main_id))
        for ind, id_ in enumerate(sorted_ids)
        if id_ in granted_queries_profiles
    ]

    check_answer(
        {"items": items, "queries": queries},
        result.answer,
        ignored_fields=["queries.predicted_cpc", "counters", "source_uniqs", "tsar_vectors"],
    )


def test_max_secondary_limits_for_clients(test_environment, profiles):
    def subtester_for_secondary_limits(default_limit, expected_count, secondary_override_params):
        exp_json = {
            "EagleSettings": {
                "LoadSettings": {
                    "MaxQueryCountBeforeSampling": 0,
                    "MaxSecondaryProfilesWithQueriesCount": 0,
                    "MaxSecondaryProfiles": default_limit,
                    "ProfileIdTypeLimits": [
                        {
                            "IdType": 1,
                            "Limit": 1000,
                        }
                    ],
                    "MaxSecondaryOverrideParams": secondary_override_params,
                }
            }
        }
        calc_time = profiles["calc_time"]
        main_id = profiles["main_id"]
        ids = profiles["all_ids"][: 1 + expected_count]

        result = test_environment.request(
            client="yabs",
            ids={"bigb-uid": main_id},
            test_time=calc_time,
            glue_type=GlueType.VULTURE_CRYPTA,
            exp_json=exp_json,
            keywords=[62, 235, 328, 564, 725],
        )

        sorted_ids = ids[:1] + [int(id_) for id_ in sorted(str(i) for i in ids[1:])]
        items = [
            {
                "keyword_id": 235,
                "update_time": calc_time,
                "uint_values": [1000 + id_],
                "main": (id_ == main_id),
                "source_uniq_index": ind,
            }
            for ind, id_ in enumerate(sorted_ids)
        ] + [{"keyword_id": 564, "update_time": calc_time, "uint_values": [1]}]
        queries = [
            generate_query(id_, calc_time, source_uniq_index=ind, is_main=(id_ == main_id))
            for ind, id_ in enumerate(sorted_ids)
        ]

        check_answer(
            {"items": items, "queries": queries},
            result.answer,
            ignored_fields=["queries.predicted_cpc", "counters", "source_uniqs", "tsar_vectors"],
        )

    # run subtests
    subtester_for_secondary_limits(
        expected_count=3, default_limit=3, secondary_override_params=[{"ClientName": "other", "Value": 4}]
    )
    subtester_for_secondary_limits(
        expected_count=4, default_limit=3, secondary_override_params=[{"ClientName": "yabs", "Value": 4}]
    )
    subtester_for_secondary_limits(
        expected_count=2, default_limit=3, secondary_override_params=[{"ClientName": "yabs", "Value": 2}]
    )
    subtester_for_secondary_limits(
        expected_count=4,
        default_limit=3,
        secondary_override_params=[{"ClientName": "yabs", "Value": 4}, {"ClientName": "other", "Value": 2}],
    )
    subtester_for_secondary_limits(
        expected_count=2,
        default_limit=3,
        secondary_override_params=[{"ClientName": "other", "Value": 4}, {"ClientName": "yabs", "Value": 2}],
    )
    subtester_for_secondary_limits(
        expected_count=3,
        default_limit=3,
        secondary_override_params=[{"ClientName": "other1", "Value": 4}, {"ClientName": "other2", "Value": 2}],
    )
    subtester_for_secondary_limits(
        expected_count=1,
        default_limit=3,
        secondary_override_params=[
            {"ClientName": "yabs", "Value": 1},
            {"ClientName": "other1", "Value": 2},
            {"ClientName": "other2", "Value": 4},
        ],
    )
