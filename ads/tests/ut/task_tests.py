# coding=utf-8

import pytest
import json

from ads.quality.phf.phf_direct_loader.lib.extensions.error_handling import PHRASE_FACTORY_ERROR_CODE
import ads.quality.phf.phf_direct_loader.lib.campaign_generation.expander as expander
from ads.quality.phf.phf_direct_loader.lib.campaign_generation.event_aggregation import SimpleEventAggregator
from ads.quality.phf.phf_direct_loader.lib.modules.templates.controllers.errors import (
    AnotherTaskInProgressError,
    UploadingNonDraft,
    PaginationError,
    OperationOnUnuploadedTemplateError
)

from ads.quality.phf.phf_direct_loader.lib.modules.templates.controllers.task import (
    DirectTask,
    TemplateUploadTask,
    ModerateTask,
    save_direct_errors_to_db
)

from ads.quality.phf.phf_direct_loader.lib.modules.templates.models.task import TaskStates, TaskDirectErrorDBO
from ads.quality.phf.phf_direct_loader.lib.modules.templates.models.template import CampaignTemplateDBO
from ads.quality.phf.phf_direct_loader.tests.test_helpers import (
    BaseTestCase,
    fake_direct_api,
    TEST_CLIENT,
    CORRECT_TEMPLATES_PATH
)


class TestTask(DirectTask):
    human_readable_name = u"Тестовый таск"

    def __init__(self, client_id, template_id, extra_param):
        super(TestTask, self).__init__(client_id, template_id)
        self.extra_param = extra_param

    def _get_arg_dict(self):
        return {
            'client_id': self.client_id,
            'template_id': self.template_id,
            'extra_param': self.extra_param
        }

    def _commit_task(self, template_dbo):
        pass

    def _execute(self, template_dbo, client_dbo):
        return TaskStates.Succeed, SimpleEventAggregator()


class ExceptionTask(TestTask):
    def _execute(self, template_dbo, client_dbo):
        raise ValueError


class SomeErrorsTask(TestTask):
    def _execute(self, template_dbo, client_dbo):
        return TaskStates.CompletedWithErrors, SimpleEventAggregator()


def test_serialization():
    task = TestTask(1, 2, 3)
    data = task.serialize()
    deserialized_task = DirectTask.deserialize(data)

    assert deserialized_task.client_id == task.client_id
    assert deserialized_task.template_id == task.template_id
    assert deserialized_task.extra_param == task.extra_param

    assert type(task) == type(deserialized_task)


class TemplateTaskStateTests(BaseTestCase):
    @staticmethod
    def _get_template_by_id(template_id):
        return CampaignTemplateDBO.query.filter(CampaignTemplateDBO.id == template_id).first()

    def _set_up_test_task(self, task_class=TestTask):
        template_id = self._get_test_template_id()

        return template_id, self._make_task_on_template(template_id, task_class)

    def _make_task_on_template(self, template_id, task_class):
        template_dbo = self._get_template_by_id(template_id)

        return task_class(client_id=template_dbo.client_id,
                          template_id=template_dbo.id,
                          extra_param=1)

    def test_state_is_draft_on_creation(self):
        template_dbo = self._get_template_by_id(self._get_test_template_id())

        assert not template_dbo.was_uploaded
        assert template_dbo.direct_campaign_ids is None
        assert template_dbo.last_task_status is TaskStates.Draft.value
        assert template_dbo.last_task_name is None

    def test_last_task_name_on_commit(self):
        template_id, task = self._set_up_test_task()

        task.commit()

        template_dbo = self._get_template_by_id(template_id)
        assert template_dbo.last_task_name == task.human_readable_name

    def test_in_progress_on_commit(self):
        template_id, task = self._set_up_test_task()
        task.commit()
        template_dbo = self._get_template_by_id(template_id)
        assert template_dbo.last_task_status == TaskStates.InProgress.value

    def test_done_after_execute(self):
        template_id, task = self._set_up_test_task()
        task.commit()
        task.execute()

        template_dbo = self._get_template_by_id(template_id)
        assert template_dbo.last_task_status == TaskStates.Succeed.value

    def test_failed_after_uncommit_w_failure(self):
        template_id, task = self._set_up_test_task()
        task.commit()
        task.uncommit_with_failure()

        template_dbo = self._get_template_by_id(template_id)
        assert template_dbo.last_task_status == TaskStates.Failed.value

    def test_failed_on_exception(self):
        template_id, task = self._set_up_test_task(ExceptionTask)
        task.commit()
        try:
            task.execute()
        except:
            pass

        template_dbo = self._get_template_by_id(template_id)
        assert template_dbo.last_task_status == TaskStates.Failed.value

    def test_completed_with_errors(self):
        template_id, task = self._set_up_test_task(SomeErrorsTask)
        task.commit()
        task.execute()

        template_dbo = self._get_template_by_id(template_id)
        assert template_dbo.last_task_status == TaskStates.CompletedWithErrors.value

    def test_already_in_queue_error(self):
        template_id, task = self._set_up_test_task()
        task.commit()
        other_task = self._make_task_on_template(template_id, TestTask)

        with pytest.raises(AnotherTaskInProgressError):
            other_task.commit()

    def task_successfully_commited_after_previous_executed(self):
        template_id, task = self._set_up_test_task()
        other_task = self._make_task_on_template(template_id, TestTask)

        task.commit()
        task.execute()
        other_task.commit()

    def _build_upload_task(self):
        template_id = self._get_test_template_id()
        client_id = TEST_CLIENT['direct_id']

        task = TemplateUploadTask(client_id=client_id,
                                  template_id=template_id)
        return template_id, task

    def test_campaign_ids_on_upload_task(self):
        template_id, task = self._build_upload_task()

        task.commit()
        with fake_direct_api() as api:
            task.execute()
            template = self._get_template_by_id(template_id)
            assert json.loads(template.direct_campaign_ids) == api.get_campaign_ids()

    def test_error_on_double_upload(self):
        template_id, upload_task = self._build_upload_task()
        upload_task.commit()

        with fake_direct_api():
            upload_task.execute()

        with pytest.raises(UploadingNonDraft):
            upload_task.commit()

    def test_template_has_uploaded_status_after_upload(self):
        template_id, upload_task = self._build_upload_task()
        upload_task.commit()

        with fake_direct_api():
            upload_task.execute()

        template_dbo = self._get_template_by_id(template_id)
        uploaded = template_dbo.was_uploaded
        assert uploaded

    def _make_errors(self, count):
        template_id, task = self._set_up_test_task()
        test_errors = [(expander.AdGroup(1, 'test', 100, 1, 1), expander.DirectError(2, 'test', 'test'))
                       for _ in xrange(count)]
        save_direct_errors_to_db(task, test_errors)
        return task, test_errors

    def test_store_errors(self):
        task, test_errors = self._make_errors(1)

        db_error = TaskDirectErrorDBO.query.filter(TaskDirectErrorDBO.template_id == task.template_id).first()
        obj, error = test_errors[0]

        assert task.template_id == db_error.template_id

        assert obj.human_readable_name == db_error.associated_object_description
        assert list(obj.user_string_tuple_repr()) == json.loads(db_error.associated_object_strings)

        assert db_error.error_code == error.code
        assert db_error.error_message == error.message
        assert db_error.error_details == error.details
        assert db_error.task_name == task.human_readable_name

    def test_no_errors_pagination_reutrns_empty_list(self):
        template_id = self._get_test_template_id()
        resp = self.client.get(CORRECT_TEMPLATES_PATH + "/{}/task_errors?per_page=10&page=0".format(template_id))
        assert resp.status_code == 200
        assert json.loads(resp.data)['items'] == []

    def test_paging_returns_correct_amount_of_items(self):
        test_error_count = 25
        task, _ = self._make_errors(test_error_count)

        error_endpoint = CORRECT_TEMPLATES_PATH + "/{}/task_errors?per_page=10".format(task.template_id)

        retrived_errors = []
        resp = self.client.get(error_endpoint + "&page=0").data
        resp = json.loads(resp)

        total_pages = resp['pages']
        retrived_errors += resp['items']
        for i in range(1, total_pages):
            resp = self.client.get(error_endpoint + "&page={}".format(i)).data
            resp = json.loads(resp)

            retrived_errors += resp['items']

        assert len(retrived_errors) == test_error_count

    def test_paging_error_on_incorrect_page(self):
        test_error_count = 25
        task, _ = self._make_errors(test_error_count)

        error_endpoint = CORRECT_TEMPLATES_PATH + "/{}/task_errors?per_page=10".format(task.template_id)

        resp = self.client.get(error_endpoint + "&page=0").data
        resp = json.loads(resp)

        total_pages = resp['pages']

        resp = self.client.get(error_endpoint + "&page={}".format(total_pages + 1))
        assert resp.status_code == PHRASE_FACTORY_ERROR_CODE
        assert json.loads(resp.data)['error_code'] == PaginationError.code

    def _build_moderate_task(self):
        template_id = self._get_test_template_id()
        client_id = TEST_CLIENT['direct_id']

        task = ModerateTask(client_id=client_id,
                            template_id=template_id)
        return template_id, task

    def test_error_on_moderating_unuploaded_template(self):
        _, task = self._build_moderate_task()
        with pytest.raises(OperationOnUnuploadedTemplateError):
            task.commit()
