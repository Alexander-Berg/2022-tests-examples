import pytest

from staff.departments.controllers.tickets import PersonTicket


@pytest.mark.django_db
def test_personal_ticket_fields_on_proposal2_for_dep1_person(company, proposal2: str):
    person = company.persons['dep1-person']
    login = person.login
    personal_ticket = PersonTicket.from_proposal_id(proposal_id=proposal2, person_login=login)

    assert personal_ticket.get_ticket_type() == 'position'
    expected_summary = f'Изменение должности и организации: {person.first_name} {person.last_name}, {login}@'
    assert personal_ticket.get_summary() == expected_summary
    assert personal_ticket.get_assignee() == 'dep2-person'

    assert personal_ticket.get_followers() == []
    assert personal_ticket.get_accessors() == ['yandex-chief']
    assert personal_ticket.get_tags() == []
    components = personal_ticket.get_components()
    assert components == ['Тэг1']
    assert personal_ticket.get_versions() == ['Москва']

    assert personal_ticket.get_unique() == f'proposal/{proposal2}/{login}'

    personal_ticket_params = personal_ticket.generate_personal_ticket_params()
    assert personal_ticket_params['raiseDate'] == personal_ticket.context.proposal_object['apply_at'].isoformat()

    assert personal_ticket_params['toCreate'] == ''
    assert personal_ticket_params['toMove'] == ''
    assert personal_ticket_params['toDelete'] == ''

    assert personal_ticket_params['employees'] == [login]
    assert set(personal_ticket_params['allHead']) == {'dep1-chief', 'yandex-chief'}
    assert personal_ticket_params['currentHead'] == 'dep1-chief'
    assert set(personal_ticket_params['analytics']) == {'dep2-person'}
    assert personal_ticket_params['analyst'] == 'dep2-person'
    assert personal_ticket_params['budgetHolder'] == 'yandex-chief'
    assert personal_ticket_params['newPosition'] == 'Уборщик'

    assert personal_ticket_params['department'] == 'Яндекс → Главный бизнес-юнит'
    assert personal_ticket_params['currentDepartment'] == 'Яндекс → Главный бизнес-юнит'
    assert personal_ticket_params['hr'] is None

    assert personal_ticket_params['salarySystem'] is None
    assert personal_ticket_params['newOffice'] is None
    assert personal_ticket_params['legalEntity2'] == company.organizations['yandex_tech'].st_translation_id


@pytest.mark.django_db
def test_personal_ticket_fields_on_proposal2_for_dep2_person(company, proposal2: str):
    person = company.persons['dep2-person']
    login = person.login
    personal_ticket = PersonTicket.from_proposal_id(proposal_id=proposal2, person_login=login)

    assert personal_ticket.get_ticket_type() == 'position'
    assert personal_ticket.get_summary() == f'Изменение должности: {person.first_name} {person.last_name}, {login}@'
    assert personal_ticket.get_assignee() == 'yandex-chief'

    assert personal_ticket.get_followers() == ['yandex-person', 'dep12-chief']
    assert personal_ticket.get_accessors() == ['yandex-person', 'dep12-chief']
    assert personal_ticket.get_tags() == []
    components = personal_ticket.get_components()
    assert components == ['Тэг2']
    assert personal_ticket.get_versions() == ['Москва']

    assert personal_ticket.get_unique() == f'proposal/{proposal2}/{login}'

    personal_ticket_params = personal_ticket.generate_personal_ticket_params()
    assert personal_ticket_params['raiseDate'] == personal_ticket.context.proposal_object['apply_at'].isoformat()

    assert personal_ticket_params['toCreate'] == ''
    assert personal_ticket_params['toMove'] == ''
    assert personal_ticket_params['toDelete'] == ''

    assert personal_ticket_params['employees'] == [login]
    assert set(personal_ticket_params['allHead']) == {'yandex-chief', 'dep2-chief'}
    assert personal_ticket_params['currentHead'] == 'dep2-chief'
    assert set(personal_ticket_params['analytics']) == {'yandex-chief'}
    assert personal_ticket_params['analyst'] == 'yandex-chief'
    assert personal_ticket_params['budgetHolder'] == 'yandex-person'
    assert personal_ticket_params['newPosition'] == 'Уборщик'

    assert personal_ticket_params['department'] == 'Яндекс → Бизнес юниты'
    assert personal_ticket_params['currentDepartment'] == 'Яндекс → Бизнес юниты'
    assert personal_ticket_params['hr'] is None

    assert personal_ticket_params['salarySystem'] is None
    assert personal_ticket_params['newOffice'] is None
    assert personal_ticket_params['legalEntity2'] is None


@pytest.mark.django_db
def test_personal_ticket_fields_on_proposal2_for_dep11_person(company, proposal2: str):
    person = company.persons['dep11-person']
    login = person.login
    personal_ticket = PersonTicket.from_proposal_id(proposal_id=proposal2, person_login=login)

    assert personal_ticket.get_ticket_type() == 'money'
    expected_summary = f'Изменение зарплаты и должности: {person.first_name} {person.last_name}, {login}@'
    assert personal_ticket.get_summary() == expected_summary
    assert personal_ticket.get_assignee() == 'dep2-person'

    assert personal_ticket.get_followers() == []
    assert personal_ticket.get_accessors() == ['yandex-chief']
    assert personal_ticket.get_tags() == []
    components = personal_ticket.get_components()
    assert components == ['Тэг1']
    assert personal_ticket.get_versions() == ['Москва']

    assert personal_ticket.get_unique() == f'proposal/{proposal2}/{login}'

    personal_ticket_params = personal_ticket.generate_personal_ticket_params()
    assert personal_ticket_params['raiseDate'] == personal_ticket.context.proposal_object['apply_at'].isoformat()

    assert personal_ticket_params['toCreate'] == ''
    assert personal_ticket_params['toMove'] == ''
    assert personal_ticket_params['toDelete'] == ''

    assert personal_ticket_params['employees'] == [login]
    assert personal_ticket_params['allHead'] == ['yandex-chief', 'dep1-chief', 'dep11-chief']
    assert personal_ticket_params['currentHead'] == 'dep11-chief'
    assert set(personal_ticket_params['analytics']) == {'dep2-person', 'dep2-person'}
    assert personal_ticket_params['analyst'] == 'dep2-person'
    assert personal_ticket_params['budgetHolder'] == 'yandex-chief'
    assert personal_ticket_params['newPosition'] == 'Уборщик'

    assert personal_ticket_params['department'] == 'Яндекс → Главный бизнес-юнит → dep11'
    assert personal_ticket_params['currentDepartment'] == 'Яндекс → Главный бизнес-юнит → dep11'
    assert personal_ticket_params['hr'] is None

    assert personal_ticket_params['salarySystem'] == 'Почасовая'
    assert personal_ticket_params['newOffice'] is None
    assert personal_ticket_params['legalEntity2'] is None


@pytest.mark.django_db
def test_personal_ticket_fields_on_proposal2_for_dep12_person(company, proposal2: str):
    person = company.persons['dep12-person']
    login = person.login
    personal_ticket = PersonTicket.from_proposal_id(proposal_id=proposal2, person_login=login)

    assert personal_ticket.get_ticket_type() == 'moving'
    expected_summary = f'Изменение должности и офиса: {person.first_name} {person.last_name}, {login}@'
    assert personal_ticket.get_summary() == expected_summary
    assert personal_ticket.get_assignee() == 'dep111-person'

    assert personal_ticket.get_followers() == []
    assert personal_ticket.get_accessors() == ['yandex-chief']
    assert personal_ticket.get_tags() == []
    components = personal_ticket.get_components()
    assert components == ['Тэг3', 'Тэг1']
    assert personal_ticket.get_versions() == ['Минск']

    assert personal_ticket.get_unique() == f'proposal/{proposal2}/{login}'

    personal_ticket_params = personal_ticket.generate_personal_ticket_params()
    assert personal_ticket_params['raiseDate'] == personal_ticket.context.proposal_object['apply_at'].isoformat()

    assert personal_ticket_params['toCreate'] == ''
    assert personal_ticket_params['toMove'] == ''
    assert personal_ticket_params['toDelete'] == ''

    assert personal_ticket_params['employees'] == [login]
    assert personal_ticket_params['allHead'] == ['yandex-chief', 'dep1-chief', 'dep12-chief']
    assert personal_ticket_params['currentHead'] == 'dep12-chief'
    assert set(personal_ticket_params['analytics']) == {'dep111-person'}
    assert personal_ticket_params['analyst'] == 'dep111-person'
    assert personal_ticket_params['budgetHolder'] == 'yandex-chief'
    assert personal_ticket_params['newPosition'] == 'Уборщик'

    assert personal_ticket_params['department'] == 'Яндекс → Главный бизнес-юнит → dep12'
    assert personal_ticket_params['currentDepartment'] == 'Яндекс → Главный бизнес-юнит → dep12'
    assert personal_ticket_params['hr'] is None

    assert personal_ticket_params['salarySystem'] is None
    assert personal_ticket_params['newOffice'] == company.offices['MRP'].id
    assert personal_ticket_params['legalEntity2'] is None
