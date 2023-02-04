import os
import yatest.common

from maps.garden.sdk.core import Resource, Version
from maps.garden.sdk.resources.yandex_mds import YMDSFileResource
from maps.garden.sdk.module_rpc.proto import resource_pb2 as resource_proto
from maps.garden.sdk.resources.proto import resource_pb2 as core_resource_proto

INTERNAL_MDS_HOST = "http://internal_host.yandex.net"
PUBLIC_MDS_HOST = "https://public_host.yandex.net"
NAMESPACE = "mds-namespace"
PREFIX = "prefix"

RELEASE_NAME = "2020-06-12"
FILE_NAME = "filename"
MDS_KEY = f"123/{PREFIX}/{RELEASE_NAME}/{FILE_NAME}"

MDS_RESPONSE = f"""<?xml version="1.0" encoding="utf-8"?>
<post obj="namespace.filename" id="81d8ba78...666dd3d1" groups="3" size="100" key="{MDS_KEY}">
  <complete addr="141.8.145.55:1032" path="/src/storage/8/data-0.0" group="223" status="0"/>
  <complete addr="141.8.145.116:1032" path="/srv/storage/8/data-0.0" group="221" status="0"/>
  <complete addr="141.8.145.119:1029" path="/srv/storage/5/data-0.0" group="225" status="0"/>
  <written>3</written>
</post>
"""


def test_resource_cycle(requests_mock):
    requests_mock.post(
        f"{INTERNAL_MDS_HOST}/upload-{NAMESPACE}/{PREFIX}/{RELEASE_NAME}/{FILE_NAME}",
        text=MDS_RESPONSE
    )

    requests_mock.get(
        f"{INTERNAL_MDS_HOST}/delete-{NAMESPACE}/{MDS_KEY}")

    environment_settings = {
        "mds": {
            "internal_host": INTERNAL_MDS_HOST,
            "public_host": PUBLIC_MDS_HOST,
            "prefix": PREFIX,
            "auth": "token"
        }
    }

    resource = YMDSFileResource(
        name="test_mds_file",
        path_template="{release_name}/filename",
        namespace=NAMESPACE
    )
    resource.version = Version(properties={"release_name": RELEASE_NAME})
    resource.load_environment_settings(environment_settings)

    local_path = yatest.common.test_source_path("data/america.txt")
    expected_size_bytes = os.path.getsize(local_path)

    resource.upload(local_path)

    assert resource.path == f"{PREFIX}/{RELEASE_NAME}/filename"
    assert resource.public_url == f"{PUBLIC_MDS_HOST}/get-{NAMESPACE}/{MDS_KEY}"
    assert resource.md5_checksum
    assert "YMDSFileResource" in str(resource)

    resource.logged_commit()
    resource.calculate_size()

    assert resource.download_size == expected_size_bytes
    assert resource.size == {"bytes": expected_size_bytes}

    proto = resource.to_proto()
    decoded_resource = Resource.from_proto(proto)

    decoded_resource.load_environment_settings(environment_settings)
    decoded_resource.remove()

    assert requests_mock.call_count == 2  # upload and delete


def test_there_and_back_again_mds_resource():
    mds_file = YMDSFileResource(
        name="test_mds_file",
        path_template="{any}_{will}_{do}",
        namespace="some-nonexistent-namespace"
    )

    proto = mds_file.to_proto()

    assert isinstance(proto, resource_proto.Resource)
    assert proto.HasExtension(core_resource_proto.mdsFile)
    meta = proto.Extensions[core_resource_proto.mdsFile]

    assert proto.name == mds_file.name
    assert meta.filenameTemplate == mds_file._path_template
    assert meta.mdsNamespace == mds_file._namespace

    decoded_mds_file = Resource.from_proto(proto)
    assert isinstance(decoded_mds_file, YMDSFileResource)

    assert decoded_mds_file.name == mds_file.name
    assert decoded_mds_file._path_template == mds_file._path_template
    assert decoded_mds_file._namespace == mds_file._namespace
