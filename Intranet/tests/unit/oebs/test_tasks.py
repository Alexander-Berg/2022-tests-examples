import pytest

from mock import patch, call, Mock
from datetime import timedelta

from django.utils import timezone
from django.test.utils import override_settings
from django.conf import settings

from utils import vcr_test
from plan.oebs.tasks.start_approve import start_oebs_approve_process
from common import factories
from plan.oebs.constants import (
    STATES,
    ERRORS,
    ACTIONS,
    OEBS_HR_FLAG,
    OEBS_PROCUREMENT_FLAG,
    OEBS_REVENUE_FLAG,
    OEBS_MATCHING_FLAGS_AND_ROLES,
    OEBS_FLAGS,
)
from plan.common.utils.ok import OkClient
from plan.oebs.tasks.check_process import check_oebs_approve_process, sync_tags
from plan.oebs.tasks.finish_approve import finish_oebs_approve_process
from plan.resources.models import ServiceResource
from plan.services.models import ServiceTag
from .conftest import StartrekIssue


def mock_get_request_state(self, agreement_id):
    if agreement_id.ok_id == 'finish_id':
        return 'closed', 'approved'
    elif agreement_id.ok_id == 'decline_id':
        return 'closed', 'declined'
    elif agreement_id.ok_id == 'time_over':
        return 'in_progress', None


@pytest.mark.parametrize('dry_run', (True, False))
@pytest.mark.parametrize('from_root', (True, False))
def test_sync_tags(dry_run, from_root):
    tag_hr = factories.ServiceTagFactory(slug=OEBS_FLAGS[OEBS_HR_FLAG])
    tag_procurement = factories.ServiceTagFactory(slug=OEBS_FLAGS[OEBS_PROCUREMENT_FLAG])
    service = factories.ServiceFactory(use_for_hr=True)
    service_2 = factories.ServiceFactory(
        parent=service,
        use_for_procurement=True,
    )
    service_2.tags.add(tag_hr)
    service_3 = factories.ServiceFactory()
    service_3.tags.add(tag_procurement)

    sync_tags(dry_run=dry_run, root_id=service.id if from_root else None)

    if dry_run:
        assert not service.tags.count()
    else:

        assert service.tags.count() == 1
        assert service.tags.get() == tag_hr

        assert service_2.tags.count() == 1
        assert service_2.tags.get() == tag_procurement
        if from_root:
            assert service_3.tags.count() == 1
            assert service_3.tags.get() == tag_procurement
        else:
            assert not service_3.tags.count()


def test_check_oebs_approve_process():
    factories.OEBSAgreementFactory()
    moving = factories.OEBSAgreementFactory(
        action=ACTIONS.MOVE,
        move_request=factories.ServiceMoveRequestFactory(),
    )

    approver = factories.StaffFactory()
    moving_approved = factories.OEBSAgreementFactory(
        action=ACTIONS.MOVE,
        move_request=factories.ServiceMoveRequestFactory(
            approver_incoming=approver,
            approver_outgoing=approver,
        ),
    )

    factories.OEBSAgreementFactory(
        state=STATES.APPLYING_IN_OEBS,
    )

    finishing = factories.OEBSAgreementFactory(
        ok_id='finish_id',
        issue='TEST-1',
    )

    declined = factories.OEBSAgreementFactory(
        ok_id='decline_id',
        issue='TEST-2',
    )

    not_approved_in_time = factories.OEBSAgreementFactory(
        ok_id='time_over',
        issue='TEST-3',
        start_date=timezone.now().date() - timedelta(days=40)
    )

    with patch('plan.oebs.tasks.check_process.start_oebs_approve_process') as start_oebs_approve_process:
        with patch.object(OkClient, 'get_request_state', mock_get_request_state):
            with patch('plan.oebs.models.close_issue') as mock_close_issue:
                check_oebs_approve_process()

    start_oebs_approve_process.delay.assert_not_called()

    finishing.refresh_from_db()
    assert finishing.state == STATES.APPLYING_IN_OEBS

    moving.refresh_from_db()
    assert moving.state == STATES.OPEN

    moving_approved.refresh_from_db()
    assert moving_approved.state == STATES.OPEN

    declined.refresh_from_db()
    assert declined.state == STATES.DECLINED

    not_approved_in_time.refresh_from_db()
    assert not_approved_in_time.state == STATES.FAILED
    assert not_approved_in_time.error_type == ERRORS.NOT_APPROVED_IN_TIME
    expected_calls = [
        call('TEST-2', comment='Согласование отклонено, изменения не применены'),
        call('TEST-3', comment='Произошла ошибка при применении изменений')
    ]
    mock_close_issue.assert_has_calls(expected_calls)


@override_settings(
    OEBS_HR_APPROVERS=[],
    OEBS_PROCUREMENT_APPROVERS=[],
    OEBS_VS_BU_APPROVERS=[]
)
@pytest.mark.parametrize('flag', [OEBS_HR_FLAG, OEBS_PROCUREMENT_FLAG, OEBS_REVENUE_FLAG])
def test_start_oebs_approve_process_no_approvers(flag, oebs_data):
    """
    Если аппруверов не нашли, то agreement должен быть переведён в статус Ошибка.
    """
    agreement = factories.OEBSAgreementFactory(
        state=STATES.VALIDATED_IN_OEBS,
        attributes={
            flag: True
        }
    )
    with patch('plan.oebs.tasks.start_approve.create_oebs_approve_issue') as create_oebs_approve_issue:
        with patch('plan.oebs.utils.create_issue') as mock_create_issue:
            mocked_issue = Mock()
            mocked_issue.key = 'ABC-123'
            mock_create_issue.return_value = mocked_issue
            start_oebs_approve_process(agreement.id)

    create_oebs_approve_issue.assert_not_called()
    mock_create_issue.assert_called_once()
    agreement.refresh_from_db()
    assert agreement.state == STATES.FAILED
    assert agreement.error_type == ERRORS.NO_APPROVERS
    assert agreement.error_message == {
        'message': f'В вышестоящих сервисах отсутствует сотрудник с ролью {OEBS_MATCHING_FLAGS_AND_ROLES[flag]}'
    }


@override_settings(
    OEBS_HR_APPROVERS=['volozh', 'alimpiev'],
    OEBS_PROCUREMENT_APPROVERS=['smosker']
)
@pytest.mark.parametrize('action', (ACTIONS.CHANGE_FLAGS, ACTIONS.RENAME))
def test_start_oebs_approve_process_success(oebs_data, action):
    issue_key = 'TEST-3'
    agreement = factories.OEBSAgreementFactory(
        action=action,
        state=STATES.VALIDATED_IN_OEBS,
    )
    agreement.service.use_for_hr = True
    agreement.service.tags.add(oebs_data.money_map_tag)
    agreement.service.save()
    service = agreement.service

    if action == ACTIONS.CHANGE_FLAGS:
        agreement.attributes = {'use_for_procurement': True}
        expected_call = (
            f'**Сервис:** (({settings.ABC_URL}/services/{service.slug}/ '
            f'{service.name}))\n**Название:** %%{service.name}%%\n**Название EN:** '
            f'%%{service.name_en}%%\n**Slug:** %%{service.slug}%%\n**ID:** '
            f'%%{service.id}%%\n**Автор запроса:** '
            f'staff:{agreement.requester.login}\n**Тип изменения:** '
            f'%%Синхронизация с OEBS%%\n\n**Текущие значения '
            f'OEBS-Флагов:**\n%%\nСервис используется в OEBS - False\n\nИспользуется для учета железа - False\nИспользуется в HR - True\n'
            f'Используется в Закупках - False\nИспользуется только для группировки - False\n'
            f'%%\n\n**Будущие значения OEBS-Флагов:**\n%%\nСервис используется в OEBS - '
            f'False\n\nИспользуется для учета железа - False\nИспользуется в '
            f'HR - True\nИспользуется в Закупках - True\nИспользуется только для группировки - False\n%%\n\n\n\n**Просмотр дерева сервисов:** {settings.ABC_URL}/embed/oebs/tree/{agreement.id}/\n')
    else:
        agreement.attributes = {
            'ru': service.name,
            'en': service.name_en,
            'new_ru': 'new_name',
            'new_en': 'new_name_eng',
        }
        expected_call = (
            f'**Сервис:** (({settings.ABC_URL}/services/{service.slug}/ '
            f'{service.name}))\n**Название:** %%{service.name}%%\n**Название EN:** '
            f'%%{service.name_en}%%\n**Slug:** %%{service.slug}%%\n**ID:** '
            f'%%{service.id}%%\n**Автор запроса:** '
            f'staff:{agreement.requester.login}\n**Тип изменения:** '
            f'%%Переименование%%\n\n**Текущие значения '
            f'OEBS-Флагов:**\n%%\nСервис используется в OEBS - False\n\nИспользуется для учета железа - False\nИспользуется в HR - True\n'
            f'Используется в Закупках - False\nИспользуется только для группировки - False\n'
            f'%%\n\n\n\n**Новое название:** %%new_name%%\n**Новое название EN:** %%new_name_eng%%'
            f'\n\n**Просмотр дерева сервисов:** {settings.ABC_URL}/embed/oebs/tree/{agreement.id}/\n')
    agreement.save()

    cassette_name = 'ok/test_create_success.json'
    with patch('plan.oebs.utils.change_state') as mock_change_state:
        with patch('plan.oebs.utils.create_issue') as mock_create_issue:
            with patch('plan.common.utils.ok.create_comment') as mock_create_comment:
                with vcr_test().use_cassette(cassette_name):
                    mock_create_issue.return_value = StartrekIssue(issue_key)
                    start_oebs_approve_process(agreement.id)

    mock_create_issue.assert_called_once_with(
        queue=settings.OEBS_APPROVE_QUEUE,
        summary=f'Согласование изменений в {service.name}',
        description=expected_call,
        createdBy=agreement.requester.login,
        components=settings.OEBS_TICKET_COMPONENTS,
    )
    mock_change_state.assert_called_once_with(key='TEST-3', transition='need_info')
    mock_create_comment.assert_called_once_with(
        'TEST-3',
        ('{{iframe src="https://ok.yandex-team.ru/approvements/9b8ec907-f604-4302-852d-7105766e4a1e'
         '?_embedded=1" frameborder=0 width=100% '
         'height=400px scrolling=no}}')
    )
    agreement.refresh_from_db()
    assert agreement.state == STATES.APPROVING
    assert agreement.issue == issue_key
    assert agreement.ok_id == '9b8ec907-f604-4302-852d-7105766e4a1e'


def test_start_oebs_approve_process_with_money_map(oebs_data):
    issue_key = 'TEST-3'
    agreement = factories.OEBSAgreementFactory(
        action=ACTIONS.MOVE,
        state=STATES.VALIDATED_IN_OEBS,
        move_request=factories.ServiceMoveRequestFactory()
    )
    agreement.service.use_for_hr = True
    agreement.service.tags.add(oebs_data.money_map_tag)
    agreement.service.save()
    service = agreement.service
    role = factories.RoleFactory(code=settings.MONEY_MAP_ROLE_CODE)
    member = factories.ServiceMemberFactory(
        service=service,
        role=role,
    )
    resource_type = factories.ResourceTypeFactory(code=settings.MONEY_MAP_RESOURCE_CODES[0])
    factories.ServiceResourceFactory(
        type=resource_type,
        service=service,
        state=ServiceResource.GRANTED,
    )

    with patch('plan.oebs.utils.change_state'):
        with patch('plan.oebs.utils.create_issue') as mock_create_issue:
            with patch('plan.oebs.tasks.start_approve.get_oebs_approvers') as mock_get_approvers:
                mock_get_approvers.return_value = ['smosker']
                with patch('plan.oebs.tasks.start_approve.OkClient'):
                    with patch('plan.common.utils.ok.create_comment'):
                        with patch('plan.oebs.utils.create_comment') as mock_create_comment:
                            mock_create_issue.return_value = StartrekIssue(issue_key)
                            start_oebs_approve_process(agreement.id)

    mock_create_comment.assert_called_once_with(
        comment_text=('Обращаем также внимание, что к сервису привязаны рекламные '
                      'площадки, их можно посмотреть тут: https://{}/services/{}/resources/?view=consuming&layout=table&supplier={}&type={}\nВ '
                      'случае переименования сервиса они все автоматически будут '
                      'продолжать быть к нему привязанными,\nтолько в отчетностях '
                      'будет другое название\n').format(settings.ABC_HOST, service.slug, resource_type.supplier_id, resource_type.id),
        key='TEST-3',
        summonees=[member.staff.login]
    )


def test_start_oebs_approve_process_ok_id_and_issue_exists(oebs_data):
    agreement = factories.OEBSAgreementFactory(
        action=ACTIONS.CHANGE_FLAGS,
        state=STATES.VALIDATED_IN_OEBS,
        attributes={'use_for_procurement': True},
        issue='TEST-3',
        ok_id='test',
    )
    agreement.service.use_for_hr = True
    agreement.service.save()

    factories.ServiceMemberFactory(
        role=oebs_data.role_hr,
        service=agreement.service,
        staff=oebs_data.staff
    )
    factories.ServiceMemberFactory(
        role=oebs_data.role_procurement,
        service=agreement.service,
        staff=oebs_data.staff
    )

    with patch('plan.common.utils.ok.create_comment') as mock_create_comment:
        start_oebs_approve_process(agreement.id)

    mock_create_comment.assert_called_once_with(
        'TEST-3',
        ('{{iframe src="https://ok.yandex-team.ru/approvements/test'
         '?_embedded=1" frameborder=0 width=100% '
         'height=400px scrolling=no}}')
    )
    agreement.refresh_from_db()
    assert agreement.state == STATES.APPROVING


@pytest.mark.parametrize('dormant_oebs_service_agreement', [ACTIONS.MOVE], indirect=True)
def test_start_oebs_approve_process_notify_only(dormant_oebs_service_agreement):
    agreement = dormant_oebs_service_agreement
    start_oebs_approve_process(agreement.id)
    agreement.refresh_from_db()
    assert agreement.state == STATES.APPLYING_IN_OEBS
    agreement.state = STATES.APPLIED_IN_OEBS
    agreement.save()
    with patch('plan.services.tasks.rename_service') as mock_rename_task:
        with patch('plan.oebs.utils.update_issue') as mock_update_issue:
            with patch('plan.oebs.utils.change_state'):
                with patch('plan.oebs.models.change_state'):
                    with patch('plan.oebs.models.create_comment'):
                        with patch('plan.oebs.utils.create_issue') as mock_create_issue:
                            mocked_issue = Mock()
                            mocked_issue.key = 'ABC-123'
                            mock_create_issue.return_value = mocked_issue
                            finish_oebs_approve_process(
                                agreement_id=agreement.id,
                                leaf_oebs_id='some_leaf',
                                parent_oebs_id='some_group',
                            )

    mock_create_issue.assert_not_called()
    mock_update_issue.assert_not_called()
    mock_rename_task.apply_async.assert_not_called()


@pytest.mark.parametrize(('action', 'request_type', 'model', 'task'), [
    (ACTIONS.MOVE, 'move_request', factories.ServiceMoveRequestFactory, 'plan.services.tasks.move_service'),
    (ACTIONS.CLOSE, 'close_request', factories.ServiceCloseRequestFactory, 'plan.services.tasks.close_service'),
    (ACTIONS.DELETE, 'delete_request', factories.ServiceDeleteRequestFactory, 'plan.services.tasks.delete_service'),
])
def test_finish_oebs_approve_process_ok(action, request_type, model, task, oebs_data):
    request = model()
    params = {
        'service': request.service,
        'action': action,
        request_type: request,
        'state': STATES.APPLIED_IN_OEBS,
    }
    agreement = factories.OEBSAgreementFactory(**params)

    with patch(task) as finish_request:
        finish_oebs_approve_process(agreement.id, '10101010', '1010101')

    agreement.refresh_from_db()
    assert agreement.state == STATES.APPLIED

    assert finish_request.apply_async.called

    resource = ServiceResource.objects.filter(
        service=agreement.service,
        type__code=settings.OEBS_PRODUCT_RESOURCE_TYPE_CODE,
        state__in=ServiceResource.ALIVE_STATES,
    ).first()
    assert not resource


def test_finish_oebs_approve_process_ok_change_flags(oebs_data):
    agreement = factories.OEBSAgreementFactory(
        action=ACTIONS.CHANGE_FLAGS,
        state=STATES.APPLIED_IN_OEBS,
        attributes={OEBS_HR_FLAG: True, OEBS_PROCUREMENT_FLAG: False}
    )

    # изначально флаг OEBS_HR_FLAG выкл, OEBS_PROCUREMENT_FLAG вкл
    service = agreement.service
    assert service.tags.count() == 0
    service.use_for_procurement = True
    service.save(update_fields=('use_for_procurement',))
    oebs_procu_tag = ServiceTag.objects.get(slug='oebs_use_for_procurement')
    service.tags.add(oebs_procu_tag)
    assert not service.use_for_hr

    finish_oebs_approve_process(
        agreement_id=agreement.id,
        parent_oebs_id='10101010',
        leaf_oebs_id='1010101',
    )

    agreement.refresh_from_db()
    service.refresh_from_db()
    assert agreement.state == STATES.APPLIED
    assert service.use_for_hr
    assert not service.use_for_procurement
    assert service.tags.count() == 1
    assert service.tags.first().slug == 'oebs_use_for_hr'

    resource = ServiceResource.objects.filter(
        service=service,
        type__code=settings.OEBS_PRODUCT_RESOURCE_TYPE_CODE,
        state__in=ServiceResource.ALIVE_STATES,
    ).first()

    assert resource.attributes['parent_oebs_id'] == '10101010'
    assert resource.attributes['leaf_oebs_id'] == '1010101'
    assert resource.resource.attributes['parent_oebs_id'] == '10101010'
    assert resource.resource.attributes['leaf_oebs_id'] == '1010101'
