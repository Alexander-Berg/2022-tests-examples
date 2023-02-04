import re
from collections import namedtuple
from contextlib import contextmanager
from io import BytesIO
from unittest.mock import MagicMock

import pytest
from django.contrib.admin.models import LogEntry
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import Client

from bcl.banks.registry import Sber, PayPal
from bcl.core.models import Role, RequestLog, Attachment, Contract
from bcl.toolbox.client_sftp import SftpClient
from bcl.toolbox.utils import ZipUtils


class TestAdmin:

    @pytest.fixture(autouse=True)
    def mock_login(self, init_user):
        self.user = init_user(roles=[Role.SUPPORT])

    def setup_method(self, method):
        self.client = Client()

    def get_reponse(self, url, *, post_data: dict = None):

        if post_data is not None:
            response = self.client.post(url, post_data, follow=True)

        else:
            response = self.client.get(url, follow=True)

        try:
            response.text = response.content.decode()

        except UnicodeDecodeError:
            response.text = ''

        return response

    def test_request_log(self):
        RequestLog.add(associate=Sber, data='somedata')
        response = self.get_reponse('/admin/core/requestlog/')
        assert response.status_code == 200
        assert 'Запись журнала взаимодействия' in response.text
        assert 'class="field-associate">Сбербанк</td>' in response.text

    def test_payments(self):
        response = self.get_reponse('/admin/core/payment/')
        assert response.status_code == 200
        assert '<h1>Выберите Платёж для изменения</h1>' in response.text

    def test_requests(self):
        response = self.get_reponse('/admin/core/request/')
        assert response.status_code == 200
        assert '<h1>Выберите Запрос вовне для изменения</h1>' in response.text

    def test_bundles(self):
        response = self.get_reponse('/admin/core/paymentsbundle/')
        assert response.status_code == 200
        assert '<h1>Выберите Пакет платежей для изменения</h1>' in response.text

    def test_users(self):
        response = self.get_reponse('/admin/core/user/')
        assert response.status_code == 200
        assert '<h1>Выберите Пользователь для изменения</h1>' in response.text

    def test_statement_registers(self):
        response = self.get_reponse('/admin/core/statementregister/')
        assert response.status_code == 200
        assert '<h1>Выберите Регистр выписки для изменения</h1>' in response.text

    def test_statement_payments(self):
        response = self.get_reponse('/admin/core/statementpayment/')
        assert response.status_code == 200
        assert '<h1>Выберите Сверочный платёж для изменения</h1>' in response.text

    def test_account(self):
        response = self.get_reponse('/admin/core/account/')
        assert response.status_code == 200
        assert '<h1>Выберите Счёт для изменения</h1>' in response.text

    def test_attaches(self, get_assoc_acc_curr, init_user):

        _, acc, _ = get_assoc_acc_curr(Sber)

        attach = Attachment(
            name='some.dat',
            user=init_user(),
        )
        attach.linked = acc
        attach.save()

        response = self.get_reponse('/admin/core/attachment/')
        assert response.status_code == 200
        text = response.text
        assert '<h1>Выберите Вложение для изменения</h1>' in text
        assert '>some.dat</td>' in text

    def test_contract(self):
        num = '12345678/1234/1234/1/1'
        Contract.objects.create(unumber=num, associate_id=Sber.id)

        response = self.get_reponse('/admin/core/contract/')
        assert response.status_code == 200
        text = response.text
        assert '<h1>Выберите УНК для изменения</h1>' in text
        assert f'>{num}</a>' in text

    def test_log_entry(self, get_assoc_acc_curr):
        _, acc, _ = get_assoc_acc_curr(Sber)

        # Добавим объект, чтобы проверить запись о его изменении.
        response = self.get_reponse(f'/admin/core/account/{acc.id}/change/', post_data={'blocked': 2})
        assert response.status_code == 200
        acc.refresh_from_db()
        assert acc.blocked == 2

        response = self.get_reponse('/admin/admin/log/')
        assert response.status_code == 200
        assert 'fakeacc' in response.text
        assert '<h1>Выберите запись в журнале для изменения</h1>' in response.text

        response = self.get_reponse(f'/admin/admin/log/{LogEntry.objects.first().id}/change/')
        assert response.status_code == 200
        assert (
            '[{&quot;blocked&quot;: &quot;{\&quot;old\&quot;: 0, \&quot;new\&quot;: \&quot;2\&quot;}&quot;}]'
            in response.text)

    def test_tasks(self):
        response = self.get_reponse('/admin/admin/admintaskmodel/')
        assert response.status_code == 200
        assert 'Забор выписок</h3>' in response.text
        assert '<input id="payments_file"' in response.text

    def test_automate_payments(self, build_payment_bundle, response_mock):
        # проверяем работу файла с приложенными номерами платежей

        # формируем словарь для имитации запроса
        data = {
            'associate_automate_payments': ['6'],
            'bundle_number': ['1'],
            'automate_payments': [''],
            'pay_choice': '0',
            'payments_file': SimpleUploadedFile(
                'payments.txt',
                b'BBEA851F-EA8F-4940-B760-BBD9123A14F1\n152B4254-87E5-460F-8897-96940D96B8F2',
                content_type='application/octet-stream'
            )
        }

        build_payment_bundle(
            associate=PayPal,
            payment_dicts=[dict(
                number_src='BBEA851F-EA8F-4940-B760-BBD9123A14F1', number=33, currency_id=443
            )]
        )
        build_payment_bundle(
            associate=PayPal,
            payment_dicts=[dict(
                number_src='152B4254-87E5-460F-8897-96940D96B8F2', number=34, currency_id=443
            )]
        )

        with response_mock(''):
            # правило response_mock не задано, значит поднимется исключение при попытке отправки в PayPal
            # это ускорит прохождение теста
            response = self.client.post(path='/admin/admin/admintaskmodel/', data=data, follow=False)

        content = response.content.decode()

        assert '<td nowrap>BBEA851F-EA8F-4940-B760-BBD9123A14F1</td>' in content
        assert '<td nowrap>152B4254-87E5-460F-8897-96940D96B8F2</td>' in content
        assert '<td nowrap>Выгружен в онлайн</td>' in content
        assert '<td nowrap>К доставке</td>' in content

        # проверяем работу файла с приложенными номерами пакетов

        bundle1 = build_payment_bundle(
            associate=PayPal,
            payment_dicts=[dict(
                number_src='152B4254-87E5-460F-8897-96940D96B8F3', number=35, associate_id=6, currency_id=443
            )]
        )
        bundle2 = build_payment_bundle(
            associate=PayPal,
            payment_dicts=[dict(
                number_src='152B4254-87E5-460F-8897-96940D96B8F4', number=36, associate_id=6, currency_id=443
            )]
        )

        data['payments_file'] = SimpleUploadedFile(
            'bundles.txt',
            f'{bundle1.id}\n{bundle2.id}'.encode(),
            content_type='application/octet-stream'
        )
        data['pay_choice'] = '1'
        with response_mock(''):
            response = self.client.post(path='/admin/admin/admintaskmodel/', data=data, follow=False)
        content = response.content.decode()

        assert '<td nowrap>152B4254-87E5-460F-8897-96940D96B8F3</td>' in content
        assert '<td nowrap>152B4254-87E5-460F-8897-96940D96B8F4</td>' in content
        assert '<td nowrap>Выгружен в онлайн</td>' in content
        assert '<td nowrap>К доставке</td>' in content

    def test_sftp_nav(self, monkeypatch):
        # Список доступных подключений.

        url = '/admin/admin/adminsftpmodel/'

        response = self.get_reponse(url)
        assert '/?alias=unicredit_ya">unicredit_ya' in response.text
        assert 'Записи в журнале' in response.text  # проверка наличия общих элементов страниц

        # Обработка ошибок.
        response = self.get_reponse(f'{url}?alias=unicredit_ya')
        assert any([
            'Ошибка: [Errno -2] Name or service not known' in response.text,  # есть доступ к сети в тестах
            'Temporary failure in name resolution' in response.text  # нет доступа к сети в тестах
        ])

        # Эмулируем список сущностей.
        SftpAttrs = namedtuple('SftpAttrs', ['filename', 'st_mtime', 'st_size', 'st_mode'])

        class Mocker:

            def open(self, *args, **kwargs):
                return BytesIO(b'2345')

            def listdir_iter(self, path):
                return [
                    SftpAttrs('one.txt', 12345, 12345, 123),  # файл
                    SftpAttrs('two.txt', 12345, 12345, 123),  # файл
                    SftpAttrs('subdir', 12345, 12345, 16704),  # директория
                ]

            def chdir(self, path=None):
                pass

            def listdir(self, path='.'):
                return ['one.txt', 'two.txt']

        @contextmanager
        def sftp_connection(self):
            yield Mocker()

        monkeypatch.setattr(SftpClient, 'sftp_connection', sftp_connection)

        response = self.get_reponse(f'{url}?alias=unicredit_ya&path=OUT/XMLACK')

        compact = re.sub(r'\s', '', response.text)

        assert 'Скачатьфайлыоднимархивом' in compact
        assert '?alias=unicredit_ya&path=OUT/XMLACK/one.txt&download=1">one.txt' in compact
        assert (
            '🗀</span><ahref="/admin/admin/adminsftpmodel/'
            '?alias=unicredit_ya&path=OUT/XMLACK/subdir">subdir</a></td><tdclass="text-right"nowrap>12.06KB</td>'
            '<tdnowrap>1970-01-0106:25:45' in compact)

        # Запрашиваем файл.
        response = self.get_reponse(f'{url}?alias=unicredit_ya&path=OUT/XMLACK/one.txt&download=1')
        assert response.content == b'2345'

        # Запрашиваем архив директории.
        response = self.get_reponse(f'{url}?alias=unicredit_ya&path=OUT/XMLACK/&download_files=1')
        assert response.content[0] == 80  # начало zip

    def test_staff_sync(self, monkeypatch, response_mock):

        content = self.client.get(path='/admin/admin/adminstaffsyncmodel/').content.decode()
        assert 'можно запустить процесс синхронизации' in content


        with response_mock(
            'GET https://staff-api.test.yandex-team.ru/v3/persons?'
            '_fields=login,work_phone,name,location.office.contacts.phone&_limit=999&login=testuser -> 200:'
            '{"links": {}, "page": 1, "limit": 999, "result": [{'
            '"work_phone": 1234, "login": "testuser", "location": { "office": { "contacts": { "phone": "70007"}}}, '
            '"name": {"middle": "middle", "has_namesake": false, "first": {"ru": "фёрст", "en": "first"}, '
            '"last": {"ru": "ласт", "en": "last"}, "hidden_middle": false}}], "total": 2, "pages": 1}'
        ):
            content = self.client.post(
                path='/admin/admin/adminstaffsyncmodel/',
                data={},
                follow=False
            ).content.decode()
            assert 'Данные пользователей обновлены' in content

        user = self.user
        user.refresh_from_db()
        assert user.name_en == 'first last'
        assert user.name_ru == 'ласт middle фёрст'
        assert user.telephone == '70007 1234'

    def test_decryptor(self, monkeypatch):

        # Список доступных внешних систем с дешифраторами.
        url = '/admin/admin/admincryptormodel/'

        response = self.get_reponse(url)
        assert '?associate=23">J.P. Morgan' in response.text

        # Форма для заполнения
        response = self.get_reponse(f'{url}?associate=23')
        assert 'Исходные данные' in response.text

        def send(*, decrypt=True, as_file: str = ''):

            post_data = {
                'datasent': '1',
                'action': '1' if decrypt else '2',
                'data': 'some',
            }

            if as_file:

                if as_file.endswith('zip'):
                    contents = ZipUtils.pack({'inner1.my': b'some', 'inner2.data': b'other'})

                else:
                    contents = b'some'

                post_data['data_file'] = SimpleUploadedFile(
                    as_file, contents, content_type='application/octet-stream')

            return self.get_reponse(f'{url}?associate=23', post_data=post_data)

        # Проверка вывода ошибок.
        response = send()
        assert 'Failed to perform GPG decryption' in response.text

        # Имитируем подписание.
        dummy_communicate = MagicMock()
        signed = b'-----BEGIN PGP MESSAGE-----\nencrypted'
        dummy_communicate.return_value = signed, ''
        monkeypatch.setattr('bcl.toolbox.signatures.Popen.communicate', dummy_communicate)

        def assert_fine(response):
            assert 'readonly>-----BEGIN PGP MESSAGE-----\nencrypted<' in response.text
            assert 'readonly>some<' in response.text
            assert 'button class="btn btn-info"' not in response.text

        # Отправка данных из файла.
        assert_fine(send(as_file='afile.my'))

        # Отправка данных из файла-архива.
        processed = ZipUtils.unpack(send(as_file='afile.zip').content)
        assert b'encrypted' in processed['inner1.my']
        assert b'encrypted' in processed['inner2.data']

        # Отправка данных из поля ввода.
        assert_fine(send())

        # Ширование.
        assert_fine(send(decrypt=False))
