# -*- coding: utf-8 -*-
import datetime
import copy
from contextlib import contextmanager

from mock import MagicMock, patch
import pytest

from balance.processors.oebs import handle_acts_invoice_export_exception
from balance import exc
from balance.constants import ExportState


@pytest.fixture
def fake_invoice_export():
    invoice_export = MagicMock()
    invoice_export.state = ExportState.exported
    invoice_export.export_dt = datetime.datetime.now()
    return invoice_export


@pytest.fixture
def fake_session(fake_invoice_export):
    @contextmanager
    def fake_context_manager():
        yield

    session = MagicMock()
    session.begin = fake_context_manager
    session.fresh_invoice_export = copy.copy(fake_invoice_export)

    def get_invoice_export(*args, **kwargs):
        return session.fresh_invoice_export

    session.query().filter_by().with_lockmode().first.side_effect = get_invoice_export

    return session


@patch('balance.processors.oebs.getApplication')
def test_invoice_export_is_none(get_application_mock, fake_session):
    """Сообщение ошибки содержит b"Не удалось найти счет, к которому привязан этот акт",
    до и после выгрузки акта invoice_export is None,
    вызываем  enqueue, кидаем исключение exc.DEFERRED_ERROR
    """
    get_application_mock().new_session.return_value = fake_session
    fake_session.fresh_invoice_export = None

    error = MagicMock()
    error.args[0].message = u"Не удалось найти счет, к которому привязан этот акт".encode("utf-8")

    # Функция должна бросить ошибку
    with pytest.raises(exc.DEFERRED_ERROR):
        handle_acts_invoice_export_exception(MagicMock(), None)

    # Должны были перепроставить счет
    fake_session.query().getone().enqueue.assert_called_once()


@patch('balance.processors.oebs.getApplication')
def test_invoice_exported_and_state_was_not_changed(get_application_mock, fake_session, fake_invoice_export):
    """Сообщение ошибки содержит u"Не удалось найти счет, к которому привязан этот акт",
    счет был выгружен, состояние счета не поменялось во время выгрузки акта.
    вызываем export, кидаем исключение exc.DEFERRED_ERROR
    """
    get_application_mock().new_session.return_value = fake_session

    error = MagicMock()
    error.args[0].message = u"Не удалось найти счет, к которому привязан этот акт".encode("utf-8")

    # Функция должна бросить ошибку
    with pytest.raises(exc.DEFERRED_ERROR):
        handle_acts_invoice_export_exception(MagicMock(), fake_invoice_export)

    # Экспортируем счет
    fake_session.fresh_invoice_export.export.assert_called_once()


@patch('balance.processors.oebs.getApplication')
@pytest.mark.parametrize("field_to_change", ["date", "state"])
def test_invoice_state_was_changed(get_application_mock, fake_session, fake_invoice_export, field_to_change):
    """Сообщение в ошибке содержит u"Не удалось найти счет, к которому привязан этот акт",
    счет был выгружен, состояние счета поменялось во время выгрузки акта,
    не вызываем enqueue, не вызываем export, не кидаем исключений.
    """
    get_application_mock().new_session.return_value = fake_session

    if field_to_change == "date":
        fake_session.fresh_invoice_export.export_dt = datetime.datetime.now() + datetime.timedelta(minutes=1)
    elif field_to_change == "state":
        fake_session.fresh_invoice_export.state = ExportState.enqueued

    error = MagicMock()
    error.args[0].message = u"Не удалось найти счет, к которому привязан этот акт".encode("utf-8")

    # Нет исключений
    handle_acts_invoice_export_exception(MagicMock(), fake_invoice_export)
    # Не пытались перевыгрузить счет
    fake_session.query().with_for_update().getone().enqueue.assert_not_called()
    fake_session.fresh_invoice_export.export.assert_not_called()


@patch('balance.processors.oebs.getApplication')
@pytest.mark.parametrize("export_state", [ExportState.enqueued, ExportState.failed])
def test_invoice_was_not_exported_state_was_not_changed(
    get_application_mock,
    fake_session,
    fake_invoice_export,
    export_state,
):
    """Сообщение ошибки содержит u"Не удалось найти счет, к которому привязан этот акт",
    счет не был выгружен, состояние счета не поменялось во время выгрузки акта,
    не вызываем enqueue, не вызываем export, не кидаем исключений
    """
    get_application_mock().new_session.return_value = fake_session

    error = MagicMock()
    error.args[0].message = u"Не удалось найти счет, к которому привязан этот акт".encode("utf-8")

    # Состояние счета != Выгружен
    fake_invoice_export.state = export_state

    # Нет исключений
    handle_acts_invoice_export_exception(MagicMock(), fake_invoice_export)
    # Не пытались перевыгрузить счет
    fake_session.query().with_for_update().getone().enqueue.assert_not_called()
    fake_session.fresh_invoice_export.export.assert_not_called()


@patch('balance.processors.oebs.getApplication')
def test_invoice_export_became_not_none_after_act_export_failure(
    get_application_mock,
    fake_session,
):
    """Сообщение ошибки содержит u"Не удалось найти счет, к которому привязан этот акт",
    перед попыткой выгрузить акт счета не было в t_export, после попытки выгрузить
    акт состояние выгрузки счета Exported,
    не вызываем enqueue, не вызываем export, не кидаем исключений
    """
    get_application_mock().new_session.return_value = fake_session

    error = MagicMock()
    error.args[0].message = u"Не удалось найти счет, к которому привязан этот акт".encode("utf-8")

    # Нет исключений
    handle_acts_invoice_export_exception(MagicMock(), None)
    # Не пытались перевыгрузить счет
    fake_session.query().with_for_update().getone().enqueue.assert_not_called()
    fake_session.fresh_invoice_export.export.assert_not_called()
