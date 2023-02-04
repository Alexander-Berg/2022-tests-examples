def test_only_position_case():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "imp",
        "person_actions": [
            {
                "login": "imp",
                "sections": ["position"],
                "position": {"new_position": "Imperator", "position_legal": "Boss"}
            },
        ]
    }

    assert main(params) == ['hrbp2', 'head']


def test_only_org_case():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "imp",
        "person_actions": [
            {
                "login": "imp",
                "sections": ["organization"],
                # "position": {"new_position": "Imperator", "position_legal": "Boss"},
                "organization": {"organization": 67}
            },
        ]
    }

    assert main(params) == ['hrbp2']


def test_only_salary_case():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "imp",
        "person_actions": [
            {
                "login": "imp",
                "sections": ["position"],
                # "position": {"new_position": "Imperator", "position_legal": "Boss"},
                # "organization": {"organization": 67}
                "salary": {"new_currency": "RUB",
                           "new_rate": "1",
                           "new_salary": "200",
                           "new_wage_system": "fixed",
                           "old_currency": "RUB",
                           "old_rate": "0.1",
                           "old_salary": "100",
                           "old_wage_system": "fixed"}
            },
        ]
    }

    assert main(params) == ['hrbp2']


def test_only_rate_case():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "imp",
        "person_actions": [
            {
                "login": "imp",
                "sections": ["salary"],
                # "position": {"new_position": "Imperator", "position_legal": "Boss"},
                # "organization": {"organization": 67}
                "salary": {"new_currency": "RUB",
                           "new_rate": "1",
                           "new_salary": "200",
                           "new_wage_system": "fixed",
                           "old_currency": "RUB",
                           "old_rate": "0.5",
                           "old_salary": "100",
                           "old_wage_system": "fixed"}
            },
        ]
    }

    assert main(params) == ['hrbp2']


def test_only_office_case():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "imp",
        "person_actions": [
            {
                "login": "imp",
                "sections": ["position"],
                # "position": {"new_position": "Imperator", "position_legal": "Boss"},
                # "organization": {"organization": 67}
                "office": {"office": 147}
            },
        ]
    }

    assert main(params) == []


def test_maternity_case():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "imp",
        "person_actions": [
            {
                "login": "imp",
                "sections": ["department"],
                "department": {"changing_duties": False,
                               "department": "outstaff_2289_dep08975_dep54476",
                               "fake_department": "",
                               "from_maternity_leave": True,
                               "service_groups": ["svc_mobilemetro"]
                    , "vacancy_url": "https://st.yandex-team.ru/JOB-66969",
                               "with_budget": True},
            },
        ]
    }

    assert main(params) == ['hrbp2']


def test_WITH_BUDGET_IN_BG_case():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "imp",
        "person_actions": [
            {
                "login": "imp",
                "sections": ["position"],
                "department": {"changing_duties": False,
                               "department": "outstaff_2289_dep08975",
                               "fake_department": "",
                               "from_maternity_leave": False,
                               "service_groups": ["svc_mobilemetro"]
                    , "vacancy_url": "https://st.yandex-team.ru/JOB-66969",
                               "with_budget": True},
            },
        ]
    }

    assert main(params) == ['head1', 'hrbp2', 'head1', 'hrbp-only1']


def test_WO_BUDGET_IN_BG_case():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "imp",
        "person_actions": [
            {
                "login": "imp",
                "sections": ["position"],
                "department": {"changing_duties": False,
                               "department": "outstaff_2289_dep08975",
                               "fake_department": "",
                               "from_maternity_leave": False,
                               "service_groups": ["svc_mobilemetro"]
                    , "vacancy_url": "https://st.yandex-team.ru/JOB-66969",
                               "with_budget": False},
            },
        ]
    }

    assert main(params) == ['head', 'head1']


def test_from_bg_wo_case():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "imp",
        "person_actions": [
            {
                "login": "imp",
                "sections": ["position"],
                "department": {
                    "changing_duties": False,
                    "department": "outstaff_9036_2140_3583_dep77726",
                    "fake_department": "",
                    "from_maternity_leave": False,
                    "service_groups": ["svc_mobilemetro"],
                    "vacancy_url": "https://st.yandex-team.ru/JOB-66969",
                    "with_budget": False,
                },
            },
        ]
    }

    assert main(params) == ['head', 'zen_head']


def test_FROM_INTERN_case():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "intern",
        "person_actions": [
            {
                "login": "intern",
                "sections": ["position"],
                "department": {"changing_duties": False,
                               "department": "outstaff_2289_dep08975_dep54476",
                               "fake_department": "",
                               "from_maternity_leave": False,
                               "service_groups": ["svc_mobilemetro"]
                    , "vacancy_url": "https://st.yandex-team.ru/JOB-66969",
                               "with_budget": False},
            },
        ]
    }

    assert main(params) == ['hrbp2-int']


def test_FROM_INTERN_AND_POSITION_case():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "intern",
        "person_actions": [
            {
                "login": "intern",
                "sections": ["position", 'department'],
                "position": {"new_position": "Imperator", "position_legal": "Boss"},
                "department": {"changing_duties": False,
                               "department": "outstaff_2289_dep08975_dep54476",
                               "fake_department": "",
                               "from_maternity_leave": False,
                               "service_groups": ["svc_mobilemetro"]
                    , "vacancy_url": "https://st.yandex-team.ru/JOB-66969",
                               "with_budget": False},
            },
        ]
    }

    assert main(params) == ['hrbp2-int']


def test_MATERNITY_AND_POSITION_case():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "imp",
        "person_actions": [
            {
                "login": "imp",
                "sections": ["position", 'department'],
                "position": {"new_position": "Imperator", "position_legal": "Boss"},
                "department": {"changing_duties": False,
                               "department": "outstaff_2289_dep08975_dep54476",
                               "fake_department": "",
                               "from_maternity_leave": True,
                               "service_groups": ["svc_mobilemetro"]
                    , "vacancy_url": "https://st.yandex-team.ru/JOB-66969",
                               "with_budget": False},
            },
        ]
    }

    assert main(params) == ['hrbp2']


def test_position_organisation_rate_case():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "imp",
        "person_actions": [
            {
                "login": "imp",
                "sections": ["salary", "position", "organization"],
                "position": {"new_position": "Imperator", "position_legal": "Boss"},
                "organization": {"organization": 67},
                "salary": {"new_currency": "RUB",
                           "new_rate": "1",
                           "new_salary": "200",
                           "new_wage_system": "fixed",
                           "old_currency": "RUB",
                           "old_rate": "0.5",
                           "old_salary": "100",
                           "old_wage_system": "fixed"}
            },
        ]
    }

    assert main(params) == ['hrbp2', 'hrbp2', 'head', 'hrbp2']


def test_transfer_and_position_and_first_common_head():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "imp",
        "person_actions": [
            {
                "login": "imp",
                "sections": ["position", "department"],
                "position": {"new_position": "Imperator", "position_legal": "Boss"},
                "department": {"changing_duties": False,
                               "department": "outstaff_2289_dep08975",
                               "fake_department": "",
                               "from_maternity_leave": False,
                               "service_groups": ["svc_mobilemetro"]
                    , "vacancy_url": "https://st.yandex-team.ru/JOB-66969",
                               "with_budget": True},
            },
        ]
    }

    assert main(params) == ['head1', 'hrbp2', 'head1', 'hrbp-only1']


def test_change_branch_wo_bud():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "imp",
        "person_actions": [
            {
                "login": "imp",
                "sections": ["position", "department"],
                "position": {"new_position": "Imperator", "position_legal": "Boss"},
                "department": {"changing_duties": False,
                               "department": "yandex_edu_analytics_bo_1991",
                               "fake_department": "",
                               "from_maternity_leave": False,
                               "service_groups": ["svc_mobilemetro"]
                    , "vacancy_url": "https://st.yandex-team.ru/JOB-66969",
                               "with_budget": False},
            },
        ]
    }

    assert main(params) == ['hrbp2']


def test_kpb_portal_position():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "portal_kpb",
        "person_actions": [
            {
                "login": "portal_kpb",
                "sections": ["position"],
                "position": {"new_position": "Imperator", "position_legal": "Boss"}
            },
        ]
    }

    assert main(params) == ['osuvorova']


def test_kpb_portal_salary():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "portal_kpb",
        "person_actions": [
            {
                "login": "portal_kpb",
                "sections": ["salary"],
                "salary": {"new_currency": "RUB",
                           "new_rate": "1",
                           "new_salary": "200",
                           "new_wage_system": "piecework",
                           "old_currency": "RUB",
                           "old_rate": "0.1",
                           "old_salary": "100",
                           "old_wage_system": "fixed"}
            },
        ]
    }

    assert main(params) == ['osuvorova', 'anna-ti']


def test_kpb_portal_salary_piecework():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "portal_kpb",
        "person_actions": [
            {
                "login": "portal_kpb",
                "sections": ["salary"],
                "salary": {"new_currency": "RUB",
                           "new_rate": "1",
                           "new_salary": "200",
                           "new_wage_system": "piecework",
                           "old_currency": "RUB",
                           "old_rate": "0.1",
                           "old_salary": "100",
                           "old_wage_system": "piecework"}
            },
        ]
    }

    assert main(params) == []


def test_kpb_media_position_organisation_rate_case():
    from src.proposal.main import main

    params = {
        "ticket_type": "personal",
        "ticket_person": "dddenisova",
        "person_actions": [
            {
                "login": "dddenisova",
                "sections": ["salary", "position", "organization"],
                "position": {"new_position": "Imperator", "position_legal": "Boss"},
                "organization": {"organization": 67},
                "salary": {"new_currency": "RUB",
                           "new_rate": "1",
                           "new_salary": "200",
                           "new_wage_system": "fixed",
                           "old_currency": "RUB",
                           "old_rate": "0.5",
                           "old_salary": "100",
                           "old_wage_system": "fixed"}
            },
        ]
    }

    assert main(params) == ['shurochka', 'shurochka', 'shurochka']
