import re
import typing as tp
from unittest.mock import Mock, call

import pytest

from transfer_manager.go.proto.api.endpoint import logfeller_pb2
from maps.infra.sedem.lib.logfeller.endpoint import LogFellerSourceEndpoint, LogFellerTargetEndpoint
from maps.infra.sedem.lib.logfeller.logfeller import LogFellerBuilder, FIELD_NAME_REGEX
from maps.infra.sedem.lib.logfeller.schema import Schema, Field
from maps.pylibs.infrastructure_api.cloud.iam.iam_client import IamClient
from maps.pylibs.infrastructure_api.cloud.logfeller.resource_manager import ResourceManagerClient
from transfer_manager.go.proto.api import (
    endpoint_service_pb2,
    endpoint_pb2,
    transfer_service_pb2,
    transfer_pb2,
)
from yandex.cloud.priv.operation.operation_pb2 import Operation
from yandex.cloud.resourcemanager.v1 import cloud_pb2, folder_pb2


class IamClientMock:
    @classmethod
    def iam_token(cls, oauth: str) -> str:
        return 'iam-token'


class ResourceManagerClientMock:
    @classmethod
    def find_cloud(cls, name: str) -> cloud_pb2.Cloud:
        return cloud_pb2.Cloud(id='qloud-id')

    @classmethod
    def find_folder(cls, cloud_id: str, name: str) -> folder_pb2.Folder:
        return folder_pb2.Folder(id='folder-id')


def test_ensure_endpoint_create() -> None:
    expected_endpoint = endpoint_pb2.Endpoint(
        id='endpoint-id',
        folder_id='folder-id',
    )
    logfeller_client = Mock()
    logfeller_client.create_endpoint.return_value = expected_endpoint

    logfeller_builder = LogFellerBuilder(
        oauth_token='fake-token',
        resource_manager_client=tp.cast(ResourceManagerClient, ResourceManagerClientMock()),
        logfeller_client=logfeller_client,
        iam_client=tp.cast(IamClient, IamClientMock()),
    )
    endpoint_name = 'endpoint_name'
    endpoint_description = 'abc_slug target endpoint (YT)'
    endpoint_settings = LogFellerTargetEndpoint(
        log_name='maps/teacup-log',
        lifetime='14d',
        sort_by_columns=['timestamp', 'id'],
        yt_account='yt_account',
        yt_cluster='Hahn',
    )
    folder_id = 'folder-id'
    endpoint = logfeller_builder.ensure_endpoint(
        folder_id=folder_id,
        endpoint=None,
        endpoint_name=endpoint_name,
        endpoint_description=endpoint_description,
        endpoint_settings=endpoint_settings,
    )

    assert expected_endpoint == endpoint
    assert logfeller_client.mock_calls == [call.create_endpoint(endpoint_service_pb2.CreateEndpointRequest(
        folder_id=folder_id,
        name=endpoint_name,
        description=endpoint_description,
        settings=endpoint_settings.to_settings(),
    ))]


def test_ensure_endpoint_update() -> None:
    expected_endpoint = endpoint_pb2.Endpoint(
        id='endpoint-id',
        folder_id='folder-id',
    )
    logfeller_client = Mock()
    logfeller_client.update_endpoint.return_value = expected_endpoint

    logfeller_builder = LogFellerBuilder(
        oauth_token='fake-token',
        resource_manager_client=tp.cast(ResourceManagerClient, ResourceManagerClientMock()),
        logfeller_client=logfeller_client,
        iam_client=tp.cast(IamClient, IamClientMock()),
    )
    endpoint_name = 'endpoint_name'
    endpoint_description = 'abc_slug target endpoint (YT)'
    endpoint_settings = LogFellerTargetEndpoint(
        log_name='maps/teacup-log',
        lifetime='14d',
        sort_by_columns=['timestamp', 'id'],
        yt_account='yt_account',
        yt_cluster='Hahn',
    )
    endpoint = logfeller_builder.ensure_endpoint(
        folder_id='folder-id',
        endpoint=endpoint_pb2.Endpoint(
            id='endpoint-id',
            folder_id='folder-id',
        ),
        endpoint_name=endpoint_name,
        endpoint_description=endpoint_description,
        endpoint_settings=endpoint_settings,
    )

    assert expected_endpoint == endpoint
    assert logfeller_client.mock_calls == [call.update_endpoint(endpoint_service_pb2.UpdateEndpointRequest(
        endpoint_id='endpoint-id',
        name=endpoint_name,
        description=endpoint_description,
        settings=endpoint_settings.to_settings(),
    ))]


def test_ensure_endpoint_skip_zero_diff() -> None:
    endpoint_name = 'endpoint_name'
    endpoint_description = 'abc_slug target endpoint (YT)'
    endpoint_settings = LogFellerTargetEndpoint(
        log_name='maps/teacup-log',
        lifetime='14d',
        sort_by_columns=['timestamp', 'id'],
        yt_account='yt_account',
        yt_cluster='Hahn',
    )
    expected_endpoint = endpoint_pb2.Endpoint(
        id='endpoint-id',
        folder_id='folder-id',
        name=endpoint_name,
        description=endpoint_description,
        settings=endpoint_settings.to_settings(),
    )
    logfeller_client = Mock()

    logfeller_builder = LogFellerBuilder(
        oauth_token='fake-token',
        resource_manager_client=tp.cast(ResourceManagerClient, ResourceManagerClientMock()),
        logfeller_client=logfeller_client,
        iam_client=tp.cast(IamClient, IamClientMock()),
    )
    endpoint = logfeller_builder.ensure_endpoint(
        folder_id='folder-id',
        endpoint=expected_endpoint,
        endpoint_name=endpoint_name,
        endpoint_description=endpoint_description,
        endpoint_settings=endpoint_settings,
    )

    assert expected_endpoint == endpoint
    assert logfeller_client.mock_calls == []


def test_ensure_target_endpoint() -> None:
    expected_endpoint = endpoint_pb2.Endpoint(
        id='endpoint-id',
        folder_id='folder-id',
    )
    logfeller_client = Mock()
    logfeller_client.create_endpoint.return_value = expected_endpoint

    logfeller_builder = LogFellerBuilder(
        oauth_token='fake-token',
        resource_manager_client=tp.cast(ResourceManagerClient, ResourceManagerClientMock()),
        logfeller_client=logfeller_client,
        iam_client=tp.cast(IamClient, IamClientMock()),
    )

    endpoint = logfeller_builder.ensure_target_endpoint(
        abc_slug='abc_slug',
        folder_id='folder-id',
        log_name='maps/teacup-log',
        lifetime='14d',
        sort_by_columns=['timestamp', 'id'],
        yt_account='yt_account',
        endpoints=[],
        endpoint_name='endpoint_name',
        yt_cluster='Hahn',
    )

    endpoint_name = 'endpoint_name'
    endpoint_description = 'abc_slug target endpoint (YT)'
    endpoint_settings = LogFellerTargetEndpoint(
        log_name='maps/teacup-log',
        lifetime='14d',
        sort_by_columns=['timestamp', 'id'],
        yt_account='yt_account',
        yt_cluster='Hahn',
    )
    assert expected_endpoint == endpoint
    assert logfeller_client.mock_calls == [call.create_endpoint(endpoint_service_pb2.CreateEndpointRequest(
        folder_id='folder-id',
        name=endpoint_name,
        description=endpoint_description,
        settings=endpoint_settings.to_settings(),
    ))]


def test_ensure_source_endpoint() -> None:
    expected_endpoint = endpoint_pb2.Endpoint(
        id='endpoint-id',
        folder_id='folder-id',
    )
    logfeller_client = Mock()
    logfeller_client.create_endpoint.return_value = expected_endpoint

    logfeller_builder = LogFellerBuilder(
        oauth_token='fake-token',
        resource_manager_client=tp.cast(ResourceManagerClient, ResourceManagerClientMock()),
        logfeller_client=logfeller_client,
        iam_client=tp.cast(IamClient, IamClientMock()),
    )

    schema = Schema.parse_obj({
        'fields': [
            {'name': 'timestamp', 'type': 'YT_TYPE_TIMESTAMP', 'sort': True},
            {'name': 'id', 'type': 'YT_TYPE_STRING', 'sort': True}
        ]
    })
    endpoint = logfeller_builder.ensure_source_endpoint(
        abc_slug='abc_slug',
        folder_id='folder-id',
        topic_path='maps/teacup-topic',
        schema=schema,
        endpoints=list(),
        endpoint_name='endpoint_name',
        log_format='JSON',
        timestamp_precision=6,
    )

    endpoint_name = 'endpoint_name'
    endpoint_description = 'abc_slug source endpoint (Logbroker)'
    endpoint_settings = LogFellerSourceEndpoint(
        topic_path='maps/teacup-topic',
        schema=schema,
        format='JSON',
        precision=6,
    )
    assert expected_endpoint == endpoint
    assert logfeller_client.mock_calls == [call.create_endpoint(endpoint_service_pb2.CreateEndpointRequest(
        folder_id='folder-id',
        name=endpoint_name,
        description=endpoint_description,
        settings=endpoint_settings.to_settings(),
    ))]


def test_ensure_transfer_settings_exists() -> None:
    expected_transfer = transfer_pb2.Transfer(
        id='transfer-id',
        folder_id='folder-id',
        name='transfer_name',
    )
    logfeller_client = Mock()
    logfeller_client.list_transfers.return_value = transfer_service_pb2.ListTransfersResponse(
        transfers=[expected_transfer],
    )

    logfeller_builder = LogFellerBuilder(
        oauth_token='fake-token',
        resource_manager_client=tp.cast(ResourceManagerClient, ResourceManagerClientMock()),
        logfeller_client=logfeller_client,
        iam_client=tp.cast(IamClient, IamClientMock()),
    )

    transfer = logfeller_builder.ensure_transfer_settings(
        source_id='source-id',
        target_id='target-id',
        transfer_name='transfer_name',
        folder_id='folder-id',
        abc_slug='service-name',
    )
    assert expected_transfer == transfer
    assert logfeller_client.mock_calls == [
        call.list_transfers(transfer_service_pb2.ListTransfersRequest(
            folder_id='folder-id',
        )),
    ]


def test_ensure_transfer_settings_create() -> None:
    expected_transfer = transfer_pb2.Transfer(
        id='transfer-id',
        folder_id='folder-id',
        name='transfer_name',
    )
    logfeller_client = Mock()
    logfeller_client.list_transfers.return_value = transfer_service_pb2.ListTransfersResponse(
        transfers=[],
    )
    logfeller_client.create_transfer.return_value = expected_transfer

    logfeller_builder = LogFellerBuilder(
        oauth_token='fake-token',
        resource_manager_client=tp.cast(ResourceManagerClient, ResourceManagerClientMock()),
        logfeller_client=logfeller_client,
        iam_client=tp.cast(IamClient, IamClientMock()),
    )

    transfer = logfeller_builder.ensure_transfer_settings(
        source_id='source-id',
        target_id='target-id',
        transfer_name='transfer_name',
        folder_id='folder-id',
        abc_slug='service-name',
    )
    assert expected_transfer == transfer
    assert logfeller_client.mock_calls == [
        call.list_transfers(transfer_service_pb2.ListTransfersRequest(
            folder_id='folder-id',
        )),
        call.create_transfer(transfer_service_pb2.CreateTransferRequest(
            source_id='source-id',
            target_id='target-id',
            name='transfer_name',
            description='service-name transfer (Logbroker->YT)',
            folder_id='folder-id',
            type=transfer_pb2.INCREMENT_ONLY,
        )),
        call.activate_transfer(
            transfer_id='transfer-id',
        )
    ]


def test_compare_endpoint_equal() -> None:
    assert LogFellerBuilder.compare_endpoint(
        endpoint=endpoint_pb2.Endpoint(
            name='abc',
            description='desc',
            settings=LogFellerTargetEndpoint(
                log_name='maps/teacup-log',
                lifetime='14d',
                sort_by_columns=['timestamp', 'id'],
                yt_account='yt_account',
                yt_cluster='Hahn',
            ).to_settings()
        ),
        endpoint_name='abc',
        endpoint_description='desc',
        endpoint_settings=LogFellerTargetEndpoint(
            log_name='maps/teacup-log',
            lifetime='14d',
            sort_by_columns=['timestamp', 'id'],
            yt_account='yt_account',
            yt_cluster='Hahn',
        )
    )


def test_compare_endpoint_diff_name() -> None:
    assert not LogFellerBuilder.compare_endpoint(
        endpoint=endpoint_pb2.Endpoint(
            name='different_name',
            description='desc',
            settings=LogFellerTargetEndpoint(
                log_name='maps/teacup-log',
                lifetime='14d',
                sort_by_columns=['timestamp', 'id'],
                yt_account='yt_account',
                yt_cluster='Hahn',
            ).to_settings()
        ),
        endpoint_name='abc',
        endpoint_description='desc',
        endpoint_settings=LogFellerTargetEndpoint(
            log_name='maps/teacup-log',
            lifetime='14d',
            sort_by_columns=['timestamp', 'id'],
            yt_account='yt_account',
            yt_cluster='Hahn',
        )
    )


def test_compare_endpoint_diff_description() -> None:
    assert not LogFellerBuilder.compare_endpoint(
        endpoint=endpoint_pb2.Endpoint(
            name='abc',
            description='different_description',
            settings=LogFellerTargetEndpoint(
                log_name='maps/teacup-log',
                lifetime='14d',
                sort_by_columns=['timestamp', 'id'],
                yt_account='yt_account',
                yt_cluster='Hahn',
            ).to_settings()
        ),
        endpoint_name='abc',
        endpoint_description='desc',
        endpoint_settings=LogFellerTargetEndpoint(
            log_name='maps/teacup-log',
            lifetime='14d',
            sort_by_columns=['timestamp', 'id'],
            yt_account='yt_account',
            yt_cluster='Hahn',
        )
    )


def test_compare_endpoint_diff_settings() -> None:
    assert not LogFellerBuilder.compare_endpoint(
        endpoint=endpoint_pb2.Endpoint(
            name='abc',
            description='desc',
            settings=LogFellerTargetEndpoint(
                log_name='maps/teacup-log',
                lifetime='14d',
                sort_by_columns=['timestamp', 'id'],
                yt_account='yt_account',
                yt_cluster='Hahn',
            ).to_settings()
        ),
        endpoint_name='abc',
        endpoint_description='desc',
        endpoint_settings=LogFellerTargetEndpoint(
            log_name='maps/different-log',
            lifetime='14d',
            sort_by_columns=['timestamp', 'id'],
            yt_account='yt_account',
            yt_cluster='Hahn',
        )
    )


def test_compare_endpoint_same_settings() -> None:
    assert tp.cast(endpoint_pb2.EndpointSettings, LogFellerTargetEndpoint(
        log_name='maps/teacup-log',
        lifetime='14d',
        sort_by_columns=['timestamp', 'id'],
        yt_account='yt_account',
        yt_cluster='Hahn',
    )) == LogFellerTargetEndpoint(
        log_name='maps/teacup-log',
        lifetime='14d',
        sort_by_columns=['timestamp', 'id'],
        yt_account='yt_account',
        yt_cluster='Hahn',
    ).to_settings()


def test_endpoint_cluster_settings() -> None:
    assert LogFellerTargetEndpoint(
        log_name='maps/teacup-log',
        lifetime='14d',
        sort_by_columns=['timestamp', 'id'],
        yt_account='yt_account',
        yt_cluster='Hahn',
    ).to_settings().logfeller_target.yt_cluster_name == logfeller_pb2.HAHN

    assert LogFellerTargetEndpoint(
        log_name='maps/teacup-log',
        lifetime='14d',
        sort_by_columns=['timestamp', 'id'],
        yt_account='yt_account',
        yt_cluster='Arnold',
    ).to_settings().logfeller_target.yt_cluster_name == logfeller_pb2.ARNOLD


def test_validate_transfer_id() -> None:
    with pytest.raises(ValueError, match=re.escape(fr'Transfer ID should match regex {FIELD_NAME_REGEX.pattern}')):
        assert LogFellerBuilder.validate_transfer_id('Teacup')
    with pytest.raises(ValueError, match='Transfer ID should be at most 59 characters long'):
        assert LogFellerBuilder.validate_transfer_id(
            'huuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuugestring')
    LogFellerBuilder.validate_transfer_id('teacup')


def test_wait_operation_transfer() -> None:
    expected_transfer = transfer_pb2.Transfer(
        id='transfer-id',
        folder_id='folder-id',
        name='transfer_name',
    )
    logfeller_client = Mock()
    logfeller_client.list_endpoint_transfers.return_value = transfer_service_pb2.ListTransfersResponse(
        transfers=[expected_transfer],
    )

    logfeller_client.list_transfer_operations.side_effect = [
        transfer_service_pb2.ListTransferOperationsResponse(
            operations=[Operation(id='operation-id', done=False)],
        ),
        transfer_service_pb2.ListTransferOperationsResponse(
            operations=[Operation(id='operation-id', done=True)],
        ),
    ]

    logfeller_builder = LogFellerBuilder(
        oauth_token='fake-token',
        resource_manager_client=tp.cast(ResourceManagerClient, ResourceManagerClientMock()),
        logfeller_client=logfeller_client,
        iam_client=tp.cast(IamClient, IamClientMock()),
        wait_interval=0.001,
    )

    logfeller_builder.wait_operations(
        folder_id='folder-id',
        endpoint_id='endpoint-id',
        timeout=0.1,
    )
    assert logfeller_client.mock_calls == [
        call.list_endpoint_transfers(endpoint_service_pb2.ListEndpointTransfersRequest(endpoint_id='endpoint-id')),
        call.list_transfer_operations(
            transfer_service_pb2.ListTransferOperationsRequest(folder_id='folder-id', transfer_id='transfer-id')),
        call.list_transfer_operations(
            transfer_service_pb2.ListTransferOperationsRequest(folder_id='folder-id', transfer_id='transfer-id')),
    ]


def test_wait_operation_endpoint() -> None:
    logfeller_client = Mock()
    logfeller_client.list_endpoint_transfers.return_value = transfer_service_pb2.ListTransfersResponse(
        transfers=[],
    )

    logfeller_client.list_endpoint_operations.side_effect = [
        endpoint_service_pb2.ListEndpointOperationsResponse(
            operations=[Operation(id='operation-id', done=False)],
        ),
        endpoint_service_pb2.ListEndpointOperationsResponse(
            operations=[Operation(id='operation-id', done=True)],
        ),
    ]

    logfeller_builder = LogFellerBuilder(
        oauth_token='fake-token',
        resource_manager_client=tp.cast(ResourceManagerClient, ResourceManagerClientMock()),
        logfeller_client=logfeller_client,
        iam_client=tp.cast(IamClient, IamClientMock()),
        wait_interval=0.001,
    )

    logfeller_builder.wait_operations(
        folder_id='folder-id',
        endpoint_id='endpoint-id',
        timeout=0.1,
    )
    assert logfeller_client.mock_calls == [
        call.list_endpoint_transfers(endpoint_service_pb2.ListEndpointTransfersRequest(endpoint_id='endpoint-id')),
        call.list_endpoint_operations(
            endpoint_service_pb2.ListEndpointOperationsRequest(folder_id='folder-id', endpoint_id='endpoint-id')),
        call.list_endpoint_operations(
            endpoint_service_pb2.ListEndpointOperationsRequest(folder_id='folder-id', endpoint_id='endpoint-id')),
    ]


def test_ensure_transfer(monkeypatch) -> None:
    logfeller_client = Mock()
    logfeller_client.list_endpoints.return_value = endpoint_service_pb2.ListEndpointsResponse(endpoints=[])

    logfeller_builder = LogFellerBuilder(
        oauth_token='fake-token',
        resource_manager_client=tp.cast(ResourceManagerClient, ResourceManagerClientMock()),
        logfeller_client=logfeller_client,
        iam_client=tp.cast(IamClient, IamClientMock()),
        wait_interval=0.001,
        wait_timeout=0.1,
    )

    validate_transfer_id_mock = Mock()
    validate_transfer_id_mock.return_value = None
    ensure_source_endpoint_mock = Mock()
    ensure_source_endpoint_mock.return_value = endpoint_pb2.Endpoint(id='source-id', folder_id='folder-id')
    ensure_target_endpoint_mock = Mock()
    ensure_target_endpoint_mock.return_value = endpoint_pb2.Endpoint(id='target-id', folder_id='folder-id')
    ensure_transfer_settings_mock = Mock()
    ensure_transfer_settings_mock.return_value = transfer_pb2.Transfer(id='transfer-id', folder_id='folder-id')
    wait_operations_mock = Mock()
    wait_operations_mock.return_value = None

    monkeypatch.setattr(logfeller_builder, 'validate_transfer_id', validate_transfer_id_mock)
    monkeypatch.setattr(logfeller_builder, 'ensure_source_endpoint', ensure_source_endpoint_mock)
    monkeypatch.setattr(logfeller_builder, 'ensure_target_endpoint', ensure_target_endpoint_mock)
    monkeypatch.setattr(logfeller_builder, 'ensure_transfer_settings', ensure_transfer_settings_mock)
    monkeypatch.setattr(logfeller_builder, 'wait_operations', wait_operations_mock)

    logfeller_builder.ensure_transfer(
        abc_slug='maps-core-teacup',
        transfer_id='teacup',
        topic_path='maps/teacup-topic',
        yt_log_path='maps/teacup-log',
        schema_json='''{"fields": [{"name": "field", "type": "YT_TYPE_STRING", "sort": true}]}''',
        lifetime='14d',
        yt_account='yt_account',
        log_format='JSON',
        timestamp_precision=6,
        yt_cluster='Hahn',
    )
    assert logfeller_client.mock_calls == [
        call.list_endpoints(endpoint_service_pb2.ListEndpointsRequest(folder_id='folder-id'))
    ]
    assert validate_transfer_id_mock.mock_calls == [call('teacup')]
    assert ensure_source_endpoint_mock.mock_calls == [
        call(
            abc_slug='maps-core-teacup',
            folder_id='folder-id',
            topic_path='maps/teacup-topic',
            schema=Schema(fields=[Field(name='field', type='YT_TYPE_STRING', sort=True)]),
            endpoints=[],
            endpoint_name='teacup-src',
            log_format='JSON',
            timestamp_precision=6,
        )
    ]
    assert ensure_target_endpoint_mock.mock_calls == [
        call(
            abc_slug='maps-core-teacup',
            folder_id='folder-id',
            log_name='maps/teacup-log',
            lifetime='14d',
            sort_by_columns=['field'],
            yt_account='yt_account',
            yt_cluster='Hahn',
            endpoints=[],
            endpoint_name='teacup-tgt',
        )
    ]
    assert ensure_transfer_settings_mock.mock_calls == [
        call(
            source_id='source-id',
            target_id='target-id',
            transfer_name='teacup',
            folder_id='folder-id',
            abc_slug='maps-core-teacup',
        )
    ]
    assert wait_operations_mock.mock_calls == [
        call(folder_id='folder-id', endpoint_id='source-id', timeout=0.1),
        call(folder_id='folder-id', endpoint_id='source-id', timeout=0.1),
    ]
