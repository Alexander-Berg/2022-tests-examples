# coding: utf-8
import six

from awtest.api import Api
from awtest.core import shift


class L7RevStatus(object):
    def __init__(self, v, i, a, v_message=None):
        """
        :type v: six.string_types
        :type i: six.string_types
        :type a: six.string_types
        :type v_message: six.string_types | None
        """
        self.v = v
        self.i = i
        self.a = a
        self.v_message = v_message

    @classmethod
    def from_pbs(cls, v_pb, i_pb, a_pb):
        """
        :type v_pb: model_pb2.Condition
        :type i_pb: model_pb2.InProgressCondition
        :type a_pb: model_pb2.Condition
        """
        return cls(v=v_pb.status,
                   i=i_pb.status,
                   a=a_pb.status,
                   v_message=v_pb.message)

    @classmethod
    def from_pb(cls, rev_status_pb):
        return cls.from_pbs(v_pb=rev_status_pb.validated,
                            i_pb=rev_status_pb.in_progress,
                            a_pb=rev_status_pb.active)

    def __eq__(self, other):
        return isinstance(other, L7RevStatus) and str(self) == str(other)

    def __str__(self):
        return '{:<10} {:<10} {:<10}'.format(self.v, self.i, self.a)

    __repr__ = __str__


class L7RevStatusList(object):
    def __init__(self, rev_statuses):
        """
        :type rev_statuses: list[L7RevStatus]
        """
        self.rev_statuses = rev_statuses

    @property
    def last_rev(self):
        return self.rev_statuses[-1]

    @classmethod
    def from_pb(cls, statuses_pb):
        """
        :type statuses_pb: model_pb2.L7BalancerState.RevisionStatuses
        """
        return cls(rev_statuses=[L7RevStatus.from_pb(status_pb) for status_pb in statuses_pb.statuses])

    def __eq__(self, other):
        if isinstance(other, list):
            other = L7RevStatusList(other)
        return isinstance(other, L7RevStatusList) and str(self) == str(other)

    def __str__(self):
        lines = []
        for rev_status in reversed(self.rev_statuses):
            lines.append(str(rev_status))
        return '\n'.join(lines)

    __repr__ = __str__


class L7State(object):
    def __init__(self, balancer,
                 upstreams=None, domains=None, backends=None, endpoint_sets=None, knobs=None, certs=None):
        self.balancer = self.maybe_to_linear_status(balancer)
        self.domains = {domain_id: self.maybe_to_linear_status(domain)
                        for domain_id, domain in six.iteritems(domains)} if domains else {}
        self.upstreams = {upstream_id: self.maybe_to_linear_status(upstream)
                          for upstream_id, upstream in six.iteritems(upstreams)} if upstreams else {}
        self.backends = {backend_id: self.maybe_to_linear_status(backend)
                         for backend_id, backend in six.iteritems(backends)} if backends else {}
        self.endpoint_sets = {endpoint_set_id: self.maybe_to_linear_status(es)
                              for endpoint_set_id, es in six.iteritems(endpoint_sets)} if endpoint_sets else {}
        self.knobs = {knob_id: self.maybe_to_linear_status(knob)
                      for knob_id, knob in six.iteritems(knobs)} if knobs else {}
        self.certs = {cert_id: self.maybe_to_linear_status(cert)
                      for cert_id, cert in six.iteritems(certs)} if certs else {}

    @staticmethod
    def maybe_to_linear_status(obj):
        if isinstance(obj, L7RevStatusList):
            return obj
        else:
            if isinstance(obj, list) and all([isinstance(o, L7RevStatus) for o in obj]):
                return L7RevStatusList(obj)
            else:
                raise AssertionError('obj is not a list[L7RevStatus]')

    def __eq__(self, other):
        return isinstance(other, L7State) and str(self) == str(other)

    @classmethod
    def from_api(cls, namespace_id, balancer_id):
        state_pb = Api.get_balancer_state(namespace_id=namespace_id, balancer_id=balancer_id)
        return cls.from_pb(state_pb)

    @classmethod
    def from_pb(cls, state_pb):
        """
        :type state_pb: model_pb2.BalancerState
        :rtype: L7State
        """
        return cls(
            balancer=L7RevStatusList.from_pb(state_pb.balancer),
            domains={domain_id: L7RevStatusList.from_pb(rev_status_pb)
                     for domain_id, rev_status_pb in state_pb.domains.items()},
            upstreams={upstream_id: L7RevStatusList.from_pb(rev_status_pb)
                       for upstream_id, rev_status_pb in state_pb.upstreams.items()},
            backends={backend_id: L7RevStatusList.from_pb(rev_status_pb)
                      for backend_id, rev_status_pb in state_pb.backends.items()},
            endpoint_sets={endpoint_set_id: L7RevStatusList.from_pb(rev_status_pb)
                           for endpoint_set_id, rev_status_pb in state_pb.endpoint_sets.items()},
            knobs={knob_id: L7RevStatusList.from_pb(rev_status_pb)
                   for knob_id, rev_status_pb in state_pb.knobs.items()},
            certs={cert_id: L7RevStatusList.from_pb(rev_status_pb)
                   for cert_id, rev_status_pb in state_pb.certificates.items()},
        )

    def __str__(self):
        lines = ['{:<20} {}'.format('balancer', shift(str(self.balancer)))]
        if self.domains:
            lines.append('domains')
            for domain_id, domain in sorted(self.domains.items()):
                lines.append(' {:<19} {}'.format('* ' + domain_id, shift(str(domain))))
        if self.upstreams:
            lines.append('upstreams')
            for upstream_id, upstream in sorted(self.upstreams.items()):
                lines.append(' {:<19} {}'.format('* ' + upstream_id, shift(str(upstream))))
        if self.backends:
            lines.append('backends')
            for backend_id, backend in sorted(self.backends.items()):
                lines.append(' {:<19} {}'.format('* ' + backend_id, shift(str(backend))))
        if self.endpoint_sets:
            lines.append('endpoint sets')
            for endpoint_set_id, endpoint_set in sorted(self.endpoint_sets.items()):
                lines.append(' {:<19} {}'.format('* ' + endpoint_set_id, shift(str(endpoint_set))))
        if self.knobs:
            lines.append('knobs')
            for knob_id, knob in sorted(self.knobs.items()):
                lines.append(' {:<19} {}'.format('* ' + knob_id, shift(str(knob))))
        if self.certs:
            lines.append('certs')
            for cert_id, cert in sorted(self.certs.items()):
                lines.append(' {:<19} {}'.format('* ' + cert_id, shift(str(cert))))
        return '\n'.join(lines)

    __repr__ = __str__


p_f_f = L7RevStatus('Pending', 'False', 'False')
f_f_f = L7RevStatus('False', 'False', 'False')
t_f_f = L7RevStatus('True', 'False', 'False')
t_t_f = L7RevStatus('True', 'True', 'False')
t_t_t = L7RevStatus('True', 'True', 'True')
t_f_t = L7RevStatus('True', 'False', 'True')
u_f_f = L7RevStatus('Unknown', 'False', 'False')
