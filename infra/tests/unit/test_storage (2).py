from infra.rsc.src.model.storage import IndexedStorage, PodIndexedStorage
import yp.data_model as data_model


def test_indexed_storage():
    pod_set1 = data_model.TPodSet()
    pod_set1.meta.id = 'ps1'
    pod_set2 = data_model.TPodSet()
    pod_set2.meta.id = 'ps2'
    objs = [pod_set1, pod_set2]

    storage = IndexedStorage.make_from_objs(objs)
    assert set(storage.list_ids()) == {'ps1', 'ps2'}

    storage2 = IndexedStorage()
    storage2.put(storage.get('ps1'))
    assert storage2.get('ps1') == pod_set1

    storage.merge(storage2)
    assert storage.get('ps2') is None

    storage.replace([], 'today')
    assert storage.list_ids() == []
    assert storage.last_replace_time == 'today'


def test_pod_indexed_storage():
    pod1, pod2, pod3 = [data_model.TPod() for _ in range(3)]
    pod1.meta.id, pod1.meta.pod_set_id = 'p1', 'ps1'
    pod2.meta.id, pod2.meta.pod_set_id = 'p2', 'ps1'
    pod3.meta.id, pod3.meta.pod_set_id = 'p3', 'ps2'
    pods = [pod3, pod2, pod1]

    storage = PodIndexedStorage()
    storage.put(pod1)
    storage.last_replace_time = 'yesterday'
    assert storage.list_by_ps_id('ps1') == [pod1], storage.list_by_ps_id('ps2') == []

    storage.replace(pods, 'today')
    assert storage.list_by_ps_id('ps1') == [pod2, pod1], storage.list_by_ps_id('ps2') == [pod3]
    assert storage.last_replace_time == 'today'
