# coding: utf-8
import logging

import inject
import mock
import pytest
from sepelib.core import config as app_config
from infra.swatlib.orly_client import OrlyBrakeApplied
from infra.awacs.proto import model_pb2

from awacs.lib import itsclient, staffclient
from awacs.model.dao import IDao
from awacs.model.namespace.ctl import NamespaceCtl
from awtest import wait_until, wait_until_passes, check_log


NS_ID = 'namespace-id'


@pytest.fixture
def staff_client_mock():
    m = mock.Mock()
    m.get_groups_by_ids.return_value = {'abcd': {'url': 'svc_abcd_administration'}}
    return m


@pytest.fixture
def its_client_mock():
    m = mock.Mock()
    m.update_location_cache = {}
    m.remove_location_cache = {}
    m.config = {
        'version': 'XXX',
        'content': {'groups': {'all-service': {'groups': {
            'sas': {'groups': {}},
            'man': {'groups': {}},
            'msk': {'groups': {}},
            'iva': {'groups': {}},
            'myt': {'groups': {}},
            'vla': {'groups': {NS_ID: {
                'filter': 'I@a_nonexistent_tag'
            }}},
        }}}}
    }
    m.get_location.return_value = m.config

    def update_side_effect(location_path, content):
        curr = m.config['content']['groups']
        edges = location_path.split('/')
        assert edges[0] == 'balancer'
        for edge in edges[1:-1]:
            if edge not in curr:
                curr[edge] = {'groups': {}}
            curr = curr[edge]['groups']
        curr[edges[-1]] = content['content']
        m.update_location_cache[location_path] = content

    def remove_side_effect(location_path, version):
        curr = m.config['content']['groups']
        edges = location_path.split('/')
        assert edges[0] == 'balancer'
        for edge in edges[1:-1]:
            curr = curr.get(edge, {'groups': {}})['groups']
        curr.pop(edges[-1], None)
        m.remove_location_cache[location_path] = version

    m.create_or_update_location.side_effect = update_side_effect
    m.remove_location.side_effect = remove_side_effect
    return m


@pytest.fixture(autouse=True)
def deps(caplog, binder, dao, its_client_mock, staff_client_mock):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(itsclient.IItsClient, its_client_mock)
        b.bind(staffclient.IStaffClient, staff_client_mock)
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.fixture
def namespace_pb(cache, zk_storage):
    _ns_pb = model_pb2.Namespace()
    _ns_pb.meta.id = NS_ID
    dao = IDao.instance()
    spec_pb = model_pb2.NamespaceSpec()
    ns_pb = dao.create_namespace(meta_pb=_ns_pb.meta,
                                 login='test',
                                 spec_pb=spec_pb
                                 )
    assert wait_until(lambda: cache.get_namespace(NS_ID), timeout=1)
    return ns_pb


@pytest.fixture
def balancer_pbs(cache, zk_storage, namespace_pb):
    b_pbs = []
    dao = IDao.instance()
    for dc in ('sas', 'myt', 'iva'):
        balancer_pb = model_pb2.Balancer(meta=model_pb2.BalancerMeta(id='balancer_{}'.format(dc),
                                                                     namespace_id=namespace_pb.meta.id))
        balancer_pb.spec.config_transport.nanny_static_file.instance_tags.itype = 'balancer'
        balancer_pb.spec.config_transport.nanny_static_file.instance_tags.ctype = 'prod'
        balancer_pb.spec.config_transport.nanny_static_file.instance_tags.prj = namespace_pb.meta.id
        balancer_pb.meta.location.type = model_pb2.BalancerMeta.Location.YP_CLUSTER
        balancer_pb.meta.location.yp_cluster = dc.upper()
        balancer_pb.spec.config_transport.nanny_static_file.service_id = 'rtc_balancer_balancer_{}'.format(dc)
        b_pb = dao.create_balancer(meta_pb=balancer_pb.meta, spec_pb=balancer_pb.spec, login='LOGIN')[0]
        b_pbs.append(b_pb)
    assert wait_until(lambda: all(cache.get_balancer(NS_ID, b_pb.meta.id) for b_pb in b_pbs), timeout=1)
    return b_pbs


@pytest.fixture
def ctl(cache, zk_storage, namespace_pb):
    """
    rtype: NamespaceCtl
    """
    ctl = NamespaceCtl(NS_ID, {'name_prefix': ''})
    ctl.PROCESS_INTERVAL = 0.1
    ctl.SELF_DELETION_COOLDOWN_PERIOD = 0.1
    ctl.EVENTS_QUEUE_GET_TIMEOUT = 0.1
    ctl._its_processor.set_pb(namespace_pb)
    ctl._its_processor._needs_its_sync = mock.Mock()
    return ctl


def test_its_sync_enabled(ctl, ctx, caplog, namespace_pb):
    with check_log(caplog) as log:
        ctl._its_processor.process(ctx, None)
        assert 'Namespace sync ITS config started' not in log.records_text()

    namespace_pb.spec.its.knobs.common_knobs.add(its_ruchka_id='cplb_balancer_load_switch')
    ctl._its_processor.set_pb(namespace_pb)
    with check_log(caplog) as log:
        ctl._its_processor.process(ctx, None)
        assert 'Namespace sync ITS config started' in log.records_text()

    namespace_pb.spec.balancer_constraints.instance_tags.ClearField('prj')
    ctl._its_processor.set_pb(namespace_pb)
    with pytest.raises(AssertionError) as e:
        ctl._its_processor.process(ctx, None)
    assert str(e.value) == 'Namespace without "spec.balancer_constraints.instance_tags.prj" found'

    app_config.set_value('run.disable_its_sync', True)
    with check_log(caplog) as log:
        ctl._its_processor.process(ctx, None)
        assert 'Namespace sync ITS config started' not in log.records_text()


def test_its_configs(ctl, ctx, caplog, namespace_pb, cache, balancer_pbs, its_client_mock):
    namespace_pb.spec.its.knobs.by_balancer_knobs.add(its_ruchka_id='service_balancer_off')
    ctl._its_processor.set_pb(namespace_pb)
    with check_log(caplog) as log:
        ctl._its_processor.process(ctx, None)
        assert 'Updated section balancer/namespace-id' in log.records_text()
        assert 'Deleted section balancer/all-service/vla/namespace-id' in log.records_text()

    def check():
        status = cache.must_get_namespace(NS_ID).its_sync_status
        assert status.last_successful_attempt == status.last_attempt
        assert status.last_successful_attempt.succeeded.status == 'True'
        assert status.last_attempt.succeeded.status == 'True'

    wait_until_passes(check)

    location_path = 'balancer/{}'.format(NS_ID)
    assert list(its_client_mock.update_location_cache.keys()) == [location_path]
    content = its_client_mock.update_location_cache[location_path]
    assert content == {
        'version': 'XXX',
        'content': {
            'groups': {
                'sas': {
                    'groups': {
                        'sas': {
                            'filter': 'I@a_itype_balancer . I@a_prj_namespace-id . I@a_ctype_prod . I@a_geo_sas',
                            'responsible': ['robot-searchmon'],
                            'ruchkas': [u'service_balancer_off']
                        }
                    }
                },
                'msk': {
                    'groups': {
                        'msk': {
                            'filter': 'I@a_itype_balancer . I@a_prj_namespace-id . I@a_ctype_prod . I@a_geo_msk',
                            'responsible': ['robot-searchmon'],
                            'ruchkas': [u'service_balancer_off']
                        }
                    }
                },
            }
        }
    }

    assert list(its_client_mock.remove_location_cache.keys()) == ['balancer/all-service/vla/{}'.format(NS_ID)]

    namespace_pb.spec.its.knobs.common_knobs.add(its_ruchka_id='cplb_balancer_load_switch')
    namespace_pb.spec.its.knobs.split_msk.value = True
    namespace_pb.spec.its.acl.staff_group_ids.append('12345')
    namespace_pb.spec.its.acl.logins.append('robot-ferenets')
    ctl._its_processor.set_pb(namespace_pb)
    with check_log(caplog) as log:
        ctl._its_processor.process(ctx, None)
        assert 'Updated section balancer/namespace-id' in log.records_text()

    def check():
        status = cache.must_get_namespace(NS_ID).its_sync_status
        assert status.last_successful_attempt == status.last_attempt
        assert status.last_successful_attempt.succeeded.status == 'True'
        assert status.last_attempt.succeeded.status == 'True'

    wait_until_passes(check)

    assert list(its_client_mock.update_location_cache.keys()) == [location_path]
    content = its_client_mock.update_location_cache[location_path]
    assert content == {
        'version': 'XXX',
        'content': {
            'groups': {
                'sas': {
                    'groups': {
                        'sas': {
                            'filter': 'I@a_itype_balancer . I@a_prj_namespace-id . I@a_ctype_prod . I@a_geo_sas',
                            'responsible': ['robot-searchmon'],
                            'ruchkas': [u'service_balancer_off'],
                            'managers': {'logins': ['robot-ferenets'], 'groups': ['svc_abcd_administration']}
                        }
                    }
                },
                'myt': {
                    'groups': {
                        'myt': {
                            'filter': 'I@a_itype_balancer . I@a_prj_namespace-id . I@a_ctype_prod . I@a_geo_msk . I@a_dc_myt',
                            'responsible': ['robot-searchmon'],
                            'ruchkas': [u'service_balancer_off'],
                            'managers': {'logins': ['robot-ferenets'], 'groups': ['svc_abcd_administration']}
                        }
                    }
                },
                'iva': {
                    'groups': {
                        'iva': {
                            'filter': 'I@a_itype_balancer . I@a_prj_namespace-id . I@a_ctype_prod . I@a_geo_msk . I@a_dc_iva',
                            'responsible': ['robot-searchmon'],
                            'ruchkas': [u'service_balancer_off'],
                            'managers': {'logins': ['robot-ferenets'], 'groups': ['svc_abcd_administration']}
                        }
                    }
                },
                'common': {
                    'groups': {
                        'common': {
                            'filter': 'I@a_itype_balancer . I@a_prj_namespace-id . [I@a_ctype_prod] . [I@a_geo_msk I@a_geo_sas]',
                            'responsible': ['robot-searchmon'],
                            'ruchkas': [u'cplb_balancer_load_switch'],
                            'managers': {'logins': ['robot-ferenets'], 'groups': ['svc_abcd_administration']}
                        }
                    }
                },
            }
        }
    }

    assert list(its_client_mock.remove_location_cache.keys()) == ['balancer/all-service/vla/{}'.format(NS_ID)]

    namespace_pb.spec.its.knobs.create_announce_knob_for_marty.value = True
    ctl._its_processor.set_pb(namespace_pb)
    with check_log(caplog) as log:
        ctl._its_processor.process(ctx, None)
        assert 'Updated section balancer/all-service/sas/namespace-id' in log.records_text()
        assert 'Updated section balancer/all-service/msk/namespace-id' in log.records_text()

    assert list(its_client_mock.remove_location_cache.keys()) == ['balancer/all-service/vla/{}'.format(NS_ID)]

    assert 'balancer/all-service/msk/{}'.format(NS_ID) in its_client_mock.update_location_cache.keys()
    assert 'balancer/all-service/sas/{}'.format(NS_ID) in its_client_mock.update_location_cache.keys()
    msk_all_service_content = its_client_mock.update_location_cache['balancer/all-service/msk/{}'.format(NS_ID)]
    assert msk_all_service_content == {
        'version': 'XXX',
        'content': {
            'filter': 'I@a_itype_balancer . I@a_prj_namespace-id . I@a_ctype_prod . I@a_geo_msk',
            'ruchkas': ['service_balancer_off'],
            'responsible': ['robot-searchmon']
        }
    }


def test_orly_forbidden(ctx, ctl, namespace_pb):
    namespace_pb.spec.its.knobs.by_balancer_knobs.add(its_ruchka_id='service_balancer_off')
    ctl._its_processor.set_pb(namespace_pb)
    with mock.patch.object(ctl._its_processor, '_sync_brake') as sync_brake:
        sync_brake.maybe_apply = mock.Mock(side_effect=OrlyBrakeApplied)
        ctl._its_processor._its_sync_attempt = mock.Mock()
        ctl._its_processor.process(ctx, None)
        ctl._its_processor._its_sync_attempt.assert_not_called()


def test_maybe_self_delete(ctx, ctl, namespace_pb, its_client_mock):
    its_client_mock.get_location.return_value = {
        'version': 'XXX',
        'content': {'groups': {NS_ID: {'filter': 'xxx'}}}
    }
    assert ctl._its_processor.maybe_self_delete(ctx)
    namespace_pb.spec.its.knobs.by_balancer_knobs.add(its_ruchka_id='service_balancer_off')
    assert not ctl._its_processor.maybe_self_delete(ctx)
    its_client_mock.get_location.return_value = {'content': {'groups': {'all-service': {'groups': {'msk': {'groups': {NS_ID: {'filter': 'xxx'}}}}}}}}
    assert not ctl._its_processor.maybe_self_delete(ctx)
    its_client_mock.get_location.return_value = {'content': {'groups': {'all-service': {'groups': {}}}}}
    assert ctl._its_processor.maybe_self_delete(ctx)


def test_ctl_version(ctl, ctx, caplog, namespace_pb, cache, balancer_pbs, its_client_mock):
    namespace_pb.spec.its.knobs.create_announce_knob_for_marty.value = True
    ctl._its_processor.set_pb(namespace_pb)

    with check_log(caplog) as log:
        ctl._its_processor.process(ctx, None)
        assert 'Deleted section balancer/all-service/vla/namespace-id' in log.records_text()
        assert 'Updated section balancer/all-service/msk/namespace-id' in log.records_text()
        assert 'Updated section balancer/all-service/sas/namespace-id' in log.records_text()

    namespace_pb.spec.its.ctl_version = 1
    ctl._its_processor.set_pb(namespace_pb)

    with check_log(caplog) as log:
        ctl._its_processor.process(ctx, None)
        assert 'Deleted section balancer/all-service/msk/namespace-id' in log.records_text()
        assert 'Updated section balancer/all-service/myt/namespace-id' in log.records_text()
        assert 'Updated section balancer/all-service/iva/namespace-id' in log.records_text()
        assert 'Updated section balancer/all-service/sas/namespace-id' not in log.records_text()

    myt_all_service_content = its_client_mock.update_location_cache['balancer/all-service/myt/{}'.format(NS_ID)]
    assert myt_all_service_content == {
        'version': 'XXX',
        'content': {
            'filter': 'I@a_itype_balancer . I@a_prj_namespace-id . I@a_ctype_prod . I@a_geo_msk . I@a_dc_myt',
            'ruchkas': ['service_balancer_off'],
            'responsible': ['robot-searchmon']
        }
    }

    namespace_pb.spec.its.ctl_version = 0
    ctl._its_processor.set_pb(namespace_pb)

    with check_log(caplog) as log:
        ctl._its_processor.process(ctx, None)
        assert 'Updated section balancer/all-service/msk/namespace-id' in log.records_text()
        assert 'Deleted section balancer/all-service/myt/namespace-id' in log.records_text()
        assert 'Deleted section balancer/all-service/iva/namespace-id' in log.records_text()

    namespace_pb.spec.its.ctl_version = 2
    namespace_pb.spec.its.knobs.common_knobs.add(its_ruchka_id='cplb_balancer_load_switch')
    namespace_pb.spec.its.knobs.by_balancer_knobs.add(its_ruchka_id='service_balancer_off')
    ctl._its_processor.set_pb(namespace_pb)

    with check_log(caplog) as log:
        ctl._its_processor.process(ctx, None)
        assert 'Updated section balancer/namespace-id' in log.records_text()
        assert 'Deleted section balancer/all-service/msk/namespace-id' in log.records_text()
        assert 'Updated section balancer/all-service/myt/namespace-id' in log.records_text()
        assert 'Updated section balancer/all-service/iva/namespace-id' in log.records_text()
        assert 'Updated section balancer/all-service/sas/namespace-id' in log.records_text()

    assert its_client_mock.config == {
        'version': 'XXX',
        'content': {
            'groups': {
                'all-service': {
                    'groups': {
                        'man': {
                            'groups': {}
                        },
                        'vla': {
                            'groups': {}
                        },
                        'msk': {
                            'groups': {}
                        },
                        'sas': {
                            'groups': {
                                'namespace-id': {
                                    'filter': 'f@rtc_balancer_balancer_sas',
                                    'responsible': ['robot-searchmon'],
                                    'ruchkas': [u'service_balancer_off'],
                                }
                            }
                        },
                        'myt': {
                            'groups': {
                                'namespace-id': {
                                    'filter': 'f@rtc_balancer_balancer_myt',
                                    'responsible': ['robot-searchmon'],
                                    'ruchkas': [u'service_balancer_off'],
                                }
                            }
                        },
                        'iva': {
                            'groups': {
                                'namespace-id': {
                                    'filter': 'f@rtc_balancer_balancer_iva',
                                    'responsible': ['robot-searchmon'],
                                    'ruchkas': [u'service_balancer_off'],
                                }
                            }
                        },
                    }
                },
                'namespace-id': {
                    'groups': {
                        'common': {
                            'groups': {
                                'common': {
                                    'filter': 'f@rtc_balancer_balancer_iva f@rtc_balancer_balancer_myt f@rtc_balancer_balancer_sas',
                                    'responsible': ['robot-searchmon'],
                                    'ruchkas': [u'cplb_balancer_load_switch'],
                                }
                            }
                        },
                        'sas': {
                            'groups': {
                                'sas': {
                                    'filter': 'f@rtc_balancer_balancer_sas',
                                    'responsible': ['robot-searchmon'],
                                    'ruchkas': [u'service_balancer_off'],
                                }
                            }
                        },
                        'msk': {
                            'groups': {
                                'msk': {
                                    'filter': 'f@rtc_balancer_balancer_iva f@rtc_balancer_balancer_myt',
                                    'responsible': ['robot-searchmon'],
                                    'ruchkas': [u'service_balancer_off'],
                                }
                            }
                        },
                    }
                }
            }
        }
    }
