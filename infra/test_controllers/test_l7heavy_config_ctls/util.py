# coding: utf-8
import six

from awtest import shift
from awtest.api import Api


class L7HeavyRevStatus(object):
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
        :type i_pb: model_pb2.L7HeavyInProgressCondition
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
        return isinstance(other, L7HeavyRevStatus) and str(self) == str(other)

    def __str__(self):
        return '{:<10} {:<10} {:<10}'.format(self.v, self.i, self.a)

    __repr__ = __str__


class L7HeavyRevStatusList(object):
    def __init__(self, rev_statuses):
        """
        :type rev_statuses: list[L7HeavyRevStatus]
        """
        self.rev_statuses = rev_statuses

    @property
    def last_rev(self):
        return self.rev_statuses[-1]

    @classmethod
    def from_pb(cls, l7heavy_statuses_pb):
        """
        :type l7heavy_statuses_pb: model_pb2.L7HeavyBalancerState.RevisionL7HeavyStatuses
        """
        return cls(rev_statuses=[L7HeavyRevStatus.from_pb(status_pb) for status_pb in l7heavy_statuses_pb.statuses])

    def __eq__(self, other):
        return isinstance(other, L7HeavyRevStatusList) and str(self) == str(other)

    def __str__(self):
        lines = []
        for rev_status in reversed(self.rev_statuses):
            lines.append(str(rev_status))
        return '\n'.join(lines)

    __repr__ = __str__


class L7HeavyState(object):
    def __init__(self, l7heavy_config, weight_sections=None, pb=None):
        self.l7heavy_config = self.maybe_to_linear_status(l7heavy_config)
        self.weight_sections = {weight_section_id: self.maybe_to_linear_status(weight_section)
                                for weight_section_id, weight_section in six.iteritems(weight_sections)} if weight_sections else {}
        self.pb = pb

    @staticmethod
    def maybe_to_linear_status(obj):
        if isinstance(obj, L7HeavyRevStatusList):
            return obj
        else:
            if isinstance(obj, list) and all([isinstance(o, L7HeavyRevStatus) for o in obj]):
                return L7HeavyRevStatusList(obj)
            else:
                raise AssertionError('obj is not a list[RevStatus]')

    def __eq__(self, other):
        return isinstance(other, L7HeavyState) and str(self) == str(other)

    @classmethod
    def from_api(cls, namespace_id, l7heavy_config_id):
        state_pb = Api.get_l7heavy_config_state(namespace_id=namespace_id, l7heavy_config_id=l7heavy_config_id)
        return cls.from_pb(state_pb)

    @classmethod
    def from_pb(cls, state_pb):
        """
        :type state_pb: model_pb2.L7HeavyConfigState
        :rtype: L7HeavyState
        """
        return cls(
            l7heavy_config=L7HeavyRevStatusList.from_pb(state_pb.l7heavy_config),
            weight_sections={weight_section_id: L7HeavyRevStatusList.from_pb(rev_status_pb)
                             for weight_section_id, rev_status_pb in state_pb.weight_sections.items()},
            pb=state_pb
        )

    def __str__(self):
        lines = ['\n{:<20} {}'.format('* L7Heavy config', shift(six.text_type(self.l7heavy_config)))]
        if self.weight_sections:
            lines.append('weight_sections')
            for weight_section_id, weight_section in sorted(self.weight_sections.items()):
                lines.append('  {:<18} {}'.format('* ' + weight_section_id, shift(six.text_type(weight_section))))
        return '\n'.join(lines) + '\n'

    __repr__ = __str__


f_f_f = L7HeavyRevStatus('False', 'False', 'False')
t_f_f = L7HeavyRevStatus('True', 'False', 'False')
t_f_t = L7HeavyRevStatus('True', 'False', 'True')
u_f_f = L7HeavyRevStatus('Unknown', 'False', 'False')
