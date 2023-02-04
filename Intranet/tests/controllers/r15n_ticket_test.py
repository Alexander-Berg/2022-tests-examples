import pytest

from staff.departments.controllers.tickets.restructurisation import RestructurisationTicket


@pytest.fixture(autouse=True)
def mock_get_components(monkeypatch):

    def get_component_id_by_name_mocked(self, component_name: str) -> int:
        return sum(ord(g) for g in component_name) * len(component_name)

    monkeypatch.setattr(
        RestructurisationTicket,
        'get_component_id_by_name',
        get_component_id_by_name_mocked,
    )


@pytest.mark.django_db
def test_r15_ticket_fields_on_proposal_1(company, proposal1: str, mocked_mongo):
    r15n_ticket = RestructurisationTicket.from_proposal_id(proposal1)

    assert r15n_ticket.logins == set()

    assert r15n_ticket.get_summary() == 'Заявка на реструктуризацию'
    assert r15n_ticket.get_unique() == f'proposal/{proposal1}/restructurisation'

    # Исполнитель yandex-chief а не dep2-person,
    # т.к. согласование изменений по dep11-person уходит в кадровый тикет, а не в реструктуризацию,
    # и соответственно, аттрибут его подразделения не выстрелит и будет взят дефолтный
    assert r15n_ticket.get_assignee() == 'yandex-chief'

    # По той же причине его hr-партнёр не попадёт ни в наблюдатели ни в доступы ни в аналитики
    assert set(r15n_ticket.get_followers()) == {'yandex-person', 'dep12-chief', 'dep2-hr-partner'}
    assert set(r15n_ticket.get_accessors()) == {'dep2-hr-partner', 'dep1-chief'}

    assert r15n_ticket.analysts == {'yandex-chief'}

    # А компонента берётся только та, что относится к аттрибуту создаваемого подразделения.
    components, _ = r15n_ticket.get_components()
    assert components == [13136]  # из замоканной get_component_id_by_name_mocked выше

    ticket_params, _ = r15n_ticket.generate_ticket_params()
    assert ticket_params['staffDate'] == r15n_ticket.context.proposal_object['apply_at'].isoformat()
    assert ticket_params['toCreate'] == r15n_ticket.context.proposal_object['actions'][0]['name']['name']
    assert ticket_params['toMove'] == ''
    assert ticket_params['toDelete'] == ''


@pytest.mark.django_db
def test_r15_ticket_fields_on_proposal_3(company, proposal3: str, mocked_mongo):
    r15n_ticket = RestructurisationTicket.from_proposal_id(proposal3)

    assert r15n_ticket.logins == {'dep1-person', 'dep11-person', 'dep111-chief', 'dep12-person', 'dep111-person'}

    assert r15n_ticket.get_summary() == 'Заявка на реструктуризацию'
    assert r15n_ticket.get_unique() == f'proposal/{proposal3}/restructurisation'

    assert r15n_ticket.get_assignee() == 'yandex-person'
    assert set(r15n_ticket.get_followers()) == set()
    assert set(r15n_ticket.get_accessors()) == set()
    components, _ = r15n_ticket.get_components()
    assert components == []
    assert r15n_ticket.analysts == {'dep2-person', 'dep111-person', 'yandex-chief'}

    ticket_params, _ = r15n_ticket.generate_ticket_params()
    assert ticket_params['staffDate'] == r15n_ticket.context.proposal_object['apply_at'].isoformat()
    assert ticket_params['toCreate'] == ''
    assert ticket_params['toMove'] == ''
    assert ticket_params['toDelete'] == ''


@pytest.mark.django_db
def test_r15_ticket_fields_on_proposal_4(company, proposal4: str, mocked_mongo):
    r15n_ticket = RestructurisationTicket.from_proposal_id(proposal4)

    assert r15n_ticket.logins == {'dep1-chief', 'dep11-person', 'dep12-person'}

    assert r15n_ticket.get_summary() == 'Заявка на реструктуризацию'
    assert r15n_ticket.get_unique() == f'proposal/{proposal4}/restructurisation'

    assert r15n_ticket.get_assignee() == 'yandex-person'
    assert set(r15n_ticket.get_followers()) == set()
    assert set(r15n_ticket.get_accessors()) == set()
    components, _ = r15n_ticket.get_components()
    assert components == []
    assert r15n_ticket.analysts == {'dep2-person', 'dep111-person', 'yandex-chief'}

    ticket_params, _ = r15n_ticket.generate_ticket_params()
    assert ticket_params['staffDate'] == r15n_ticket.context.proposal_object['apply_at'].isoformat()
    assert ticket_params['toCreate'] == 'Новое подразделение'
    assert ticket_params['toMove'] == ''
    assert ticket_params['toDelete'] == ''
