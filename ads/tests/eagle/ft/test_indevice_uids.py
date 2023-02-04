from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer


def test_indev_idfa_gaid(test_environment):
    id1 = test_environment.new_uid()
    id2 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{_id}".format(_id=id1): {},
            "y{_id}".format(_id=id2): {},
        }
    )

    InDeviceGaid1 = "6f9619ff-8b86-d011-b42d-00cf4fc964ff"
    InDeviceGaid2 = "6f9619ff-8b86-d011-b42d-00cf4fc96400"
    InDeviceIdfa1 = "AEBE52E7-03EE-455A-B3C4-E57283966239"
    InDeviceIdfa2 = "AEBE52E7-03EE-455A-B3C4-E5728396623A"

    MainIdfa = "AEBE52E7-03EE-455A-B3C4-E57283960001"
    MainGaid = "6f9619ff-8b86-d011-b42d-00cf4fc00001"

    test_environment.vulture.add(
        {
            "y{_id}".format(_id=id1): {
                "KeyRecord": {"user_id": "{_id}".format(_id=id1), "id_type": 1, "is_main": True},
                "ValueRecords": [
                    {
                        "user_id": "00000000-0000-0000-0000-000000000003",
                        "id_type": 8,
                        "crypta_graph_distance": 3,
                    },
                    {
                        "user_id": "00000000-0000-0000-0000-000000000004",
                        "id_type": 9,
                        "crypta_graph_distance": 3,
                    },
                    {
                        "user_id": "00000000-0000-0000-0000-000000000001",
                        "id_type": 8,
                        "crypta_graph_distance": 2,
                        "is_indevice": True,
                    },
                    {
                        "user_id": "00000000-0000-0000-0000-000000000002",
                        "id_type": 9,
                        "crypta_graph_distance": 2,
                        "is_indevice": True,
                    },
                    {
                        "user_id": InDeviceGaid1,
                        "id_type": 8,
                        "crypta_graph_distance": 1,
                        "is_indevice": True,
                    },
                    {
                        "user_id": InDeviceIdfa1,
                        "id_type": 9,
                        "crypta_graph_distance": 1,
                        "is_indevice": True,
                    },
                ],
            },
            "y{_id}".format(_id=id2): {
                "KeyRecord": {"user_id": "{_id}".format(_id=id2), "id_type": 1, "is_main": True},
                "ValueRecords": [
                    {
                        "user_id": "00000000-0000-0000-0000-000000000003",
                        "id_type": 8,
                        "crypta_graph_distance": 1,
                    },
                    {
                        "user_id": "00000000-0000-0000-0000-000000000004",
                        "id_type": 9,
                        "crypta_graph_distance": 1,
                    },
                    {
                        "user_id": "00000000-0000-0000-0000-000000000001",
                        "id_type": 8,
                        "crypta_graph_distance": 2,
                    },
                    {
                        "user_id": "00000000-0000-0000-0000-000000000002",
                        "id_type": 9,
                        "crypta_graph_distance": 2,
                    },
                    {
                        "user_id": InDeviceGaid2,
                        "id_type": 8,
                        "crypta_graph_distance": 3,
                        "is_indevice": True,
                    },
                    {
                        "user_id": InDeviceIdfa2,
                        "id_type": 9,
                        "crypta_graph_distance": 3,
                        "is_indevice": True,
                    },
                ],
            },
            "idfa/{_id}".format(_id=MainIdfa): {
                "KeyRecord": {"user_id": MainIdfa, "id_type": 9, "is_main": True},
                "ValueRecords": [],
            },
            "gaid/{_id}".format(_id=MainGaid): {
                "KeyRecord": {"user_id": MainGaid, "id_type": 8, "is_main": True},
                "ValueRecords": [],
            },
        }
    )

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
    )
    check_answer(
        {
            "user_identifiers": {"InDeviceIdfa": InDeviceIdfa1, "InDeviceGaid": InDeviceGaid1},
        },
        result.answer,
        ignored_fields=["items", "glued_uniqs", "source_uniqs", "tsar_vectors", "search_pers_profiles"],
    )

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id2},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
    )
    check_answer(
        {
            "user_identifiers": {"InDeviceIdfa": InDeviceIdfa2, "InDeviceGaid": InDeviceGaid2},
        },
        result.answer,
        ignored_fields=["items", "glued_uniqs", "source_uniqs", "tsar_vectors", "search_pers_profiles"],
    )

    result = test_environment.request(
        client="yabs",
        ids={"idfa": MainIdfa, "gaid": MainGaid},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
    )
    check_answer(
        {
            "user_identifiers": {"InDeviceIdfa": MainIdfa, "InDeviceGaid": MainGaid},
        },
        result.answer,
        ignored_fields=["items", "glued_uniqs", "source_uniqs", "tsar_vectors", "search_pers_profiles"],
    )

    result = test_environment.request(
        client="yabs",
        ids={"idfa": InDeviceIdfa1, "gaid": InDeviceGaid1},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
    )
    check_answer(
        {"user_identifiers": {"InDeviceIdfa": "", "InDeviceGaid": ""}},
        result.answer,
        ignored_fields=["items", "glued_uniqs", "source_uniqs", "tsar_vectors", "search_pers_profiles"],
    )
