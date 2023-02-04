import unittest
from unittest import mock
import datetime

import reactor_client.reactor_objects as r_objs
import logos.libs.micro_reactor as micro_reactor

from agency_rewards.rewards.utils.reactor import ReactorClient


class TestReactor(unittest.TestCase):
    def test_create_artifact(self):
        client = ReactorClient('dev', "TOKEN", mock.MagicMock())
        client.client = mock.MagicMock()
        exc = client.create_artifact("/path/to/artifact", "ARTIFACT_TYPE")
        self.assertIsNone(exc)
        client.client.artifact.create.assert_called_with(
            r_objs.ArtifactTypeIdentifier(artifact_type_key="ARTIFACT_TYPE"),
            r_objs.ArtifactIdentifier(namespace_identifier=r_objs.NamespaceIdentifier("/path/to/artifact")),
            description='',
            permissions=r_objs.NamespacePermissions({}),
            cleanup_strategy=r_objs.CleanupStrategyDescriptor([r_objs.CleanupStrategy(r_objs.TtlCleanupStrategy(10))]),
            project_identifier=r_objs.ProjectIdentifier(
                namespace_identifier=r_objs.NamespaceIdentifier("/billing/yb-ar/test/Project")
            ),
            create_if_not_exist=True,
            create_parent_namespaces=True,
        )

    def test_instantiate_existing_artifact_bool(self):
        client = ReactorClient('dev', "TOKEN", mock.MagicMock())
        client.client = mock.MagicMock()
        response = client.instantiate_artifact_bool("/path/to/bool_artifact")
        client.client.artifact.check_exists.assert_called_with(
            namespace_identifier=r_objs.NamespaceIdentifier("/path/to/bool_artifact")
        )
        self.assertIsNotNone(response)

    def test_instantiate_non_existing_artifact_bool(self):
        client = ReactorClient('dev', "TOKEN", mock.MagicMock())
        client.client = mock.MagicMock()
        client.client.artifact.check_exists.return_value = False
        response = client.instantiate_artifact_bool("/path/to/bool_artifact")
        self.assertIsNotNone(response)
        client.client.artifact.create.assert_called_with(
            r_objs.ArtifactTypeIdentifier(artifact_type_key="BOOL"),
            r_objs.ArtifactIdentifier(namespace_identifier=r_objs.NamespaceIdentifier("/path/to/bool_artifact")),
            description='',
            permissions=r_objs.NamespacePermissions({}),
            cleanup_strategy=r_objs.CleanupStrategyDescriptor([r_objs.CleanupStrategy(r_objs.TtlCleanupStrategy(10))]),
            project_identifier=r_objs.ProjectIdentifier(
                namespace_identifier=r_objs.NamespaceIdentifier("/billing/yb-ar/test/Project")
            ),
            create_if_not_exist=True,
            create_parent_namespaces=True,
        )

    def test_error_deleting_not_stopped_reaction(self):
        # создаем фейковые реактор и клиент
        my_reactor = micro_reactor.MicroReactor()
        client = ReactorClient('dev', "TOKEN", mock.MagicMock())
        client.client = micro_reactor.MicroReactorClient(my_reactor)
        # создаем инстанс артефакта
        yt_path = '//statbox/statkey/home/artifact_test'
        artifact_path = '/yt/hahn/statbox/statkey/home/artifact_test'
        cluster = 'hahn'

        namespace_identifier = r_objs.NamespaceIdentifier(artifact_path)
        _ = client.instantiate_artifact_yt_path(
            yt_path=yt_path, artifact_path=artifact_path, user_time=datetime.datetime.now(), cluster=cluster
        )

        # возвращает последний созданный инстанс артефакта по заданному пути
        self.assertTrue(
            client.client.artifact_instance.last(r_objs.ArtifactIdentifier(namespace_identifier=namespace_identifier))
        )

    def test_parse_reactor_exception(self):
        """
        проверяем заголовок сообщения об ошибке если нет доступа до пути в реакторе
        """
        my_reactor = micro_reactor.MicroReactor()
        client = ReactorClient('dev', "TOKEN", mock.MagicMock())
        client.client = micro_reactor.MicroReactorClient(my_reactor)

        exc = (
            """Response contains error with code: 403"""
            """Reason: Forbidden"""
            """Response: {"message":"'get or create namespace {\"login\":\"robot-balance-ar-tst\",\"parentId\":
            \"817247\",\"typeName\":"""
            """\"ns_folder\",\"name\":\"test\",\"roleId2permission\":"""
            """{\"11077\":\"OWNER\"}}' failed: NO_PERMISSION\n - """
            """Can't create namespace because you don't have permission for it's parent: @robot-balance-ar-tst """
            """don't have WRITER permission for """
            """Namespace#817247","uuid":"6ed5e95d-6e45-4719-8c3e-c8c1f01c0493","causeMessages":"""
            """["'get or create namespace {\"login\":\"robot-balance-ar-tst\",\"parentId\":\"817247\","""
            """\"typeName\":\"ns_folder\","""
            """ \"name\":\"test\",\"roleId2permission\":{\"11077\":\"OWNER\"}}' failed: NO_PERMISSION\n - """
            """Can't create namespace because you don't have permission for it's parent: """
            """@robot-balance-ar-tst don't have WRITER permission for Namespace#817247"],"codes":[],"details":[]}"""
            """Request url https://reactor.yandex-team.ru/api/v1/a/create."""
            """Request content:"""
            """{'artifactTypeIdentifier': {'artifactTypeKey': 'YT_PATH'}, 'permissions': """
            """{'version': 0, 'roles': {}}, 'cleanupStrategy': """
            """ {'cleanupStrategies': [{'ttlCleanupStrategy': {'ttlDays': '30'}}]}, 'artifactIdentifier': """
            """{'namespaceIdentifier': {'namespacePath': """
            """'/home/estarchak/test/yb-ar/rewards/market/2021-29-m-2021_market_pokupki'}}, """
            """ 'createIfNotExist': True, 'description': '', 'createParentNamespaces': True}"""
        )
        required_title = (
            'Необходимо выдать права @robot-balance-ar-tst для создания артефакта по пути\n'
            '/home/estarchak/test/yb-ar/rewards/market/2021-29-m-2021_market_pokupki\n'
            'https://reactor.yandex-team.ru/browse?selected=817247'
        )
        msg = client._parse_reactor_exception(exc)
        title = msg[: msg.find('<{{')]
        self.assertEqual(title, required_title)
