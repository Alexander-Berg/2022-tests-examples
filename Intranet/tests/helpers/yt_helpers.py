import json
import os
import time
from uuid import uuid4

from django.conf import settings
from django.db.models import Model
from django.utils import timezone

from .indexations_helpers import get_base_expected_doc


def get_row(**kwargs):
    data = {
        'url': uuid4().hex,
        'raw': None,
        'timestamp': int(time.time()),
        'JsonMessage': '{}',
        'deleted': False
    }
    data.update(kwargs)
    return data


def sort_table(data):
    return sorted(data, key=lambda a: a['url'])


def write_yt_table(yt, path, data):
    if not yt.exists(path):
        yt.create('table', path, recursive=True)
    yt.write_table(path, data)


def read_yt_table(yt, path=None):
    data = list(yt.read_table(path))
    return sort_table(data)


def read_yt_dynamic_table(yt, path):
    def clear_system_fields(row):
        row.pop('$row_index', None)
        row.pop('$tablet_index', None)
        return row

    data = [clear_system_fields(row) for row in yt.select_rows(f"* from [{path}]")]
    return sort_table(data)


def get_yt_merged_path(revision):
    return get_yt_path(revision).rsplit('-', 2)[0]


def get_yt_path(revision, suffix=None):
    if isinstance(revision, Model):
        revision = revision.__dict__

    table_name = '{search}-{index}-{id}-{suffix}'.format(suffix=suffix, **revision)
    base_path = os.path.join(settings.ISEARCH['yt']['base_path'], 'data', settings.YENV_TYPE)
    return os.path.join(base_path, table_name)


def get_yt_merged_push_path(revision):
    return get_yt_push_path(revision, add_suffix=False)


def get_yt_push_path(revision, push_time=None, add_suffix=True):
    if not push_time:
        push_time = timezone.now()

    table_name = '{search}-{index}-{id}'.format(**revision)
    if add_suffix:
        suffix = push_time.strftime('%Y-%m-%dT%H:00:00')
        table_name += f'-{suffix}'
    base_path = os.path.join(settings.ISEARCH['yt']['base_path'], 'data', settings.YENV_TYPE, 'pushes')
    return os.path.join(base_path, table_name)


def get_expected_row(doc, revision, message=None, options=None, **kwargs):
    message = message or get_base_expected_doc(doc, revision, **(options or {}))
    row = {
        'url': doc.url,
        'JsonMessage': json.dumps(message),
        'raw': doc.raw_data,
        'timestamp': doc.updated_ts,
        'deleted': False
    }
    row.update(kwargs)
    return row
