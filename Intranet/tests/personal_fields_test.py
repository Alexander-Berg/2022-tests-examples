import pytest
import sform

from staff.preprofile.forms.fields import personal_fields


class PersonalForm(sform.SForm):
    first_name = personal_fields.first_name()
    last_name = personal_fields.last_name()
    first_name_en = personal_fields.first_name_en()
    last_name_en = personal_fields.last_name_en()
    middle_name = personal_fields.middle_name()


@pytest.fixture
def form_data_dictionary():
    data = {
        'first_name_en': 'Name',
        'last_name_en': 'Surname',
        'first_name': 'Имя',
        'last_name': 'Фамилия',
        'middle_name': 'Отчество',
    }

    return data


@pytest.mark.parametrize('valid_name', ['Denis', 'Anna', 'John', 'Abyrvalg', 'Lol', 'Kek)', 'Cheburek-'])
def test_first_name_en_valid_names(form_data_dictionary, valid_name):
    form_data_dictionary['first_name_en'] = valid_name

    form = PersonalForm(form_data_dictionary)
    assert form.is_valid()


@pytest.mark.parametrize('invalid_name', ['D__enis', 'An--na', 'Jo\'\'hn', 'Abyr!valg', 'Lo@l', 'Ke#k', 'Che$burek'])
def test_first_name_en_invalid_names(form_data_dictionary, invalid_name):
    form_data_dictionary['first_name_en'] = invalid_name

    form = PersonalForm(form_data_dictionary)
    assert not form.is_valid()


@pytest.mark.parametrize('valid_name', ['Denis', 'Anna', 'John', 'Abyrvalg', 'Lol', 'Kek)', 'Cheburek-'])
def test_last_name_en_valid_names(form_data_dictionary, valid_name):
    form_data_dictionary['last_name_en'] = valid_name

    form = PersonalForm(form_data_dictionary)
    assert form.is_valid()


@pytest.mark.parametrize('invalid_name', ['D__enis', 'An--na', 'Jo\'\'hn', 'Abyr!valg', 'Lo@l', 'Ke#k', 'Che$burek'])
def test_last_name_en_invalid_names(form_data_dictionary, invalid_name):
    form_data_dictionary['first_name_en'] = invalid_name

    form = PersonalForm(form_data_dictionary)
    assert not form.is_valid()


@pytest.mark.parametrize('valid_name', ['Денис', 'Анна', 'Иван', 'Абырвалг', 'Лол', 'Кек)', 'Чебурек-'])
def test_first_name_valid_names(form_data_dictionary, valid_name):
    form_data_dictionary['first_name'] = valid_name

    form = PersonalForm(form_data_dictionary)
    assert form.is_valid()


@pytest.mark.parametrize('invalid_name', ['Де__нис', 'Ан--на', 'Ив\'\'ан', 'Абыр!валг', 'Ло@л', 'Ке#к', 'Че$бурек'])
def test_first_name_invalid_names(form_data_dictionary, invalid_name):
    form_data_dictionary['first_name'] = invalid_name

    form = PersonalForm(form_data_dictionary)
    assert not form.is_valid()


@pytest.mark.parametrize('valid_name', ['Денис', 'Анна', 'Иван', 'Абырвалг', 'Лол(', 'Кек)', 'Чебурек-'])
def test_last_name_valid_names(form_data_dictionary, valid_name):
    form_data_dictionary['last_name'] = valid_name

    form = PersonalForm(form_data_dictionary)
    assert form.is_valid()


@pytest.mark.parametrize('invalid_name', ['Де__нис', 'Ан--на', 'Ив\'\'ан', 'Абыр!валг', 'Ло@л', 'Ке#к', 'Че$бурек'])
def test_last_name_invalid_names(form_data_dictionary, invalid_name):
    form_data_dictionary['last_name'] = invalid_name

    form = PersonalForm(form_data_dictionary)
    assert not form.is_valid()


@pytest.mark.parametrize('valid_name', ['Денис', 'Анна', 'Иван', 'Абырвалг', 'Лол', 'Кек', 'Чебурек'])
def test_middle_name_valid_names(form_data_dictionary, valid_name):
    form_data_dictionary['middle_name'] = valid_name

    form = PersonalForm(form_data_dictionary)
    assert form.is_valid()


@pytest.mark.parametrize('invalid_name', ['Де__нис', 'Ан--на', 'Ив\'\'ан', 'Абыр(валг', 'Ло)л', 'Ке_к', 'Че$бурек'])
def test_middle_name_invalid_names(form_data_dictionary, invalid_name):
    form_data_dictionary['middle_name'] = invalid_name

    form = PersonalForm(form_data_dictionary)
    assert not form.is_valid()
