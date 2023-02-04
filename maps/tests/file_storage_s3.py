import re
import socket

import moto.s3.models
import pytest

from maps.garden.sdk.resources.file import StorageKey, S3FileStorage

from .file_resource import TestFileResource


def test_prefix_none(file_storage_settings_s3):
    s3_settings = file_storage_settings_s3["file_storage_s3"]
    storage = S3FileStorage(
        endpoint_url=s3_settings["mds_host"],
        bucket_name=s3_settings["bucket"],
        prefix=None,
        access_key=s3_settings["access_key"],
        secret_key=s3_settings["secret_key"]
    )
    assert storage.prefix is None
    assert "/" not in storage._get_key_name("name")


def test_prefix_empty(file_storage_settings_s3):
    s3_settings = file_storage_settings_s3["file_storage_s3"]
    storage = S3FileStorage(
        endpoint_url=s3_settings["mds_host"],
        bucket_name=s3_settings["bucket"],
        prefix="",
        access_key=s3_settings["access_key"],
        secret_key=s3_settings["secret_key"]
    )
    assert not storage.prefix
    assert "/" not in storage._get_key_name("name")


def test_urls_valid(file_storage_settings_s3):
    s3_settings = file_storage_settings_s3["file_storage_s3"]
    storages = [
        S3FileStorage(
            endpoint_url=endpoint_url,
            bucket_name=s3_settings["bucket"],
            prefix=s3_settings["prefix"],
            access_key=s3_settings["access_key"],
            secret_key=s3_settings["secret_key"]
        ) for endpoint_url in ["s3.mds.yandex.net", "http://s3.mds.yandex.net", "https://s3.mds.yandex.net"]
    ]

    for storage in storages:
        assert re.match(r"^https?://[\w.]+$", storage.endpoint_url) is not None
        url = storage.url(StorageKey("key", "s3"))
        assert re.match(r"^https?://[\w.]+/[\w\-.]+/[\w.]+$", url) is not None, url


@pytest.mark.parametrize("method_name", ["get_object", "upload_part"])
@pytest.mark.parametrize("call_number", list(range(1, 10)))
def test_exception(monkeypatch, file_storage_settings_s3, method_name, call_number):
    """ Tests if the `call_number`-th `method_name` call will be successfully handled

    Seems the following approach isn't the best one: https://stackoverflow.com/q/60013745

    :param method_name: moto s3_backend's method name, that must fail
    :param call_number: the `method_name` call number that must raise an exception
    """
    def _raise_at(func):
        """Raises `socket.timeout` on `call_number`-th `func` call"""
        state = {"count": 0}

        def _wrapper_function(*args, **kwargs):
            state["count"] += 1
            if state["count"] == call_number:
                raise socket.timeout
            else:
                return func(*args, **kwargs)

        return _wrapper_function

    wrapped_function = _raise_at(getattr(moto.s3.models.s3_backends["global"], method_name))
    monkeypatch.setattr(moto.s3.models.s3_backends["global"], method_name, wrapped_function)
    TestFileResource().test_lifecycle(file_storage_settings_s3, "big_file")
