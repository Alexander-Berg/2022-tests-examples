from ads.bsyeti.tests.eagle.ft.lib.test_environment import GlueType, check_answer


def test_glued_uniqs(test_environment):
    id1 = test_environment.new_uid()
    test_environment.profiles.add(
        {
            "y{_id}".format(_id=id1): {},
        }
    )

    vulture_rows = {
        "y{_id}".format(_id=id1): {
            "KeyRecord": {"user_id": "{_id}".format(_id=id1), "id_type": 1},
            "ValueRecords": [
                {"user_id": "y123450000606001", "id_type": 1, "crypta_graph_distance": 1},
                {"user_id": "y123450000606002", "id_type": 1, "crypta_graph_distance": 2},
                {"user_id": "y123450000606003", "id_type": 1, "crypta_graph_distance": 3},
                {"user_id": "y123450000606004", "id_type": 1, "crypta_graph_distance": 4},
                {"user_id": "y123450000606005", "id_type": 1, "crypta_graph_distance": 5},
                {"user_id": "y123450000606006", "id_type": 1, "crypta_graph_distance": 6},
                {"user_id": "y1234500006066007", "id_type": 1, "crypta_graph_distance": 7},
                {"user_id": "y123450000606008", "id_type": 1, "crypta_graph_distance": 8},
                {"user_id": "y123450000606009", "id_type": 1, "crypta_graph_distance": 9},
                {"user_id": "y123450000606010", "id_type": 1, "crypta_graph_distance": 10},
                {"user_id": "y123450000606011", "id_type": 1, "crypta_graph_distance": 11},
                {"user_id": "y123450000606012", "id_type": 1, "crypta_graph_distance": 12},
                {"user_id": "y123450000606013", "id_type": 1, "crypta_graph_distance": 13},
                {"user_id": "y123450000606014", "id_type": 1, "crypta_graph_distance": 14},
                {"user_id": "y123450000606015", "id_type": 1, "crypta_graph_distance": 15},
                {"user_id": "y123450000606016", "id_type": 1, "crypta_graph_distance": 16},
                {"user_id": "y123450000606017", "id_type": 1, "crypta_graph_distance": 17},
                {"user_id": "y123450000606018", "id_type": 1, "crypta_graph_distance": 18},
                {"user_id": "y123450000606019", "id_type": 1, "crypta_graph_distance": 19},
                {"user_id": "y123450000606020", "id_type": 1, "crypta_graph_distance": 20},
                {"user_id": "y123450000606021", "id_type": 1, "crypta_graph_distance": 21},
                {"user_id": "y123450000606022", "id_type": 1, "crypta_graph_distance": 22},
                {"user_id": "y123450000606023", "id_type": 1, "crypta_graph_distance": 23},
                {"user_id": "y123450000606024", "id_type": 1, "crypta_graph_distance": 24},
                {"user_id": "y123450000609025", "id_type": 1, "crypta_graph_distance": 25},
                {"user_id": "mmdi/000", "id_type": 10, "crypta_graph_distance": 777},
                {"user_id": "mmdi/001", "id_type": 10, "crypta_graph_distance": 534},
                {"user_id": "y777075", "id_type": 2, "crypta_graph_distance": 123},
                {"user_id": "y777076", "id_type": 3, "crypta_graph_distance": 142},
                {"user_id": "y777077", "id_type": 4, "crypta_graph_distance": 43},
                {"user_id": "p777000", "id_type": 6, "crypta_graph_distance": 22},
                {"user_id": "gaid/777001", "id_type": 8, "crypta_graph_distance": 329},
                {"user_id": "idfa/777002", "id_type": 9, "crypta_graph_distance": 340},
                {"user_id": "oaid/777003", "id_type": 15, "crypta_graph_distance": 342},
                {"user_id": "duid/777004", "id_type": 13, "crypta_graph_distance": 345},
            ],
        }
    }
    test_environment.vulture.add(vulture_rows, sync=True)

    to_check_profiles = sorted(
        vulture_rows["y{_id}".format(_id=id1)]["ValueRecords"] + [vulture_rows["y{_id}".format(_id=id1)]["KeyRecord"]],
        key=lambda x: tuple([x["id_type"], x["user_id"]]),
    )

    result = test_environment.request(
        client="debug",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
    )
    check_answer(
        {"glued_uniqs": to_check_profiles},
        result.answer,
        ignored_fields=["items", "source_uniqs", "tsar_vectors", "search_pers_profiles"],
    )

    result = test_environment.request(
        client="zen", ids={"bigb-uid": id1}, test_time=1500000000, glue_type=GlueType.VULTURE_CRYPTA
    )
    check_answer(
        {"glued_uniqs": to_check_profiles},
        result.answer,
        ignored_fields=["items", "source_uniqs", "tsar_vectors", "search_pers_profiles"],
    )

    result = test_environment.request(
        client="yabs",
        ids={"bigb-uid": id1},
        test_time=1500000000,
        glue_type=GlueType.VULTURE_CRYPTA,
    )
    check_answer(
        {
            "glued_uniqs": to_check_profiles,
        },
        result.answer,
        ignored_fields=["items", "source_uniqs", "tsar_vectors", "search_pers_profiles"],
    )
