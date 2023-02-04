# coding: utf-8
import six

from awtest import shift
from awtest.api import Api


class L3RevStatus(object):
    def __init__(self, v, i, a, v_message=None):
        """
        :type v: six.string_types
        :type i: six.string_types
        :type a: six.string_types
        :type v_message: six.string_types | None
        """
        self.v = v
        self.v_message = v_message
        self.i = i
        self.a = a

    @classmethod
    def from_pbs(cls, v_pb, i_pb, a_pb):
        """
        :type v_pb: model_pb2.Condition
        :type i_pb: model_pb2.L3InProgressCondition
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
        return isinstance(other, L3RevStatus) and str(self) == str(other)

    def __str__(self):
        return '{:<10} {:<10} {:<10}'.format(self.v, self.i, self.a)

    __repr__ = __str__


class L3RevStatusList(object):
    def __init__(self, rev_statuses):
        """
        :type rev_statuses: list[L3RevStatus]
        """
        self.rev_statuses = rev_statuses

    @property
    def last_rev(self):
        return self.rev_statuses[-1]

    @classmethod
    def from_pb(cls, l3_statuses_pb):
        """
        :type l3_statuses_pb: model_pb2.L3BalancerState.RevisionL3Statuses
        """
        return cls(rev_statuses=[L3RevStatus.from_pb(l3_status_pb) for l3_status_pb in l3_statuses_pb.l3_statuses])

    def __eq__(self, other):
        return isinstance(other, L3RevStatusList) and str(self) == str(other)

    def __str__(self):
        lines = []
        for rev_status in reversed(self.rev_statuses):
            lines.append(str(rev_status))
        return '\n'.join(lines)

    __repr__ = __str__


class L3State(object):
    def __init__(self, balancer, backends=None, endpoint_sets=None, pb=None):
        self.balancer = self.maybe_to_linear_status(balancer)
        self.backends = {backend_id: self.maybe_to_linear_status(backend)
                         for backend_id, backend in six.iteritems(backends)} if backends else {}
        self.endpoint_sets = {endpoint_set_id: self.maybe_to_linear_status(endpoint_set)
                              for endpoint_set_id, endpoint_set in six.iteritems(endpoint_sets)} if endpoint_sets else {}
        self.pb = pb

    @staticmethod
    def maybe_to_linear_status(obj):
        if isinstance(obj, L3RevStatusList):
            return obj
        else:
            if isinstance(obj, list) and all([isinstance(o, L3RevStatus) for o in obj]):
                return L3RevStatusList(obj)
            else:
                raise AssertionError('obj is not a list[RevStatus]')

    def __eq__(self, other):
        return isinstance(other, L3State) and str(self) == str(other)

    @classmethod
    def from_api(cls, namespace_id, l3_balancer_id):
        state_pb = Api.get_l3_balancer_state(namespace_id=namespace_id, l3_balancer_id=l3_balancer_id)
        return cls.from_pb(state_pb)

    @classmethod
    def from_pb(cls, state_pb):
        """
        :type state_pb: model_pb2.L3BalancerState
        :rtype: L3State
        """
        return cls(
            balancer=L3RevStatusList.from_pb(state_pb.l3_balancer),
            backends={backend_id: L3RevStatusList.from_pb(rev_status_pb)
                      for backend_id, rev_status_pb in state_pb.backends.items()},
            endpoint_sets={endpoint_set_id: L3RevStatusList.from_pb(rev_status_pb)
                           for endpoint_set_id, rev_status_pb in state_pb.endpoint_sets.items()},
            pb=state_pb
        )

    def __str__(self):
        lines = ['\n{:<20} {}'.format('* L3 balancer', shift(six.text_type(self.balancer)))]
        if self.backends:
            lines.append('backends')
            for backend_id, backend in sorted(self.backends.items()):
                lines.append('  {:<18} {}'.format('* ' + backend_id, shift(six.text_type(backend))))
        if self.endpoint_sets:
            lines.append('endpoint sets')
            for endpoint_set_id, endpoint_set in sorted(self.endpoint_sets.items()):
                lines.append('  {:<18} {}'.format('* ' + endpoint_set_id, shift(six.text_type(endpoint_set))))
        return '\n'.join(lines) + '\n'

    __repr__ = __str__


f_f_f = L3RevStatus('False', 'False', 'False')
t_f_f = L3RevStatus('True', 'False', 'False')
t_t_f = L3RevStatus('True', 'True', 'False')
t_t_t = L3RevStatus('True', 'True', 'True')
t_f_t = L3RevStatus('True', 'False', 'True')
u_f_f = L3RevStatus('Unknown', 'False', 'False')
