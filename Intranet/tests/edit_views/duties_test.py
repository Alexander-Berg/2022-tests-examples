from datetime import date
import json

import pytest

from django.conf import settings
from django.core.urlresolvers import reverse

from staff.person.models import Staff
from staff.lib.testing import StaffFactory
from staff.innerhire.models import Candidate

from staff.person_profile.edit_views.edit_duties_view import FORM_NAME


VIEW_NAME = 'profile:edit-duties'

DUTY = {
    'duties': 'русские обязательства',
    'duties_en': 'english duties',
    'candidate_info': 'candidate info',
}

EMPTY_DUTY = {
    'duties': '',
    'duties_en': '',
    'candidate_info': '',
}

DUTY_ANSWER_BODY = 'candidate info'

WRONG_DUTY = {
    "duties": """В ДНК встречается четыре вида азотистых оснований (аденин,
гуанин, тимин и цитозин). Азотистые основания одной из цепей соединены с
азотистыми основаниями другой цепи водородными связями согласно принципу
комплементарности: аденин соединяется только с тимином, гуанин — только с
цитозином. Последовательность нуклеотидов позволяет «кодировать» информацию о
различных типах РНК, наиболее важными из которых являются информационные, или
матричные (мРНК), рибосомальные (рРНК) и транспортные (тРНК). Все эти типы РНК
синтезируются на матрице ДНК за счёт копирования последовательности ДНК в
последовательность РНК, синтезируемой в процессе транскрипции, и принимают
участие в биосинтезе белков (процессе трансляции). Помимо кодирующих
последовательностей, ДНК клеток содержит последовательности, выполняющие
регуляторные и структурные функции. Кроме того, в геноме эукариот часто
встречаются участки, принадлежащие «генетическим паразитам», например,
транспозонам. С химической точки зрения ДНК — это длинная полимерная молекула,
состоящая из повторяющихся блоков — нуклеотидов. Каждый нуклеотид состоит из
азотистого основания, сахара (дезоксирибозы) и фосфатной группы. Связи между
нуклеотидами в цепи образуются за счёт дезоксирибозы и фосфатной группы
(фосфодиэфирные связи).""",
    'duties_en': '',
    'candidate_info': '',
}


@pytest.mark.django_db()
def test_edit_duty(client):
    test_person = StaffFactory(login=settings.AUTH_TEST_USER)
    url = reverse(VIEW_NAME, kwargs={'login': test_person.login})

    response = client.post(
        url,
        json.dumps({FORM_NAME: [DUTY]}),
        content_type='application/json',
    )

    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {
        'target': {'candidate_info': DUTY_ANSWER_BODY}
    }

    assert (
        Staff.objects.values('duties', 'duties_en')
        .get(login=test_person.login) == {
            'duties': DUTY['duties'],
            'duties_en': DUTY['duties_en'],
        }
    )
    assert (
        Candidate.objects.values_list(
            'description_source', 'description', 'date_open', 'date_close'
        ).get(person=test_person) == (
            DUTY['candidate_info'],
            DUTY_ANSWER_BODY,
            date.today(),
            None,
        )
    )

    # Добавляем невалидный duty
    response = client.post(
        url,
        json.dumps({FORM_NAME: [WRONG_DUTY]}),
        content_type='application/json',
    )

    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {
        'errors': {
            FORM_NAME: {
                '0': {
                    'duties': [
                        {
                            'error_key': 'default-field-max_length',
                            'params': {
                                'limit_value': '1024', 'show_value': '1276'
                            }
                        }
                    ]
                }
            }
        }
    }

    # Очищаем duty

    response = client.post(
        url,
        json.dumps({FORM_NAME: [EMPTY_DUTY]}),
        content_type='application/json',
    )

    assert response.status_code == 200, response.content
    answer = json.loads(response.content)
    assert answer == {}

    assert (
        Staff.objects.values('duties', 'duties_en')
        .get(login=test_person.login) == {'duties': '', 'duties_en': ''}
    )
    assert (
        Candidate.objects.values_list(
            'description_source', 'description', 'date_open', 'date_close'
        ).get(person=test_person) == (
            DUTY['candidate_info'],
            ''.join(DUTY_ANSWER_BODY.splitlines()),
            date.today(),
            date.today(),
        )
    )
