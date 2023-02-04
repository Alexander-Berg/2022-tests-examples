import pytest

from unittest.mock import patch



@pytest.mark.parametrize('heads, person_login, country_to, city_to, purposeone, is_auto_approved', (
    # руководитель + сотрудник + страна
    (['roman-volodin', 'top'], 'izapolsky', 'Беларусь', '', '', True),
    (['roman-volodin', 'top'], 'vlad-kuz', 'Беларусь', '', '', False),
    # руководитель + страна
    (['veral', 'top'], '', 'Россия', '', '', True),
    (['veral', 'top'], '', 'США', '', '', False),
    # руководитель + сотрудник + страна + цель
    (
        ['dvshelehov', 'top'],
        'elinakiyko',
        'Россия',
        '',
        'Деловые переговоры с внешними партнерами',
        True,
    ),
    (['dvshelehov', 'top'], 'shikarina', 'Belarus', '', 'Внутренняя конференция Яндекса', True),
    (['dvshelehov', 'top'], 'vlad-kuz', 'Россия', '', 'Обучение', False),
    # руководитель + страна + цель
    (['smoroz', 'top'], '', 'Россия', '', 'Рабочая встреча с коллегами', True),
    (['smoroz', 'top'], '', 'США', '', 'Рабочая встреча с коллегами', False),
    (['smoroz', 'top'], '', 'Россия', '', 'Обучение', False),
    # руководитель + город
    (['dmkurilov', 'top'], '', '', 'Москва', '', True),
    (['dmkurilov', 'top'], '', '', 'Воронеж', '', False),
    # руководитель + сотрудник + город
    (['ko4evnik', 'top'], 'edragun', '', 'Москва', '', True),
    (['ko4evnik', 'top'], 'edragun', '', 'Новосибирск', '', True),
    (['ko4evnik', 'top'], 'edragun', '', 'Воронеж', '', False),
    # руководитель + сотрудник + город + цель
    (['glebkhol', 'top'], 'kostya-k', '', 'Новосибирск', 'Рабочая встреча с коллегами', True),
    (['glebkhol', 'top'], 'qkrorlqr', '', 'Yekaterinburg', 'Рабочая встреча с коллегами', True),
    (['glebkhol', 'top'], 'qkrorlqr', '', 'Воронеж', 'Рабочая встреча с коллегами', False),
    (['glebkhol', 'top'], 'kostya-k', '', 'Yekaterinburg', 'Обучение', False),
))


@patch('src.trip.main.set_approvement_field')
def test_auto_approve_rules(mocked_set_approvement_field,
                            heads, person_login, country_to, city_to,
                            purposeone, is_auto_approved):
    with patch('src.trip.main.department_chain_of_heads', lambda *x, **y: heads):
        from src.trip.main import main
        main({
            'author_login': person_login,
            'country_to': country_to,
            'city_to': city_to,
            'purposeone': purposeone,
        })
    assert mocked_set_approvement_field.called is is_auto_approved
    if is_auto_approved:
        mocked_set_approvement_field.assert_called_once_with(
            approve_if_no_approvers=True,
            text='Командировка утверждена автоматически',
        )
