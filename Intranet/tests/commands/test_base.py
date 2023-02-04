# coding: utf-8


from freezegun import freeze_time
import mock
import pytest

from django.utils import timezone

from idm.core.management.base import IdmBaseCommand
from idm.core.models import CommandTimestamp

pytestmark = pytest.mark.django_db


class TestCommand(IdmBaseCommand):
    def idm_handle(self, *args, **options):
        pass


def test_command_timestamp_with_success():
    command = TestCommand()
    now = timezone.now()

    # Проверим, что при успешном выполнении таски создается объект и ему проставляется start / finish
    with freeze_time(now):
        command.handle(use_block_lock=False)
    assert CommandTimestamp.objects.count() == 1
    command_stat = CommandTimestamp.objects.get()
    assert command_stat.command == 'commands.test_base'  # 'commands.' + <имя файла>  (== имя команды в django)
    assert command_stat.last_success_start == now

    # Проверим, что при повторном запуске команды новый объект не создается, а обновляется уже существующий
    other_time = now + timezone.timedelta(hours=1)
    with freeze_time(other_time):
        command.handle(use_block_lock=False)
    assert CommandTimestamp.objects.count() == 1
    command_stat = CommandTimestamp.objects.get()
    assert command_stat.last_success_start == other_time


def test_command_timestamp_with_exception():
    command = TestCommand()
    first_start = timezone.now() - timezone.timedelta(minutes=5)
    first_finish = first_start + timezone.timedelta(minutes=5)
    # Проверим, что при ошибке во время выполнении таски объект не создается
    with mock.patch.object(command.__class__, 'idm_handle', side_effect=ValueError()), pytest.raises(ValueError):
        command.handle(use_block_lock=False)
    assert CommandTimestamp.objects.count() == 0

    CommandTimestamp.objects.create(
        command='commands.test_base',  # 'commands.' + <имя файла>  (== имя команды в django)
        last_success_start=first_start,
        last_success_finish=first_finish
    )

    # Проверим, что при ошибке данные не обновятся
    other_time = first_start + timezone.timedelta(hours=1)
    with freeze_time(other_time), mock.patch.object(command.__class__, 'idm_handle', side_effect=ValueError()), pytest.raises(ValueError):
        command.handle(use_block_lock=False)
    assert CommandTimestamp.objects.count() == 1
    command_stat = CommandTimestamp.objects.get()
    assert command_stat.last_success_start == first_start
    assert command_stat.last_success_finish == first_finish
