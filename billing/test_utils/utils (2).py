import os
import logging

from yt.wrapper import (
    YtClient,
    ypath_join,
    yson,
)


def create_yt_client(backend=None):
    config = {
        'enable_request_logging': True,
        # Set default transaction timeout to one hour (max),
        # so that we do not need to pass it every time.
        'transaction_timeout': 1000 * 60 * 60,
    }
    if backend is not None:
        config['backend'] = backend
    return YtClient(os.environ['YT_PROXY'], config=config)


def create_subdirectory(yt_client, yt_root, dir_name):
    dir_path = ypath_join(yt_root, dir_name)
    yt_client.create('map_node', dir_path)
    return dir_path


def create_dyntable(yt_client, path, schema, data=None, attributes=None):
    tbl_schema = yson.YsonList()
    tbl_schema.extend(schema)
    tbl_schema.attributes['unique_keys'] = True

    tbl_attributes = {'schema': tbl_schema}
    if attributes:
        tbl_attributes.update(attributes)

    yt_client.create('table', path, attributes=tbl_attributes)

    if data:
        yt_client.write_table(path, data)
    yt_client.alter_table(path, dynamic=True)
    yt_client.mount_table(path, sync=True, freeze=True)


def fix_yt_logger():
    """
    YT по умолчанию настраивает свой логгер на вывод в stderr,
    что не очень то удобно. Мы выше настраиваем свое логирование,
    а здесь просто перенаправляем логирование YT в общий handler.
    """
    from yt.logger import LOGGER

    assert len(LOGGER.handlers) == 1, LOGGER.handlers
    assert isinstance(LOGGER.handlers[0], logging.StreamHandler), LOGGER.handlers[0]
    LOGGER.removeHandler(LOGGER.handlers[0])
    LOGGER.propagate = True
