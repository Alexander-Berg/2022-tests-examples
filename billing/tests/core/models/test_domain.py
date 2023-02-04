import pytest

from mdh.core.exceptions import UserHandledError


def test_basic(init_domain, init_user):

    user = init_user()

    with pytest.raises(UserHandledError) as e:
        init_domain('ерунда', user=user)

    assert "'ерунда' should obey the format" in f'{e.value}'

    domain = init_domain('domain1', user=user)

    domain.title = 'uno'
    assert domain.title == 'uno'

    # Локализация.
    domain.set_title('заголовок', lang='ru')
    domain.set_title('atitle')
    domain.set_hint('ahint')
    domain.save()

    assert domain.titles['ru'] == 'заголовок'
    assert domain.hints['en'] == 'ahint'

    assert domain.get_title(lang='JA') == 'atitle'  # LANG__DEFAULT
    assert domain.get_title(lang='ja', fallback='') == 'atitle'
    assert domain.get_title(lang='rU') == 'заголовок'

    with pytest.raises(ValueError):
        domain.set_title('some', lang='bogus')

    with pytest.raises(ValueError):
        domain.get_title(lang='bogus')

    # Аудит.
    domain.set_title('some', lang='en')
    domain.mark_published()
    domain.save()

    domain.set_title('other', lang='az')
    domain.save()

    audit = list(domain.audit.all())

    assert len(audit) == 4
    assert audit[0].changes == {}

    change = audit[2].changes
    change.pop('dt_upd')
    assert change == {
        'status': [1, 6],
        'titles': [{'en': 'atitle', 'ru': 'заголовок'}, {'en': 'some', 'ru': 'заголовок'}]}

    change = audit[3].changes
    change.pop('dt_upd')
    assert change == {
        'titles': [
            {'ru': 'заголовок', 'en': 'some'},
            {'ru': 'заголовок', 'en': 'some', 'az': 'other'}]}
