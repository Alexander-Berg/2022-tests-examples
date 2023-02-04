from mock import MagicMock, patch

from staff.person.tasks.yandex_disk import ActualizeYandexDisk


@patch('staff.person.effects.base.yenv.type', 'test')
def test_actualize_yandex_disk_task():
    get_tvm_mock = MagicMock(return_value='ticket')

    person = MagicMock(uid='123')

    with patch('staff.person.tasks.yandex_disk.requests.get') as r_get:
        with patch('staff.person.tasks.yandex_disk.tvm2.get_tvm_ticket_by_deploy', get_tvm_mock):
            ActualizeYandexDisk.apply_async(kwargs={'person_uid': person.uid, 'person_login': person.login})
            r_get.assert_called_once()
            get_tvm_mock.assert_called_once()
