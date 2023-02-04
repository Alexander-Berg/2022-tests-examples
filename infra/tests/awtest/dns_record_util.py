# coding: utf-8
import six

from awtest.api import Api


class DnsRecordRevStatus(object):
    def __init__(self, v, v_message=None):
        """
        :type v: six.string_types
        :type v_message: six.string_types | None
        """
        self.v = v
        self.v_message = v_message

    @classmethod
    def from_pbs(cls, v_pb):
        """
        :type v_pb: model_pb2.Condition
        """
        return cls(v=v_pb.status,
                   v_message=v_pb.message)

    @classmethod
    def from_pb(cls, rev_status_pb):
        return cls.from_pbs(v_pb=rev_status_pb.validated)

    def __eq__(self, other):
        return isinstance(other, DnsRecordRevStatus) and str(self) == str(other)

    def __str__(self):
        return '{:<10}'.format(self.v)

    __repr__ = __str__


class DnsRecordRevStatusList(object):
    def __init__(self, rev_statuses):
        """
        :type rev_statuses: list[DnsRecordRevStatus]
        """
        self.rev_statuses = rev_statuses

    @property
    def last_rev(self):
        return self.rev_statuses[-1]

    @classmethod
    def from_pb(cls, dns_record_statuses_pb):
        """
        :type dns_record_statuses_pb: model_pb2.DnsRecordState.RevisionDnsRecordStatuses
        """
        return cls(rev_statuses=[
            DnsRecordRevStatus.from_pb(dns_record_status_pb)
            for dns_record_status_pb in dns_record_statuses_pb.statuses
        ])

    def __eq__(self, other):
        return isinstance(other, DnsRecordRevStatusList) and str(self) == str(other)

    def __str__(self):
        lines = []
        for rev_status in reversed(self.rev_statuses):
            lines.append(str(rev_status))
        return '\n'.join(lines)

    __repr__ = __str__


class DnsRecordState(object):
    def __init__(self, dns_record, backends=None, endpoint_sets=None, pb=None):
        backends = backends or {}
        endpoint_sets = endpoint_sets or {}
        self.dns_record = self.maybe_to_linear_status(dns_record)
        self.backends = {backend_id: self.maybe_to_linear_status(backend)
                         for backend_id, backend in six.iteritems(backends)}
        self.endpoint_sets = {es_id: self.maybe_to_linear_status(endpoint_set)
                              for es_id, endpoint_set in six.iteritems(endpoint_sets)}
        self.pb = pb

    @staticmethod
    def maybe_to_linear_status(obj):
        if isinstance(obj, DnsRecordRevStatusList):
            return obj
        else:
            if isinstance(obj, list) and all([isinstance(o, DnsRecordRevStatus) for o in obj]):
                return DnsRecordRevStatusList(obj)
            else:
                raise AssertionError('obj is not a list[DnsRecordRevStatus]: {}'.format(type(obj)))

    def __eq__(self, other):
        return isinstance(other, DnsRecordState) and str(self) == str(other)

    @classmethod
    def from_api(cls, namespace_id, dns_record_id):
        state_pb = Api.get_dns_record_state(namespace_id=namespace_id, dns_record_id=dns_record_id)
        return cls.from_pb(state_pb)

    @classmethod
    def from_pb(cls, state_pb):
        """
        :type state_pb: model_pb2.DnsRecordState
        :rtype: DnsRecordState
        """
        return cls(
            dns_record=DnsRecordRevStatusList.from_pb(state_pb.dns_record),
            backends={backend_id: DnsRecordRevStatusList.from_pb(rev_status_pb)
                      for backend_id, rev_status_pb in state_pb.backends.items()},
            endpoint_sets={endpoint_set_id: DnsRecordRevStatusList.from_pb(rev_status_pb)
                           for endpoint_set_id, rev_status_pb in state_pb.endpoint_sets.items()},
            pb=state_pb
        )

    def __str__(self):
        lines = ['{:<20} {}'.format('* dns_record', self.dns_record)]
        if self.backends:
            lines.append('backends')
            for backend_id, backend in sorted(self.backends.items()):
                lines.append('  {:<18} {}'.format('* ' + backend_id, backend))
        if self.endpoint_sets:
            lines.append('endpoint sets')
            for endpoint_set_id, endpoint_set in sorted(self.endpoint_sets.items()):
                lines.append('  {:<18} {}'.format('* ' + endpoint_set_id, endpoint_set))
        return '\n'.join(lines)

    __repr__ = __str__


f = DnsRecordRevStatus('False')
t = DnsRecordRevStatus('True')
u = DnsRecordRevStatus('Unknown')
