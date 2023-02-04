import pytest

from staff.departments.controllers.tickets.headcount import HeadcountTicket


@pytest.fixture(autouse=True)
def mock_get_components(monkeypatch):

    def get_component_id_by_name_mocked(self, component_name: str) -> int:
        return sum(ord(g) for g in component_name) * len(component_name)

    monkeypatch.setattr(
        HeadcountTicket,
        'get_component_id_by_name',
        get_component_id_by_name_mocked,
    )


@pytest.mark.django_db
def test_headcount_ticket_fields_on_proposal_7(company, proposal7: str, mocked_mongo):
    headcount_ticket = HeadcountTicket.from_proposal_id(proposal7)

    assert headcount_ticket.logins == set()

    assert headcount_ticket.get_summary() == 'Заявка на перемещение БП'
    assert headcount_ticket.get_unique() == f'proposal/{proposal7}/headcount'

    # TODO: Тикет на изменение БП наполняется точно также, как и тикет на реструктуризацию, ок ли это с т.з. доступов?

    # Исполнитель yandex-chief а не dep2-person,
    # т.к. согласование изменений по dep11-person уходит в кадровый тикет, а не в реструктуризацию,
    # и соответственно, аттрибут его подразделения не выстрелит и будет взят дефолтный
    assert headcount_ticket.get_assignee() == 'yandex-chief'

    # По той же причине его hr-партнёр не попадёт ни в наблюдатели ни в доступы ни в аналитики
    assert set(headcount_ticket.get_followers()) == {'yandex-person', 'dep12-chief', 'dep2-hr-partner'}
    assert set(headcount_ticket.get_accessors()) == {'dep2-hr-partner', 'dep1-chief'}

    assert headcount_ticket.analysts == {'yandex-chief'}

    # А компонента берётся только та, что относится к аттрибуту создаваемого подразделения.
    components, _ = headcount_ticket.get_components()
    assert components == [13136]  # из замоканной get_component_id_by_name_mocked выше

    ticket_params, _ = headcount_ticket.generate_ticket_params()
    assert ticket_params['staffDate'] == headcount_ticket.context.proposal_object['apply_at'].isoformat()
    assert ticket_params['toCreate'] == headcount_ticket.context.proposal_object['actions'][0]['name']['name']
    assert ticket_params['toMove'] == ''
    assert ticket_params['toDelete'] == ''
