import pytest
from django.core import mail
from django.core.urlresolvers import reverse

from plan.suspicion.constants import ServiceAppealIssueStates
from plan.suspicion.models import ServiceAppealIssue
from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def test_data(owner_role, staff_factory):
    service1 = factories.ServiceFactory()
    service2 = factories.ServiceFactory(parent=service1)
    service3 = factories.ServiceFactory(parent=service2)

    staff1 = staff_factory()
    staff2 = staff_factory()
    staff3 = staff_factory()
    factories.ServiceMemberFactory(staff=staff3, service=service3, role=owner_role)
    issue = factories.IssueFactory(name='Какая-то проблема', description='Какое-то описание')
    service_issue = factories.ServiceIssueFactory(service=service3, issue=issue)

    return {
        'services': [service1, service2, service3],
        'staff': [staff1, staff2, staff3],
        'service_issue': service_issue,
    }


def test_new_appeal_not_send_email_if_dont_have_responsible(client, data, responsible_role, test_data):
    """
    Проверим, что если у вышестоящех сервисов отсутствуют управляющие, то письмо никому не отправим
    """
    _, _, staff3 = test_data['staff']
    service_issue = test_data['service_issue']
    client.login(staff3.login)
    url = reverse('api-v3:appeal-list')

    response = client.json.post(url, {'issue': service_issue.id, 'message': 'Какой-то комментарий'})
    assert response.status_code == 201
    assert len(mail.outbox) == 0


def test_new_appeal_send_email_if_root_have_responsible(client, data, settings, test_data, owner_role):
    """
    Проверим, что если руководители есть только у самого верхнего сервиса,
    то письмо уйдет ему, на рассылку и создателю апелляции
    """
    staff1, _, staff3 = test_data['staff']
    service1, _, _ = test_data['services']
    service_issue = test_data['service_issue']
    factories.ServiceMemberFactory(staff=staff1, service=service1, role=owner_role)
    client.login(staff3.login)
    url = reverse('api-v3:appeal-list')
    response = client.json.post(url, {'issue': service_issue.id, 'message': 'Какой-то комментарий'})
    assert response.status_code == 201
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == [staff1.work_email]
    assert set(message.cc) == set(settings.DEFAULT_CC_EMAILS + [staff3.work_email])
    assert message.subject == '[ABC] Подтвердите исключение из правил'


def test_new_appeal_send_email_only_nearby_responsibles(client, data, settings, test_data, owner_role, responsible_role):
    """
    Проверим, что если руководители есть у всех сервисов в цепочке,
    то письмо уйдет ближайшему руководителю, на рассылку и создателю апелляции
    """
    staff1, staff2, staff3 = test_data['staff']
    staff4 = factories.StaffFactory()
    service1, service2, _ = test_data['services']
    service_issue = test_data['service_issue']
    factories.ServiceMemberFactory(staff=staff1, service=service1, role=owner_role)
    factories.ServiceMemberFactory(staff=staff2, service=service2, role=responsible_role)
    factories.ServiceMemberFactory(staff=staff4, service=service2, role=owner_role)
    client.login(staff3.login)
    url = reverse('api-v3:appeal-list')
    response = client.json.post(url, {'issue': service_issue.id, 'message': 'Какой-то комментарий'})
    assert response.status_code == 201
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert sorted(message.to) == sorted([staff2.work_email, staff4.work_email])
    assert set(message.cc) == set(settings.DEFAULT_CC_EMAILS + [staff3.work_email])
    assert message.subject == '[ABC] Подтвердите исключение из правил'


def test_autoapprove_appeal_not_send_email(client, data, test_data, owner_role, responsible_role):
    """
    Проверим, что автоподтвержденная апелляция не отправляет письма
    """
    staff1, staff2, staff3 = test_data['staff']
    service1, service2, _ = test_data['services']
    service_issue = test_data['service_issue']
    factories.ServiceMemberFactory(staff=staff1, service=service1, role=owner_role)
    factories.ServiceMemberFactory(staff=staff2, service=service2, role=responsible_role)
    client.login(staff1.login)
    url = reverse('api-v3:appeal-list')
    response = client.json.post(url, {'issue': service_issue.id, 'message': 'Какой-то комментарий'})
    appeal = ServiceAppealIssue.objects.get()
    assert appeal.state == ServiceAppealIssueStates.APPROVED
    assert response.status_code == 201
    assert len(mail.outbox) == 0


def test_appeal_approve_send_email(client, data, settings, test_data, responsible_role):
    """
    Проверим, что при подтверждении апелляции отправим письмо, запросившему апелляцию
    """
    _, approver, requester = test_data['staff']
    _, service2, service3 = test_data['services']
    service_issue = test_data['service_issue']
    factories.ServiceMemberFactory(staff=approver, service=service2, role=responsible_role)
    appeal = factories.ServiceAppealIssueFactory(service_issue=service_issue, requester=requester)

    client.login(approver.login)
    url = reverse('api-v3:appeal-approve', args=[appeal.id])
    response = client.json.post(url)
    assert response.status_code == 204
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == [requester.work_email]
    assert message.cc == settings.DEFAULT_CC_EMAILS
    assert message.subject == '[ABC] Принято решение по вашему запросу'
    assert 'согласовал ответственный вышестоящего сервиса' in message.body


def test_appeal_reject_send_email(client, data, settings, test_data, responsible_role):
    """
    Проверим, что при подтверждении апелляции отправим письмо, запросившему апелляцию
    """
    _, rejecter, requester = test_data['staff']
    _, service2, service3 = test_data['services']
    service_issue = test_data['service_issue']
    factories.ServiceMemberFactory(staff=rejecter, service=service2, role=responsible_role)
    appeal = factories.ServiceAppealIssueFactory(service_issue=service_issue, requester=requester)

    client.login(rejecter.login)
    url = reverse('api-v3:appeal-reject', args=[appeal.id])
    response = client.json.post(url)
    assert response.status_code == 204
    assert len(mail.outbox) == 1
    message = mail.outbox[0]
    assert message.to == [requester.work_email]
    assert message.cc == settings.DEFAULT_CC_EMAILS
    assert message.subject == '[ABC] Принято решение по вашему запросу'
    assert 'отклонил ответственный вышестоящего сервиса' in message.body
