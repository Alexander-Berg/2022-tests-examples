import responses
import datetime
import irt.common.abc_adapter


@responses.activate
def test_abc_adapter():
    responses.add(responses.GET, 'https://abc-back.yandex-team.ru/api/v4/services/?id=1158',
                  json={"count": 1,
                        "next": None,
                        "previous": None,
                        "total_pages": 1,
                        "results": [{"id": 1158, "readonly_state": None, "slug": "IRT", "state": "develop"}]
                        },
                  status=200)

    members_response = {"next": None,
                        "previous": None,
                        "results": [{
                            "id": 28136,
                            "person": {"id": 6862, "login": "sergio", "uid": "112000"},
                            "service": {"id": 1158, "slug": "IRT"},
                            "role": {"id": 8, "scope": {"slug": "development"}, "code": "developer"},
                            "department_member": {"department": {"url": "yandex_monetize_search_research"}}},
                            {"id": 30818,
                             "person": {"id": 22812, "login": "apovetkin", "uid": "112001"},
                             "service": {"id": 1158, "slug": "IRT"},
                             "role": {"id": 8, "scope": {"slug": "development"}, "code": "developer"},
                             "department_member": {"department": {"url": "yandex_monetize_search_research"}}},
                            {"id": 40976,
                             "person": {"id": 22812, "login": "apovetkin", "uid": "112001"},
                             "service": {"id": 1158, "slug": "IRT"},
                             "role": {"id": 16, "scope": {"slug": "administration"}, "code": "other"},
                             "department_member": {"department": {"url": "yandex_monetize_search_research"}}}]}

    responses.add(responses.GET, 'https://abc-back.yandex-team.ru/api/v4/services/members/?service=1158',
                  json=members_response,
                  status=200)
    responses.add(responses.GET, 'https://abc-back.yandex-team.ru/api/v4/services/members/?service__slug=IRT',
                  json=members_response,
                  status=200)

    duty_response = {"results": [
        {"id": 338000,
         "person": {"login": "some-name", "uid": "112002"},
         "schedule": {"id": 1000, "name": "name"},
         "is_approved": True,
         "start": (datetime.date.today() - datetime.timedelta(days=5)).isoformat(),
         "end": (datetime.date.today() - datetime.timedelta(days=3)).isoformat()},
        {"id": 338001,
         "person": {"login": "other-name", "uid": "112003"},
         "schedule": {"id": 1001, "name": "name"},
         "is_approved": True,
         "start": (datetime.date.today() - datetime.timedelta(days=2)).isoformat(),
         "end": (datetime.date.today() + datetime.timedelta(days=1)).isoformat()}
    ]}

    responses.add(responses.GET,
                  'https://abc-back.yandex-team.ru/api/v4/duty/shifts/?date_from={}&date_to={}&service=1158'.format(
                      (datetime.date.today() - datetime.timedelta(days=4)).isoformat(),
                      datetime.date.today().isoformat()
                  ),
                  json=duty_response,
                  status=200)

    responses.add(responses.GET,
                  'https://abc-back.yandex-team.ru/api/v4/duty/shifts/?date_from={}&date_to={}&service__slug=IRT'.format(
                      (datetime.date.today() - datetime.timedelta(days=4)).isoformat(),
                      datetime.date.today().isoformat()
                  ),
                  json=duty_response,
                  status=200)

    abc_adapter = irt.common.abc_adapter.ABCAdapter('')

    assert abc_adapter.get_service_slug(1158) == 'IRT'

    irt_members = abc_adapter.get_members(1158)
    assert len(irt_members) > 0
    assert {x['username']: x for x in irt_members} == {x['username']: x for x in abc_adapter.get_members('IRT')}
    duty = abc_adapter.get_duty(1158, datetime.date.today() - datetime.timedelta(days=4), datetime.date.today())
    duty_slug = abc_adapter.get_duty('IRT', datetime.date.today() - datetime.timedelta(days=4), datetime.date.today())
    assert len(duty) > 0
    assert set(':'.join(d[k] for k in sorted(d.keys())) for d in duty) == set(':'.join(d[k] for k in sorted(d.keys())) for d in duty_slug)
