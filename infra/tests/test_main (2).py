import logging
import mock
import pytest
import yatest.common

from infra.awacs.proto import model_pb2

from awacsctl2.cli import Ctl
from awacsctl2.marshaller import yml_to_balancer
from awacsctl2.lib.yamlutil import compute_doc_boundaries, get_docs
from awacsctl2 import marshaller


logger = logging.getLogger("test_logger")
p = logger.info


def test_ctl():
    config_path = yatest.common.test_source_path('./awacsctl-test.cfg')
    Ctl(config_path=config_path)


def test_yamlutil():
    yml = '''
---
doc1
---
doc2
---
'''
    assert compute_doc_boundaries(yml) == [(1, 3), (3, 5), (5, 6)]
    docs = get_docs(yml)
    assert docs[0] == '---\ndoc1\n'
    assert docs[1] == '---\ndoc2\n'
    assert docs[2] == '---\n'

    yml = '''doc1
---
doc2'''
    assert compute_doc_boundaries(yml) == [(0, 1), (1, 3)]
    docs = get_docs(yml)
    assert docs[0] == 'doc1\n'
    assert docs[1] == '---\ndoc2\n'

    yml = '''doc1
---
{% for x in xs %}some yaml{% endfor %}'''
    assert compute_doc_boundaries(yml, assume_jinja2=True) == [(0, 1), (1, 3)]
    docs = get_docs(yml, assume_jinja2=True)
    assert docs[0] == 'doc1\n'
    assert docs[1] == '---\n{% for x in xs %}some yaml{% endfor %}\n'


def test_marshaller():
    yml = """
---
auth.staff.owners:
    logins: [romanovich, nekto0n]
    groups: [yandex_search_tech_searchinfradev_cluster, yandex_mnt_sa_runtime_mondev]
labels:
    key1: value1
    key2: value2
---
regexp_section:
  matcher:
    match_fsm:
      host: face-jstracer1.n.yandex-team.ru(:\\d+)?
  modules: []
"""
    group_ids = {
        'yandex_search_tech_searchinfradev_cluster': '1',
        'yandex_mnt_sa_runtime_mondev': '2',
    }
    group_urls = dict((v, k) for k, v in group_ids.iteritems())
    with mock.patch.object(marshaller, '_staff_group_url_to_id', side_effect=group_ids.get):
        auth_pb, spec_pb = marshaller.yml_to_upstream(yml)

    assert auth_pb.type == model_pb2.Auth.STAFF
    assert auth_pb.staff.owners.logins == ['romanovich', 'nekto0n']
    assert auth_pb.staff.owners.group_ids == ['1', '2']

    assert spec_pb.labels == {
        'key1': 'value1',
        'key2': 'value2',
    }
    assert spec_pb.yandex_balancer.yaml == get_docs(yml)[1]

    with mock.patch.object(marshaller, '_staff_group_id_to_url', side_effect=group_urls.get):
        result, _ = marshaller.upstream_to_yml(auth_pb, spec_pb)
    expected = '''auth:
  staff:
    owners:
      logins:
      - romanovich
      - nekto0n
      groups:
      - yandex_search_tech_searchinfradev_cluster
      - yandex_mnt_sa_runtime_mondev
labels:
  key1: value1
  key2: value2
---
regexp_section:
  matcher:
    match_fsm:
      host: face-jstracer1.n.yandex-team.ru(:\d+)?
  modules: []\n'''  # noqa: W605
    assert result == expected

    yml = '''auth:
  staff:
    owners:
      logins:
      - romanovich
      - nekto0n
      groups:
      - yandex_search_tech_searchinfradev_cluster
      - yandex_mnt_sa_runtime_mondev
---
regexp_section:
  matcher:
    match_fsm:
      host: face-jstracer1.n.yandex-team.ru(:\d+)?
  modules: []'''  # noqa: W605
    with pytest.raises(ValueError) as e:
        with mock.patch.object(marshaller, '_staff_group_url_to_id', side_effect=group_ids.get):
            yml_to_balancer(yml)
    assert e.match('must contain "config_transport" field')

    yml = '''auth.staff.owners:
  logins:
  - romanovich
  - nekto0n
  groups:
  - yandex_search_tech_searchinfradev_cluster
  - yandex_mnt_sa_runtime_mondev
config_transport.nanny_static_file:
  service_id: production_nanny
  snapshot_priority: NONE
template_engine: DYNAMIC_JINJA2
---
instance_macro:
  maxconn: 4000
  buffer: 1048576
  log_dir: /place/db/www/logs/
  sections:
    admin:
      ips: [127.0.0.1, '::1']
      ports: [8180]
      modules:
        - http: {}
        - admin: {}'''
    with mock.patch.object(marshaller, '_staff_group_url_to_id', side_effect=group_ids.get):
        auth_pb, spec_pb = yml_to_balancer(yml)

    assert auth_pb.type == model_pb2.Auth.STAFF
    assert auth_pb.staff.owners.logins == ['romanovich', 'nekto0n']
    assert auth_pb.staff.owners.group_ids == ['1', '2']

    assert spec_pb.yandex_balancer.yaml == get_docs(yml)[1]
    nsf_pb = spec_pb.config_transport.nanny_static_file
    assert nsf_pb.service_id == 'production_nanny'
    assert nsf_pb.snapshot_priority == nsf_pb.NONE

    with mock.patch.object(marshaller, '_staff_group_id_to_url', side_effect=group_urls.get):
        with mock.patch.object(marshaller, '_staff_group_url_to_id', side_effect=group_ids.get):
            assert (
                marshaller.yml_to_balancer(marshaller.balancer_to_yml(auth_pb, spec_pb)[0]) ==
                (auth_pb, spec_pb)
            )

    yml = '''auth.staff.owners:
  logins:
  - romanovich
  - nekto0n
  groups:
  - yandex_search_tech_searchinfradev_cluster
  - yandex_mnt_sa_runtime_mondev
labels:
  key1: value1
  key2: value2
port.override: 8000
use_mtn: true
nanny_snapshots:
- service_id: xxx
  snapshot_id: yyy
  port.shift: 2
  weight.override: 100
  use_mtn: false
'''
    with mock.patch.object(marshaller, '_staff_group_url_to_id', side_effect=group_ids.get):
        auth_pb, spec_pb = marshaller.yml_to_backend(yml)

    assert auth_pb.type == model_pb2.Auth.STAFF
    assert auth_pb.staff.owners.logins == ['romanovich', 'nekto0n']
    assert auth_pb.staff.owners.group_ids == ['1', '2']

    assert spec_pb.labels == {
        'key1': 'value1',
        'key2': 'value2',
    }
    s_pb = spec_pb.selector
    assert s_pb.use_mtn
    assert s_pb.port.policy == s_pb.port.OVERRIDE
    assert s_pb.port.override == 8000
    assert len(s_pb.nanny_snapshots) == 1
    snapshot_pb = s_pb.nanny_snapshots[0]
    assert snapshot_pb.service_id == 'xxx'
    assert snapshot_pb.snapshot_id == 'yyy'
    assert snapshot_pb.port.policy == snapshot_pb.port.SHIFT
    assert snapshot_pb.port.shift == 2
    assert snapshot_pb.weight.policy == snapshot_pb.weight.OVERRIDE
    assert snapshot_pb.weight.override == 100
    assert snapshot_pb.HasField('use_mtn') and snapshot_pb.use_mtn.value is False

    yml = '''auth.staff.owners:
  logins:
  - romanovich
  - nekto0n
  groups:
  - yandex_search_tech_searchinfradev_cluster
  - yandex_mnt_sa_runtime_mondev
port.shift: 2
gencfg_groups:
- name: xxx
  version: yyy
  port.override: 2
  use_mtn: true
'''
    with mock.patch.object(marshaller, '_staff_group_url_to_id', side_effect=group_ids.get):
        auth_pb, spec_pb = marshaller.yml_to_backend(yml)

    assert not spec_pb.labels
    s_pb = spec_pb.selector
    assert not s_pb.use_mtn
    assert s_pb.port.policy == s_pb.port.SHIFT
    assert s_pb.port.shift == 2
    assert len(s_pb.gencfg_groups) == 1
    group_pb = s_pb.gencfg_groups[0]
    assert group_pb.name == 'xxx'
    assert group_pb.version == 'yyy'
    assert group_pb.port.policy == group_pb.port.OVERRIDE
    assert group_pb.port.override == 2
    assert not group_pb.HasField('weight')
    assert group_pb.HasField('use_mtn') and group_pb.use_mtn.value is True

    with mock.patch.object(marshaller, '_staff_group_id_to_url', side_effect=group_urls.get):
        with mock.patch.object(marshaller, '_staff_group_url_to_id', side_effect=group_ids.get):
            assert marshaller.yml_to_backend(marshaller.backend_to_yml(auth_pb, spec_pb)[0]) == (auth_pb, spec_pb)
