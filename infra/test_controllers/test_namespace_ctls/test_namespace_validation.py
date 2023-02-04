# coding: utf-8

import textwrap
import pytest

from infra.awacs.proto import model_pb2
from awacs.lib.rpc import exceptions
from awacs.web.validation import proto_readonly


@pytest.fixture
def namespace_spec_pb():
    spec_pb = model_pb2.NamespaceSpec()
    spec_pb.preset = model_pb2.NamespaceSpec.PR_DEFAULT
    spec_pb.alerting.version = '0.1.0'
    spec_pb.alerting.juggler_raw_downtimers.staff_group_ids.extend((12345,))
    notify_rule = spec_pb.alerting.juggler_raw_notify_rules.balancer.add()
    notify_rule.template_name = 'on_status_change'
    notify_rule.template_kwargs = textwrap.dedent(
        """\
        status:
          - from: WARN
            to: CRIT
          - from: OK
            to: CRIT
        login:
          - '@svc_12345'
        method:
          - telegram
        """)

    return spec_pb


def test_readonly_no_changes(namespace_spec_pb):
    proto_readonly.validate_readonly_fields(namespace_spec_pb, namespace_spec_pb)


def test_readonly_safe_changes(namespace_spec_pb):
    new_pb = model_pb2.NamespaceSpec()
    new_pb.CopyFrom(namespace_spec_pb)

    new_pb.incomplete = True
    new_pb.deleted = True
    del new_pb.alerting.juggler_raw_notify_rules.balancer[:]
    new_pb.alerting.version = '0.2.0'

    proto_readonly.validate_readonly_fields(namespace_spec_pb, new_pb)
    proto_readonly.validate_readonly_fields(new_pb, namespace_spec_pb)


def test_add_remove_readonly_oneof(namespace_spec_pb):
    new_pb = model_pb2.NamespaceSpec()
    new_pb.CopyFrom(namespace_spec_pb)
    new_pb.alerting.notify_rules_disabled = True
    with pytest.raises(exceptions.BadRequestError):
        proto_readonly.validate_readonly_fields(namespace_spec_pb, new_pb)

    with pytest.raises(exceptions.BadRequestError):
        proto_readonly.validate_readonly_fields(new_pb, namespace_spec_pb)


def test_change_readonly_oneof(namespace_spec_pb):
    new_pb = model_pb2.NamespaceSpec()
    new_pb.CopyFrom(namespace_spec_pb)

    namespace_spec_pb.alerting.notify_rules_disabled = True
    new_pb.alerting.notify_rules_disabled = False

    with pytest.raises(exceptions.BadRequestError):
        proto_readonly.validate_readonly_fields(namespace_spec_pb, new_pb)

    with pytest.raises(exceptions.BadRequestError):
        proto_readonly.validate_readonly_fields(new_pb, namespace_spec_pb)


def test_change_readonly_bool(namespace_spec_pb):
    new_pb = model_pb2.NamespaceSpec()
    new_pb.CopyFrom(namespace_spec_pb)

    namespace_spec_pb.alerting.balancer_checks_disabled = True

    with pytest.raises(exceptions.BadRequestError):
        proto_readonly.validate_readonly_fields(namespace_spec_pb, new_pb)

    with pytest.raises(exceptions.BadRequestError):
        proto_readonly.validate_readonly_fields(new_pb, namespace_spec_pb)


def test_change_readonly_enum(namespace_spec_pb):
    new_pb = model_pb2.NamespaceSpec()
    new_pb.CopyFrom(namespace_spec_pb)

    new_pb.preset = model_pb2.NamespaceSpec.PR_WITHOUT_NOTIFICATIONS

    with pytest.raises(exceptions.BadRequestError):
        proto_readonly.validate_readonly_fields(namespace_spec_pb, new_pb)

    with pytest.raises(exceptions.BadRequestError):
        proto_readonly.validate_readonly_fields(new_pb, namespace_spec_pb)

    namespace_spec_pb.preset = model_pb2.NamespaceSpec.PR_WITHOUT_NOTIFICATIONS

    proto_readonly.validate_readonly_fields(namespace_spec_pb, new_pb)
    proto_readonly.validate_readonly_fields(new_pb, namespace_spec_pb)


def test_add_remove_message_with_readonly(namespace_spec_pb):
    namespace_spec_pb.alerting.balancer_checks_disabled = True
    new_pb = model_pb2.NamespaceSpec()
    new_pb.CopyFrom(namespace_spec_pb)
    new_pb.ClearField('alerting')

    proto_readonly.validate_readonly_fields(namespace_spec_pb, new_pb)

    with pytest.raises(exceptions.BadRequestError):
        proto_readonly.validate_readonly_fields(new_pb, namespace_spec_pb)

    namespace_spec_pb.alerting.balancer_checks_disabled = False

    proto_readonly.validate_readonly_fields(namespace_spec_pb, new_pb)
    proto_readonly.validate_readonly_fields(new_pb, namespace_spec_pb)
