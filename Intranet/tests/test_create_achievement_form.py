import pytest

from staff.achievery.forms.create_achievement import CreateAchievementForm
from staff.achievery.tests.factories.model import AchievementFactory
from staff.lib.testing import StaffFactory


def _create_valid_form_data():
    st = StaffFactory()
    form_data = {
        'title': 'title ru',
        'title_en': 'title en',
        'service_name': 'qwerty',
        'category': 'fun',
        'creator_login': st.login,
    }
    return form_data


@pytest.mark.django_db
def test_create_achievement_form_valid_data():

    form_data = _create_valid_form_data()
    form = CreateAchievementForm(icon_required=False, data=form_data)

    expected_achievement_data = {
        'title': form_data.get('title', ''),
        'title_en': form_data.get('title_en', ''),
        'description': form_data.get('description', ''),
        'description_en':  form_data.get('description_en', ''),
        'description_short':  form_data.get('description_short', ''),
        'description_short_en':  form_data.get('description_short_en', ''),
        'description_html':  form_data.get('description_html', ''),
        'description_html_en':  form_data.get('description_html_en', ''),
        'service_name': form_data.get('service_name', ''),
        'service_name_en': form_data.get('service_name_en', ''),
        'category': form_data.get('category', ''),
    }

    assert form.is_valid(), form.errors
    assert form.achievement_data == expected_achievement_data


@pytest.mark.django_db
def test_clean_title_valid_data():
    title = 'cool achievement'

    form_data = _create_valid_form_data()
    form_data['title'] = title

    form = CreateAchievementForm(icon_required=False, data=form_data)

    assert form.is_valid()
    assert form.cleaned_data['title'] == title


@pytest.mark.django_db
def test_clean_title_invalid_data():
    title = 'cool achievement'

    form_data = _create_valid_form_data()
    form_data['title'] = title

    AchievementFactory(title=title)
    form = CreateAchievementForm(icon_required=False, data=form_data)

    assert not form.is_valid()
    assert ('title', [f'Achievement {title} already exists, use other name']) in form.errors.items()


@pytest.mark.django_db
def test_clean_title_en_valid_data():
    title_en = 'cool achievement'

    form_data = _create_valid_form_data()
    form_data['title_en'] = title_en

    form = CreateAchievementForm(icon_required=False, data=form_data)

    assert form.is_valid()
    assert form.cleaned_data['title_en'] == title_en


@pytest.mark.django_db
def test_clean_title_en_invalid_data():
    title_en = 'cool achievement'

    form_data = _create_valid_form_data()
    form_data['title_en'] = title_en

    AchievementFactory(title_en=title_en)
    form = CreateAchievementForm(icon_required=False, data=form_data)

    assert not form.is_valid()
    assert ('title_en', [f'Achievement {title_en} already exists, use other name']) in form.errors.items()


@pytest.mark.django_db
def test_clean_icon_url_valid_data():
    icon_url = 'some icon url'
    form_data = _create_valid_form_data()
    form_data['icon_url'] = icon_url

    form = CreateAchievementForm(icon_required=True, data=form_data)

    assert form.is_valid()


@pytest.mark.django_db
def test_clean_icon_url_invalid_data():
    form_data = _create_valid_form_data()
    form_data['icon_url'] = ''

    form = CreateAchievementForm(icon_required=True, data=form_data)

    assert not form.is_valid()
    assert ('icon_url', ['Icon url is empty']) in form.errors.items()


@pytest.mark.django_db
def test_clean_creator_login_valid_data():
    st = StaffFactory()

    form_data = _create_valid_form_data()
    form_data['creator_login'] = st.login

    form = CreateAchievementForm(icon_required=False, data=form_data)

    assert form.is_valid()
    assert form.cleaned_data['creator'] == st


@pytest.mark.django_db
def test_clean_creator_login_invalid_data():
    bad_login = '#####'

    form_data = _create_valid_form_data()
    form_data['creator_login'] = bad_login

    form = CreateAchievementForm(icon_required=False, data=form_data)

    assert not form.is_valid()
    assert ('creator_login', [f'User with login {bad_login} does not exist']) in form.errors.items()
