from __future__ import unicode_literals
import mock

import yp.data_model

from infra.mc_rsc.src import reflector


def test_reflector():
    updated_ts_selector = '/spec'

    p1 = yp.data_model.TPod()
    p1.meta.id = 'p1'
    p1.meta.pod_set_id = 'ps1'
    p2 = yp.data_model.TPod()
    p2.meta.id = 'p2'
    p2.meta.pod_set_id = 'ps1'
    p3 = yp.data_model.TPod()
    p3.meta.id = 'p3'
    p3.meta.pod_set_id = 'ps1'

    ts1 = {updated_ts_selector: 1 << 30}
    ts2 = {updated_ts_selector: 2 << 30}
    ts3 = {updated_ts_selector: 3 << 30}

    gen_ts = 5 << 30
    yp_client = mock.Mock()
    yp_client.loader.get_updated_time_selector_by_object_type.return_value = updated_ts_selector
    yp_client.generate_timestamp.return_value = gen_ts
    yp_client.select_object_ids.return_value = ['p1', 'p2', 'p3']
    yp_client.get_objects.return_value = [(p1, ts1), (p2, ts2), (p3, ts3)]

    s = mock.Mock()
    s.size.return_value = 0  # just for logging size in reflector
    r = reflector.Reflector(
        name='pod',
        cluster='sas-test',
        obj_type=yp.data_model.OT_POD,
        obj_class=yp.data_model.TPod,
        obj_filter='some-filter',
        selectors=[],
        watch_selectors=[],
        use_watches=True,
        fetch_timestamps=True,
        storage=s,
        client=yp_client,
        select_ids_batch_size=100,
        get_objects_batch_size=100,
        watch_time_limit_secs=0,
        event_count_limit=100,
        sleep_secs=0,
        select_threads_count=1,
        metrics_registry=mock.Mock(),
        full_sync_storage=None
    )
    r.start(gen_ts)
    timestamped_objects = [(p1, ts1[updated_ts_selector] >> 30),
                           (p2, ts2[updated_ts_selector] >> 30),
                           (p3, ts3[updated_ts_selector] >> 30)]
    assert yp_client.generate_timestamp.call_count == 0
    assert yp_client.watch_objects.call_count == 0
    assert yp_client.select_object_ids.call_count == 1
    assert yp_client.get_objects.call_args.kwargs['timestamp'] == gen_ts
    assert s.sync_with_objects.call_args == mock.call(objs_with_timestamps=timestamped_objects)
    assert r.last_sync_timestamp == gen_ts

    gen_ts = 6 << 30
    yp_client.reset_mock()
    s.reset_mock()

    p4 = yp.data_model.TPod()
    p4.meta.id = 'p4'
    p4.meta.pod_set_id = 'ps1'

    ts4 = {updated_ts_selector: 4 << 30}

    e1 = yp.data_model.TEvent()
    e1.event_type = yp.data_model.ET_OBJECT_UPDATED
    e1.object_id = p1.meta.id
    e2 = yp.data_model.TEvent()
    e2.event_type = yp.data_model.ET_OBJECT_REMOVED
    e2.timestamp = ts2[updated_ts_selector]
    e2.object_id = p2.meta.id
    e3 = yp.data_model.TEvent()
    e3.event_type = yp.data_model.ET_OBJECT_CREATED
    e3.object_id = p4.meta.id

    yp_client.watch_objects.return_value = [e1, e2, e3]
    yp_client.get_objects.return_value = [(p1, ts1), (p4, ts4)]

    r.last_sync_timestamp
    r.start(gen_ts)
    assert yp_client.generate_timestamp.call_count == 0
    assert yp_client.select_object_ids.call_count == 0
    assert yp_client.watch_objects.call_count == 1
    assert yp_client.get_objects.call_args.kwargs['timestamp'] == gen_ts
    assert yp_client.get_objects.call_args.kwargs['ids'] == set([p1.meta.id, p4.meta.id])

    assert s.method_calls == [
        mock.call.remove(p2.meta.id, ts2[updated_ts_selector] >> 30),
        mock.call.replace(p1, ts1[updated_ts_selector] >> 30),
        mock.call.replace(p4, ts4[updated_ts_selector] >> 30),
        mock.call.size()
    ]
    assert r.last_sync_timestamp == gen_ts
