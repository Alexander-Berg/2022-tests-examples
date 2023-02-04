import json
import mock

import pytest

from django.conf import settings
from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from staff.person.models import EMPLOYMENT

from staff.preprofile.models import CITIZENSHIP, Preprofile, FORM_TYPE, PREPROFILE_STATUS
from staff.preprofile import tasks
from staff.preprofile.utils import generate_masshire_login
from staff.preprofile.views import edit_form, new_form, person_forms, check_masshire_status
from staff.preprofile.views.views import post_new_person_form


@pytest.fixture
def post_masshire_form(company, tester, base_form):
    def fu(**kwargs):
        tester.user_permissions.add(Permission.objects.get(codename='can_outstaff'))
        base_form.update({
            'department': company.dep1.url,
            'login': 'testlogin',
            'citizenship': CITIZENSHIP.UKRAINIAN,
        })
        base_form.update(**kwargs)
        return post_new_person_form(base_form, FORM_TYPE.MASS_HIRE, tester.staff)
    return fu


@pytest.fixture
def get_masshire_preprofile_id(post_masshire_form):
    def fu(**kwargs):
        json_resp, _ = post_masshire_form(**kwargs)
        id_ = json_resp['id']
        tag = kwargs.get('tag')
        if tag:
            Preprofile.objects.filter(id=id_).update(masshire_tag=tag)
        return json_resp['id']
    return fu


@pytest.mark.django_db
def test_masshire_form_correct_data_saving(company, post_masshire_form):
    login = 'testlogin'
    dep = company.dep2

    json_resp, code = post_masshire_form(login=login, department=dep.url)

    assert code == 200, json_resp
    preprofile = Preprofile.objects.get(login=login)
    assert json_resp['id'] == preprofile.id
    assert preprofile.department == dep
    assert preprofile.login == login
    assert preprofile.status == PREPROFILE_STATUS.NEW
    assert preprofile.employment_type == EMPLOYMENT.FULL
    assert preprofile.organization_id == settings.ROBOTS_ORGANIZATION_ID
    assert preprofile.office_id == settings.HOMIE_OFFICE_ID


@pytest.mark.django_db()
def test_create_masshire_through_api_forbidden(rf, tester, base_form):
    request = rf.post(
        reverse('preprofile:new_form', kwargs={'form_type': FORM_TYPE.MASS_HIRE}),
        json.dumps(base_form),
        content_type='application/json',
    )
    request.user = tester

    result = new_form(request, FORM_TYPE.MASS_HIRE)

    assert result.status_code == 400
    assert json.loads(result.content)['errors'][0]['message'] == 'invalid_type'


@pytest.mark.django_db
@pytest.mark.parametrize(
    'checking_person', ['dep1-chief', 'dep1-hr-partner']
)
def test_correct_sees_masshire_preprofile(rf, company, get_masshire_preprofile_id, checking_person):
    preprofile_id = get_masshire_preprofile_id(department=company.dep1.url)

    request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile_id}))
    request.user = company.persons[checking_person].user
    result = edit_form(request, preprofile_id)

    assert result.status_code == 200, result.content
    assert json.loads(result.content).get('form_type') == FORM_TYPE.MASS_HIRE, result.content


@pytest.mark.django_db
def test_user_with_masshire_perm_sees_preprofile(rf, company, get_masshire_preprofile_id):
    preprofile_id = get_masshire_preprofile_id(department=company.dep1.url)
    random_user = company.persons['dep2-person'].user

    random_user.user_permissions.add(Permission.objects.get(codename='can_outstaff'))
    request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile_id}))
    request.user = random_user
    result = edit_form(request, preprofile_id)

    assert result.status_code == 200, result.content
    assert json.loads(result.content).get('form_type') == FORM_TYPE.MASS_HIRE, result.content


@pytest.mark.django_db
def test_user_without_any_perm_not_sees_preprofile(rf, company, get_masshire_preprofile_id):
    preprofile_id = get_masshire_preprofile_id(department=company.dep1.url)
    random_user = company.persons['dep111-person'].user

    request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile_id}))
    request.user = random_user
    result = edit_form(request, preprofile_id)

    assert result.status_code == 403, result.content


@pytest.mark.django_db
def test_user_with_masshire_perm_not_sees_preprofile_list(rf, tester, get_masshire_preprofile_id):
    preprofile_id = get_masshire_preprofile_id()
    tester.user_permissions.add(Permission.objects.get(codename='can_outstaff'))
    request = rf.get(reverse('preprofile:person_forms'))
    request.user = tester

    result = person_forms(request)

    assert result.status_code == 200
    result = json.loads(result.content)
    ids = {form['id'] for form in result['result']}
    assert preprofile_id not in ids, ids


@pytest.mark.django_db
def test_user_with_masshire_perm_can_adopt(tester, get_masshire_preprofile_id, robot_staff_user, achievements):
    tag = 'mh'
    masshire_id = get_masshire_preprofile_id(tag=tag)
    tester.user_permissions.add(Permission.objects.get(codename='can_outstaff'))
    tester.user_permissions.add(Permission.objects.get(codename='add_personadoptapplication'))
    with mock.patch('staff.preprofile.controllers.controller.get_uid_from_passport', return_value='ads'):
        with mock.patch('staff.preprofile.controllers.controller.get_guid_from_ldap', return_value='ads'):
            tasks.adopt_masshire(person_id=tester.get_profile().id, masshire_tags=[tag])
    assert Preprofile.objects.filter(id=masshire_id, status=PREPROFILE_STATUS.CLOSED).exists()


@pytest.mark.django_db
def test_user_with_masshire_perm_can_view_status(rf, tester, get_masshire_preprofile_id):
    tag = 'mh'
    masshire_ids = [get_masshire_preprofile_id(tag=tag, login=f'testlogin{i}{i}') for i in range(3)]
    tester.user_permissions.add(Permission.objects.get(codename='can_outstaff'))

    request = rf.get(reverse('preprofile:check_masshire_status', kwargs={'tag': tag}))
    request.user = tester
    result = check_masshire_status(request, tag)

    expected_res = dict.fromkeys(map(str, masshire_ids), PREPROFILE_STATUS.NEW)
    assert json.loads(result.content) == expected_res


test_data = {
    ('Анастасия', 'Савельева', 'Александровна', 'F'): ('a-a-saveleva', 'Anastasiya', 'Saveleva'),
    ('Алиса', 'Черёмушникова', 'Романовна', 'F'): ('a-r-cheremu', 'Alisa', 'Cheremushnikova'),
    ('Зоя', 'Щеголькова', 'Максимовна', 'F'): ('z-m-schegolko', 'Zoya', 'Schegolkova'),
    ('Юлия', 'Эскендарова', 'Владиславовна', 'F'): ('y-v-eskendaro', 'Yuliya', 'Eskendarova'),
    ('Анастасия', 'Исингалиева', 'Арстановна', 'F'): ('a-a-isingalie', 'Anastasiya', 'Isingalieva'),
    ('Дана', 'Зайцева', 'Ивановна', 'F'): ('d-i-zaytseva', 'Dana', 'Zaytseva'),
    ('Мария', 'Инова', 'Михайловна', 'F'): ('m-m-inova', 'Mariya', 'Inova'),
    ('Анна', 'Кожина', 'Александровна', 'F'): ('a-a-kozhina', 'Anna', 'Kozhina'),
    ('Елена', 'Литвиненко', 'Владимировна', 'F'): ('e-v-litvine', 'Elena', 'Litvinenko'),
    ('Ксения', 'Осипова', 'Эдуардовна', 'F'): ('k-e-osipova', 'Kseniya', 'Osipova'),
    ('Елена', 'Семисотнова', 'Андреевна', 'F'): ('e-a-semisotno', 'Elena', 'Semisotnova'),
    ('Татьяна', 'Халикова', 'Сергеевна', 'F'): ('t-s-halikova', 'Tatyana', 'Halikova'),
    ('Дарья', 'Батракова', 'Ивановна', 'F'): ('d-i-batrakova', 'Darya', 'Batrakova'),
    ('Екатерина', 'Жданова', 'Александровна', 'F'): ('e-a-zhdanova', 'Ekaterina', 'Zhdanova'),
    ('Вероника', 'Збрицкая', 'Владимировна', 'F'): ('v-v-zbritskay', 'Veronika', 'Zbritskaya'),
    ('Елена', 'Мельникова', 'Валерьевна', 'F'): ('e-v-melnikova', 'Elena', 'Melnikova'),
    ('Полина', 'Никифорова', 'Александровна', 'F'): ('p-a-nikiforo', 'Polina', 'Nikiforova'),
    ('Диана', 'Ростовцева', 'Олеговна', 'F'): ('d-o-rostovtse', 'Diana', 'Rostovtseva'),
    ('Виктория', 'Рудь', 'Валерьевна', 'F'): ('v-v-rud', 'Viktoriya', 'Rud'),
    ('Елизавета', 'Яшникова', 'Валерьевна', 'F'): ('e-v-yashniko', 'Elizaveta', 'Yashnikova'),
    ('Ирина', 'Атрошенко', 'Александровна', 'F'): ('i-a-atroshe', 'Irina', 'Atroshenko'),
    ('Виктория', 'Гречухина', 'Григорьевна', 'F'): ('v-g-grechuhi', 'Viktoriya', 'Grechuhina'),
    ('Дарья', 'Емельянова', 'Александровна', 'F'): ('d-a-emelyano', 'Darya', 'Emelyanova'),
    ('Ксения', 'Жадаева', 'Ираклиевна', 'F'): ('k-i-zhadaeva', 'Kseniya', 'Zhadaeva'),
    ('Нина', 'Кащеева', 'Александровна', 'F'): ('n-a-kascheeva', 'Nina', 'Kascheeva'),
    ('Мария', 'Ким', 'Владимировна', 'F'): ('m-v-kim', 'Mariya', 'Kim'),
    ('Михаил', 'Самойловский', 'Игоревич', 'M'): ('m-i-samoylovs', 'Mihail', 'Samoylovskiy'),
    ('Рамазан', 'Анненков', 'Александрович', 'M'): ('r-a-annenkov', 'Ramazan', 'Annenkov'),
    ('Михаил', 'Бурджаев', 'Гурбанович', 'M'): ('m-g-burdzhaev', 'Mihail', 'Burdzhaev'),
    ('Вадим', 'Сидоров', 'Сергеевич', 'M'): ('v-s-sidorov', 'Vadim', 'Sidorov'),
    ('Алексей', 'Тололин', 'Анатольевич', 'M'): ('a-a-tololin', 'Aleksey', 'Tololin'),
    ('Богдан', 'Тонкошкур', 'Алексеевич', 'M'): ('b-a-tonkoshk', 'Bogdan', 'Tonkoshkur'),
    ('Назар', 'Белик', 'Александрович', 'M'): ('n-a-belik', 'Nazar', 'Belik'),
    ('Руслан', 'Винский', 'Владиславович', 'M'): ('r-v-vinskiy', 'Ruslan', 'Vinskiy'),
    ('Ян', 'Далаков', 'Михайлович', 'M'): ('y-m-dalakov', 'Yan', 'Dalakov'),
    ('Михаил', 'Мамаев', 'Андреевич', 'M'): ('m-a-mamaev', 'Mihail', 'Mamaev'),
    ('Егор', 'Пушанкин', 'Сергеевич', 'M'): ('e-s-pushankin', 'Egor', 'Pushankin'),
    ('Максим', 'Таранов', 'Александрович', 'M'): ('m-a-taranov', 'Maksim', 'Taranov'),
    ('Алексей', 'Ювашев', 'Александрович', 'M'): ('a-a-yuvashev', 'Aleksey', 'Yuvashev'),
    ('Максим', 'Войтенко', 'Андреевич', 'M'): ('m-a-voytenko', 'Maksim', 'Voytenko'),
    ('Александр', 'Григорьев', 'Владимирович', 'M'): ('a-v-grigorev', 'Aleksandr', 'Grigorev'),
    ('Вячеслав', 'Иванчук', 'Александрович', 'M'): ('v-a-ivanchuk', 'Vyacheslav', 'Ivanchuk'),
    ('Евгений', 'Калачаров', 'Евгеньевич', 'M'): ('e-e-kalachar', 'Evgeniy', 'Kalacharov'),
}


def test_generate_masshire_login(db):
    with mock.patch('staff.preprofile.login_validation.validate_in_ldap'):
        with mock.patch('staff.preprofile.login_validation.validate_for_dns'):
            for fio, result in test_data.items():
                assert generate_masshire_login(*fio) == result
