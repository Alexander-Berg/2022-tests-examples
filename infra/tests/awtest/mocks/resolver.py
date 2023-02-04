from awacs.model.balancer.generator import Resolution
from infra.awacs.proto import internals_pb2


class ResolverStub(object):
    def __init__(self):
        self.port = 80

    def __call__(self, ctx, selector_pb, **kwargs):
        if selector_pb.nanny_snapshots and selector_pb.nanny_snapshots[0].service_id == 'service_5':
            instance_pbs = [
                internals_pb2.Instance(host='google.com', port=80, weight=1, ipv4_addr='127.0.0.2', ipv6_addr=':::2'),
            ]
        else:
            instance_pbs = [
                internals_pb2.Instance(host='ya.ru', port=self.port, weight=1, ipv4_addr='127.0.9999',
                                       ipv6_addr=':::1'),
            ]
        return Resolution(instance_pbs=instance_pbs, yp_sd_timestamps={})

    def increment_port(self):
        self.port += 1
