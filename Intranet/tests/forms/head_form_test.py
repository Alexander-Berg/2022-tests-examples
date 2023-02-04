import pytest
from staff.person_profile.forms.head import HeadForm


DEFAULT_POST_DATA = {
    'first_name': 'Владимир',
    'last_name': 'Спасский',
    'middle_name': 'Климентович',
    'first_name_en': 'Volodymyr',
    'last_name_en': 'Spasskyi',
    'position': 'разработчик',
    'position_en': 'software developer\r\nnew line',
}
NO_ERRORS = {}


def _head_post_data(**kwargs):
    post_data = DEFAULT_POST_DATA.copy()
    post_data.update(kwargs)
    return post_data


EDIT_HEAD_CASES = [
    # (<post data>: {<field_name>: <error_key>}),
    # valid
    (_head_post_data(), NO_ERRORS),
    (_head_post_data(
        preferred_name='Виктор Разработчик',
        preferred_name_en='Victor Developer',
    ), NO_ERRORS),
    (
        _head_post_data(
            first_name='Анна-Мария', last_name='Петров-Во ́дкин',
            middle_name='Зурабович Церетелиевич',
            first_name_en='Do Re Mi', last_name_en='Fa-Sol-la',
        ),
        NO_ERRORS
    ),
    (
        _head_post_data(
            first_name='Марина(Ґал\'я)', last_name='Öбама(Усіновä)',
            middle_name='Ваçил\'ьевнÄ',
            first_name_en='Galina', last_name_en='Brusnichkina',
            position='Должность(не моя)', position_en='Yo ́'
        ),
        {
            'position_en': 'staff-invalid_latin_field',
        }
    ),
    (
        _head_post_data(
            first_name_en='Ga ́lina', last_name_en='Brusnichkina(Niyazova)',
        ),
        NO_ERRORS
    ),

    # invalid
    (
        _head_post_data(
            first_name='Марина 2-я', last_name='У', middle_name='Тимуровна()',
            first_name_en='Русские', last_name_en='символы',
            position_en='калькулятор'
        ),
        {
            'first_name': 'staff-invalid_name_field',
            'last_name': 'staff-invalid_name_field',
            'middle_name': 'staff-invalid_name_field',
            'first_name_en': 'staff-invalid_name_field',
            'last_name_en': 'staff-invalid_name_field',
            'position_en': 'staff-invalid_latin_field',
        }
    ),

    (
        _head_post_data(
            first_name='-Минусик', last_name='(Скобкин)', middle_name='\'квчк',
            first_name_en='Joseph--mcDonaldas', last_name_en='X  Y-Z',
        ),
        {
            'first_name': 'staff-invalid_name_field',
            'last_name': 'staff-invalid_name_field',
            'middle_name': 'staff-invalid_name_field',
            'first_name_en': 'staff-invalid_name_field',
            'last_name_en': 'staff-invalid_name_field',
        }
    ),

    (
        _head_post_data(
            first_name='', last_name='', middle_name='(NotAtAll)',
            first_name_en='', last_name_en='', position=''
        ),
        {
            'first_name': 'default-field-required',
            'last_name': 'default-field-required',
            'middle_name': 'staff-invalid_name_field',
            'first_name_en': 'default-field-required',
            'last_name_en': 'default-field-required',
            'position': 'default-field-required',
        }
    ),

    (
        _head_post_data(
            first_name=(
                'Гиясаддин Абуль-Фатх Омар ибн Ибрахим аль-Хайям Нишапури'
            ),
            last_name=(
                'Гиясаддин Абуль-Фатх Омар ибн Ибрахим аль-Хайям Нишапури '
                'Гиясаддин Абуль-Фатх Омар ибн Ибрахим аль-Хайям Нишапури'
            ),
            middle_name=(
                'очень длинное отчество очень длинное '
                'отчество очень длинное отчество'
            ),
            first_name_en=(
                'longlonglonglonglonglonglonglonglonglonglonglonglonglonglong'
            ),
            last_name_en=(
                'longlonglonglonglonglonglonglonglonglonglonglonglong'
                'longlonglonglonglonglonglonglonglonglonglonglonglong'
            )
        ),
        {
            'first_name': 'default-field-max_length',
            'last_name': 'default-field-max_length',
            'middle_name': 'default-field-max_length',
            'first_name_en': 'default-field-max_length',
            'last_name_en': 'default-field-max_length',
        }
    ),
]


@pytest.mark.parametrize('test_data', EDIT_HEAD_CASES)
def test_form_validation(test_data):
    form_data, supposed_errors = test_data
    form = HeadForm(form_data)
    assert form.is_valid() == (not supposed_errors)
    assert set(supposed_errors.keys()) == set(form.errors.keys())
    for error_field, error_key in supposed_errors.items():
        assert form.errors[error_field][0]['error_key'] == error_key
