# coding: utf-8


import pytest
from django.core import mail
from mock import patch

from idm.core.workflow.exceptions import BossAndZamNotAvailableError
from idm.core.models import Role
from idm.core.workflow.plain.user import UserWrapper
from idm.tests.utils import set_workflow, assert_contains, add_perms_by_role
from idm.users.models import Department

pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def __shim_department(arda_users):
    # TODO: удалить при удалении департаментов
    department = Department.objects.create(name='dev', slug='dev', chief=arda_users.frodo)
    arda_users.legolas.department = department
    arda_users.legolas.save()


def test_absent_heads(simple_system, arda_users):
    """Проверяем отправку писем ответственным по системе при запросе роли,
    если подтверждающие руководители отсутствуют"""

    set_workflow(simple_system, 'approvers = [approver(user.get_boss_or_zam())]')

    frodo = arda_users.frodo
    gandalf = arda_users.gandalf
    galadriel = arda_users.galadriel
    legolas = arda_users.legolas

    with patch.object(UserWrapper, 'get_boss_or_zam',  autospec=True) as uw:
        uw.side_effect = BossAndZamNotAvailableError(boss=frodo, zams=[gandalf, galadriel])

        with pytest.raises(BossAndZamNotAvailableError):
            Role.objects.request_role(legolas, legolas, simple_system, '', {'role': 'admin'}, None)

    assert Role.objects.count() == 0


def test_absent_head_can_still_request_role_for_subordinates(simple_system, arda_users):
    """Проверяем RULES-1499: если роль запрашивает отсутствующий руководитель,
    то он не считается отсутствующим и не исключается из цепочки проверки"""

    set_workflow(simple_system, 'approvers = [approver(user.get_boss_or_zam())]')

    frodo = arda_users.frodo
    legolas = arda_users.legolas

    with patch('idm.sync.gap.is_user_absent') as is_absent:
        is_absent.return_value = True

        Role.objects.request_role(frodo, legolas, simple_system, '', {'role': 'admin'}, None)

    assert Role.objects.count() == 1
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['legolas@example.yandex.ru']
    assert message.subject == 'Simple система. Новая роль'
    assert_contains(('Вы получили новую роль в системе', 'Simple система', 'Роль: Админ'), message.body)


def test_absent_deputy_can_still_request_role_for_subordinates(simple_system, arda_users):
    """Проверяем RULES-1499: если роль запрашивает отсутствующий заместитель отсутствующего руководителя,
    то он (заместитель) не считается отсутствующим и не исключается из цепочки проверки"""

    set_workflow(simple_system, 'approvers = [approver(user.get_boss_or_zam())]')
    legolas = arda_users.legolas
    sam = arda_users.sam
    # TODO: удалить после решения задачи про заместителей
    add_perms_by_role('responsible', sam)

    with patch('idm.core.workflow.plain.user.get_department_zams') as get_deputies:
        get_deputies.return_value = ['sam']
        with patch('idm.sync.gap.is_user_absent') as is_absent:
            is_absent.return_value = True

            Role.objects.request_role(sam, legolas, simple_system, '', {'role': 'admin'}, None)

    assert Role.objects.count() == 1
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == ['legolas@example.yandex.ru']
    assert message.subject == 'Simple система. Новая роль'
    assert_contains(('Вы получили новую роль в системе', 'Simple система', 'Роль: Админ'), message.body)
