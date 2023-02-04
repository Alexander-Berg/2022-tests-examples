# -*- coding: utf-8 -*-
import hashlib
import responses
import os

from bson import ObjectId
from datetime import datetime
from django.conf import settings
from django.contrib.auth.models import Permission
from django.contrib.contenttypes.models import ContentType
from django.core.files.base import ContentFile
from django.test import TestCase, override_settings
from guardian.shortcuts import assign_perm
from unittest.mock import patch, ANY

from events.accounts.helpers import YandexClient
from events.common_storages.factories import ProxyStorageModelFactory
from events.common_storages.models import ProxyStorageModel
from events.common_storages.storage import MdsClient, APIError
from events.surveyme.factories import SurveyFactory, SurveyGroupFactory
from events.surveyme.models import Survey, SurveyGroup


class TestFileExistance(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def test_not_exists(self):
        path = '/hello/world.txt'
        with patch.object(MdsClient, 'get') as mock_get:
            response = self.client.get(f'/v1/files/?path={path}')

        self.assertEqual(response.status_code, 404)
        mock_get.assert_not_called()

    def test_not_exists_with_login(self):
        self.client.login_yandex()
        path = '/hello/world.txt'
        with patch.object(MdsClient, 'get') as mock_get:
            response = self.client.get(f'/v1/files/?path={path}')

        self.assertEqual(response.status_code, 404)
        mock_get.assert_not_called()


class TestFileWithoutContentTypeObject(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.path = '/hello/world.txt'
        self.file_meta = ProxyStorageModelFactory(path=self.path)

    def test_client_not_authorized(self):
        with patch.object(MdsClient, 'get') as mock_get:
            mock_get.return_value = b'some binary data'
            response = self.client.get(f'/v1/files/?path={self.path}')

        self.assertEqual(response.status_code, 404)
        mock_get.assert_not_called()

    def test_client_authorized(self):
        user = self.client.login_yandex()
        self.file_meta.user = user
        self.file_meta.save()
        with patch.object(MdsClient, 'get') as mock_get:
            mock_get.return_value = b'some binary data'
            response = self.client.get(f'/v1/files/?path={self.path}')

        self.assertEqual(response.status_code, 200)
        mock_get.assert_called_with(self.path)


@override_settings(FRONTENDS_BY_AUTH_KEY={'key': {'auth_key': 'key', 'name': 'events'}})
class TestFilesViewUpload(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()
        self.headers = {
            'HTTP_X_FRONTEND_AUTHORIZATION': 'key'
        }
        self.file = ContentFile('hello, world!', name='hello.txt')
        self.sha256_of_file = self.get_sha256(self.file)
        self.survey = SurveyFactory(slug='testit')

    def get_sha256(self, content):
        content.seek(0)
        m = hashlib.sha256()
        m.update(content.read().encode('utf-8'))
        m.update(content.name.encode('utf-8'))
        content.seek(0)
        return m.hexdigest()

    def test_should_return_403_if_not_authorized_frontend(self):
        file = ContentFile('hello, world!', name='hello.txt')
        response = self.client.post('/v1/files/', {'file': file})

        msg = 'Если клиент не авторизован - нужно вернуть 403'
        self.assertEqual(response.status_code, 403, msg=msg)

    def test_should_return_file_id_if_authorized_frontend(self):
        self.assertEqual(ProxyStorageModel.objects.count(), 0)
        with patch('events.common_storages.storage.generate_code') as mock_generate:
            mock_generate.return_value = 'b9j2a642b859r370557ab890943c999r'
            with patch.object(MdsClient, 'upload') as mock_save:
                mock_save.return_value = '401/{}_{}'.format('b9j2a642b859r370557ab890943c999r', 'hello.txt')
                response = self.client.post('/v1/files/', {'file': self.file}, **self.headers)

        msg = 'Если клиент авторизован - нужно вернуть 200 и словарь, содержащий id файла'
        self.assertEqual(response.status_code, 200, msg=msg)
        self.assertEqual(response.data['id'], self.sha256_of_file, msg=msg)

        try:
            ProxyStorageModel.objects.get_by_sha256(self.sha256_of_file)
            raised = False
        except ProxyStorageModel.DoesNotExist:
            raised = True
        self.assertEqual(ProxyStorageModel.objects.count(), 1)
        self.assertFalse(raised)

    def test_should_set_correct_expire_timeout(self):
        with patch.object(MdsClient, 'upload') as mock_upload:
            response = self.client.post(
                f'/v1/files/?survey={self.survey.pk}',
                {'file': self.file}, **self.headers,
            )
            self.assertEqual(response.status_code, 200)
            meta_data = ProxyStorageModel.objects.get(sha256=response.data['id'])
            self.assertEqual(meta_data.survey_id, self.survey.pk)
            self.assertEqual(meta_data.user_id, self.user.pk)
            mock_upload.assert_called_once_with(ANY, ANY, expire=settings.DEFAULT_EXPIRE_TIMEOUT)

    def test_shouldnt_set_user_for_anonymous(self):
        self.client.remove_cookie()
        with patch.object(MdsClient, 'upload') as mock_upload:
            response = self.client.post(
                f'/v1/files/?survey={self.survey.pk}',
                {'file': self.file}, **self.headers,
            )
            self.assertEqual(response.status_code, 200)
            meta_data = ProxyStorageModel.objects.get(sha256=response.data['id'])
            self.assertEqual(meta_data.survey_id, self.survey.pk)
            self.assertEqual(meta_data.user_id, None)
            mock_upload.assert_called_once_with(ANY, ANY, expire=settings.DEFAULT_EXPIRE_TIMEOUT)

    def test_should_set_correct_id_for_survey_slug(self):
        with patch.object(MdsClient, 'upload') as mock_upload:
            response = self.client.post(
                f'/v1/files/?survey={self.survey.slug}',
                {'file': self.file}, **self.headers,
            )
            self.assertEqual(response.status_code, 200)
            meta_data = ProxyStorageModel.objects.get(sha256=response.data['id'])
            self.assertEqual(meta_data.survey_id, self.survey.pk)
            self.assertEqual(meta_data.user_id, self.user.pk)
            mock_upload.assert_called_once_with(ANY, ANY, expire=settings.DEFAULT_EXPIRE_TIMEOUT)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_should_set_correct_expire_timeout_for_biz(self):
        with patch.object(MdsClient, 'upload') as mock_upload:
            response = self.client.post('/v1/files/', {'file': self.file}, **self.headers)
            self.assertEqual(response.status_code, 200)
            meta_data = ProxyStorageModel.objects.get(sha256=response.data['id'])
            self.assertEqual(meta_data.survey_id, None)
            self.assertEqual(meta_data.user_id, self.user.pk)
            mock_upload.assert_called_once_with(ANY, ANY, expire=settings.BUSINESS_EXPIRE_TIMEOUT)

    def test_shouldnt_set_expire_timeout(self):
        with patch.object(MdsClient, 'upload') as mock_upload:
            mock_upload.return_value = '123/test.txt'
            response = self.client.post(
                f'/admin/api/v2/files/?survey={self.survey.pk}',
                {'file': self.file}, **self.headers,
            )
            self.assertEqual(response.status_code, 200)
            meta_data = ProxyStorageModel.objects.get(path=response.data['path'])
            self.assertEqual(meta_data.survey_id, self.survey.pk)
            self.assertEqual(meta_data.user_id, self.user.pk)
            mock_upload.assert_called_once_with(ANY, ANY, expire=None)

    @override_settings(IS_BUSINESS_SITE=True)
    def test_shouldnt_set_expire_timeout_for_biz(self):
        with patch.object(MdsClient, 'upload') as mock_upload:
            response = self.client.post('/admin/api/v2/files/', {'file': self.file}, **self.headers)
            self.assertEqual(response.status_code, 200)
            meta_data = ProxyStorageModel.objects.get(path=response.data['path'])
            self.assertEqual(meta_data.survey_id, None)
            self.assertEqual(meta_data.user_id, self.user.pk)
            mock_upload.assert_called_once_with(ANY, ANY, expire=None)

    def test_should_return_400_if_file_empty_or_without_name(self):
        response = self.client.post('/v1/files/', {'file': ContentFile('', name='hello.txt')}, **self.headers)
        msg = 'Если файл пустой - нужно вернуть 400'
        self.assertEqual(response.status_code, 400, msg=msg)

        response = self.client.post('/v1/files/', {'file': ContentFile('', name='')}, **self.headers)
        msg = 'Если файл пустой - нужно вернуть 400'
        self.assertEqual(response.status_code, 400, msg=msg)

    def test_should_fallback_to_default_file_name(self):
        with patch('events.common_storages.storage.generate_code') as mock_generate:
            mock_generate.return_value = 'b9j2a642b859b370557ab890943c999j'
            with patch.object(MdsClient, 'upload') as mock_save:
                mock_save.return_value = '401/{}_{}'.format('b9j2a642b859b370557ab890943c999j', 'hello')

                response = self.client.post('/v1/files/', {'file': ContentFile('hello', name='')}, **self.headers)
        self.assertEqual(response.data['name'], 'file')
        self.assertEqual(response.status_code, 200)

    @override_settings(MDS_MAX_FILE_SIZE=10)
    def test_should_return_400_if_uploaded_file_too_large(self):
        content_file = ContentFile('hello, world!', name='hello.txt')
        response = self.client.post('/v1/files/', {'file': content_file}, **self.headers)
        self.assertEqual(response.status_code, 400)

    @responses.activate
    @patch('events.common_storages.storage.generate_code')
    def test_should_return_expiration_date(self, mock_generate):
        mock_generate.return_value = '12345'
        responses.add(
            responses.POST,
            'https://storage-int.mdst.yandex.net:1443/upload-forms/12345_file',
            content_type='text/xml',
            body='''<?xml version="1.0" encoding="utf-8"?>
<post key="402/12345_file" />''',
        )
        responses.add(
            responses.HEAD,
            'https://storage.mdst.yandex.net/get-forms/402/12345_file',
            headers={
                'x-mds-expiration-date': 'Thu, 10 Jun 2021 08:56:56 GMT',
            },
        )

        with patch.object(MdsClient, '_get_tvm2_ticket', return_value='123'):
            response = self.client.post('/v1/files/', {'file': ContentFile('hello', name='')}, **self.headers)
        self.assertEqual(response.status_code, 200)

        file_meta = ProxyStorageModel.objects.get_by_sha256(response.data['id'])
        self.assertEqual(file_meta.namespace, 'forms')
        proxy_storage = file_meta.get_original_storage()

        expiration_date = proxy_storage.get_expiration_date(file_meta.path)
        self.assertTrue(isinstance(expiration_date, datetime))


class TestDownloadFileFromMds(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.path = '/hello/world.txt'
        self.user = self.client.login_yandex()
        self.meta_info = ProxyStorageModelFactory(path=self.path, user=self.user)

    def test_should_return_404_on_mds_readerror(self):
        with patch.object(MdsClient, 'get') as mds_get:
            mds_get.side_effect = APIError
            response = self.client.get(f'/v1/files/?path={self.path}')
        mds_get.assert_called_once_with(self.path)
        self.assertEqual(response.status_code, 404)


class TestFileContentTypes(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()

    def test_svg(self):
        path = '/image/test.svg'
        ProxyStorageModelFactory(path=path, user=self.user)
        file_content = b'<svg></svg>'

        with patch.object(MdsClient, 'get') as mds_get:
            mds_get.return_value = file_content
            response = self.client.get(f'/v1/files/?path={path}')

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['Content-Type'], 'application/octet-stream')
        self.assertTrue(response.has_header('Content-Disposition'))
        self.assertEqual(response['Content-Disposition'], f'attachment; filename="{os.path.basename(path)}"')
        self.assertEqual(response['X-Content-Type-Options'], 'nosniff')
        self.assertEqual(response.content, file_content)

    def test_png(self):
        path = '/image/test.png'
        ProxyStorageModelFactory(path=path, user=self.user)
        file_content = b'binary content'

        with patch.object(MdsClient, 'get') as mds_get:
            mds_get.return_value = file_content
            response = self.client.get(f'/v1/files/?path={path}')

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response['Content-Type'], 'image/png')
        self.assertFalse(response.has_header('Content-Disposition'))
        self.assertEqual(response['X-Content-Type-Options'], 'nosniff')
        self.assertEqual(response.content, file_content)


class TestNamespaceSupport(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        self.user = self.client.login_yandex()
        self.path = '/401/test.txt'
        self.meta_info = ProxyStorageModelFactory(path=self.path, namespace=None, user=self.user)

    @responses.activate
    def test_should_open_file_from_old_namespace(self):
        url = f'{settings.MDS_PUBLIC_URL}/get-{settings.MDS_OLD_NAMESPACE}{self.path}'
        responses.add(responses.GET, url, body=b'content')

        response = self.client.get(f'/v1/files/?path={self.path}')
        self.assertEqual(response.status_code, 200)

        self.assertEqual(len(responses.calls), 1)
        request = responses.calls[0].request
        self.assertTrue(f'/get-{settings.MDS_OLD_NAMESPACE}/' in request.url)
        self.assertTrue('Authorization' in request.headers)
        self.assertTrue(request.headers['Authorization'].startswith('Basic'))
        self.assertTrue('X-Ya-Service-Ticket' not in request.headers)

    @responses.activate
    def test_should_open_file_from_new_namespace(self):
        self.meta_info.namespace = settings.MDS_NAMESPACE
        self.meta_info.save()

        url = f'{settings.MDS_PUBLIC_URL}/get-{settings.MDS_NAMESPACE}{self.path}'
        responses.add(responses.GET, url, body=b'content')

        with patch.object(MdsClient, '_get_tvm2_ticket', return_value='123'):
            response = self.client.get(f'/v1/files/?path={self.path}')
        self.assertEqual(response.status_code, 200)

        self.assertEqual(len(responses.calls), 1)
        request = responses.calls[0].request
        self.assertTrue(f'/get-{settings.MDS_NAMESPACE}/' in request.url)
        self.assertTrue('Authorization' not in request.headers)
        self.assertTrue('X-Ya-Service-Ticket' in request.headers)
        self.assertEqual(request.headers['X-Ya-Service-Ticket'], '123')


class TestFilePermission(TestCase):
    fixtures = ['initial_data.json']
    client_class = YandexClient

    def setUp(self):
        Permission.objects.get_or_create(
            name=settings.ROLE_FORM_FILEDOWNLOAD,
            codename=settings.ROLE_FORM_FILEDOWNLOAD,
            content_type=ContentType.objects.get_for_model(Survey),
        )
        Permission.objects.get_or_create(
            name=settings.ROLE_GROUP_FILEDOWNLOAD,
            codename=settings.ROLE_GROUP_FILEDOWNLOAD,
            content_type=ContentType.objects.get_for_model(SurveyGroup),
        )

    def test_not_authenticated(self):
        path = f'/401/{ObjectId()}_test.txt'
        ProxyStorageModelFactory(path=path)

        response = self.client.get(f'/v1/files/?path={path}')

        self.assertEqual(response.status_code, 404)

    def test_permitted_for_superuser(self):
        self.client.login_yandex(is_superuser=True)
        path = f'/401/{ObjectId()}_test.txt'
        ProxyStorageModelFactory(path=path)

        file_content = b'binary content'
        with patch.object(MdsClient, 'get') as mds_get:
            mds_get.return_value = file_content
            response = self.client.get(f'/v1/files/?path={path}')

        self.assertEqual(response.status_code, 200)

    def test_permitted_for_author(self):
        user = self.client.login_yandex()
        path = f'/401/{ObjectId()}_test.txt'
        ProxyStorageModelFactory(path=path, user=user)

        file_content = b'binary content'
        with patch.object(MdsClient, 'get') as mds_get:
            mds_get.return_value = file_content
            response = self.client.get(f'/v1/files/?path={path}')

        self.assertEqual(response.status_code, 200)

    def test_not_permitted_for_user_without_survey(self):
        self.client.login_yandex()
        path = f'/401/{ObjectId()}_test.txt'
        ProxyStorageModelFactory(path=path)

        response = self.client.get(f'/v1/files/?path={path}')

        self.assertEqual(response.status_code, 404)

    def test_not_permitted_for_user_without_rights_on_survey(self):
        self.client.login_yandex()
        survey = SurveyFactory()
        path = f'/401/{ObjectId()}_test.txt'
        ProxyStorageModelFactory(path=path, survey=survey)

        response = self.client.get(f'/v1/files/?path={path}')

        self.assertEqual(response.status_code, 404)

    def test_permitted_for_user_with_rights_on_change_survey(self):
        user = self.client.login_yandex()
        survey = SurveyFactory()
        path = f'/401/{ObjectId()}_test.txt'
        ProxyStorageModelFactory(path=path, survey=survey)
        assign_perm(settings.ROLE_FORM_MANAGER, user, survey)

        file_content = b'binary content'
        with patch.object(MdsClient, 'get') as mds_get:
            mds_get.return_value = file_content
            response = self.client.get(f'/v1/files/?path={path}')

        self.assertEqual(response.status_code, 200)

    def test_permitted_for_user_with_rights_on_viewfile_survey(self):
        user = self.client.login_yandex()
        survey = SurveyFactory()
        path = f'/401/{ObjectId()}_test.txt'
        ProxyStorageModelFactory(path=path, survey=survey)
        assign_perm(settings.ROLE_FORM_FILEDOWNLOAD, user, survey)

        file_content = b'binary content'
        with patch.object(MdsClient, 'get') as mds_get:
            mds_get.return_value = file_content
            response = self.client.get(f'/v1/files/?path={path}')

        self.assertEqual(response.status_code, 200)

        response = self.client.get(f'/admin/api/v2/surveys/{survey.pk}/')
        self.assertEqual(response.status_code, 403)

    def test_permitted_for_user_with_rights_on_change_surveygroup(self):
        user = self.client.login_yandex()
        survey_group = SurveyGroupFactory()
        survey = SurveyFactory(group=survey_group)
        path = f'/401/{ObjectId()}_test.txt'
        ProxyStorageModelFactory(path=path, survey=survey)
        assign_perm(settings.ROLE_GROUP_MANAGER, user, survey_group)

        file_content = b'binary content'
        with patch.object(MdsClient, 'get') as mds_get:
            mds_get.return_value = file_content
            response = self.client.get(f'/v1/files/?path={path}')

        self.assertEqual(response.status_code, 200)

    def test_permitted_for_user_with_rights_on_viewfile_surveygroup(self):
        user = self.client.login_yandex()
        survey_group = SurveyGroupFactory()
        survey = SurveyFactory(group=survey_group)
        path = f'/401/{ObjectId()}_test.txt'
        ProxyStorageModelFactory(path=path, survey=survey)
        assign_perm(settings.ROLE_GROUP_FILEDOWNLOAD, user, survey_group)

        file_content = b'binary content'
        with patch.object(MdsClient, 'get') as mds_get:
            mds_get.return_value = file_content
            response = self.client.get(f'/v1/files/?path={path}')

        self.assertEqual(response.status_code, 200)

        response = self.client.get(f'/admin/api/v2/surveys/{survey.pk}/')
        self.assertEqual(response.status_code, 403)

        response = self.client.get(f'/admin/api/v2/survey-groups/{survey_group.pk}/')
        self.assertEqual(response.status_code, 403)
