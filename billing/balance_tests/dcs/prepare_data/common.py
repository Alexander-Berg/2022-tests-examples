import collections
import datetime as dt

from balance.actions.dcs.logic import DCSLogic

from tests.balance_tests.dcs.dcs_common import yt_client_mock


def _run_prepare_data(prepare_func, min_finish_dt, object_or_objects, **kwargs):
    if min_finish_dt is None:
        min_finish_dt = dt.datetime(2000, 1, 1)

    if not isinstance(object_or_objects, collections.Iterable):
        object_or_objects = (object_or_objects, )

    cluster = 'hahn'
    output_path = '//table'

    rows = []

    # noinspection PyUnusedLocal
    def write_table_side_effect(path, stream, *args, **kwargs):
        assert path == output_path
        rows.extend(stream)

    with yt_client_mock() as m:
        client = m.return_value
        client.write_table.side_effect = write_table_side_effect

        prepare_func(cluster, output_path, min_finish_dt, objects=object_or_objects, **kwargs)

        client.create.assert_called_once()
        client.write_table.assert_called_once()

    return rows


def run_prepare_data_for_caob(object_or_objects, min_finish_dt=None):
    return _run_prepare_data(
        DCSLogic().nirvana_prepare_data_for_caob,
        min_finish_dt,
        object_or_objects,
    )


def run_prepare_data_for_ccaob(object_or_objects, min_finish_dt=None, excluded_attributes=None):
    return _run_prepare_data(
        DCSLogic().nirvana_prepare_data_for_ccaob,
        min_finish_dt,
        object_or_objects,
        excluded_attributes=excluded_attributes,
    )
