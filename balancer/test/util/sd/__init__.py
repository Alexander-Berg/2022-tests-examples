# -*- coding: utf-8 -*-

import multiprocessing

import infra.yp_service_discovery.api.api_pb2 as api

from balancer.test.util.proto.handler import server
from balancer.test.util.predef import http


class SDCacheMock(server.http.PreparseHandler):
    def handle_parsed_request(self, raw_request, stream):
        request = api.TReqResolveEndpoints()
        request.ParseFromString(raw_request.data.content)

        if request.client_name != "balancer_functional_test":
            raise RuntimeError()

        endpoint_set = self.state.endpoint_sets[(request.cluster_name, request.endpoint_set_id)]

        resolved = api.TRspResolveEndpoints()
        resolved.timestamp = self.state.timestamp.value
        resolved.endpoint_set.CopyFrom(endpoint_set)

        response = http.response.ok(data=resolved.SerializeToString()).to_raw_response()
        stream.write_response(response)
        self.finish_response_impl(response)


class SDCacheState(server.State):
    def __init__(self, config):
        super(SDCacheState, self).__init__(config)
        self.manager = multiprocessing.Manager()
        self.endpoint_sets = self.manager.dict()
        self.timestamp = multiprocessing.Value('i', 0)

    def set_timestamp(self, timestamp):
        self.timestamp.value = timestamp

    def set_endpointset(self, cluster_name, endpoint_set_id, endpoints):
        endpoint_set = api.TEndpointSet()
        endpoint_set.endpoint_set_id = endpoint_set_id

        for ep_data in endpoints:
            ep = endpoint_set.endpoints.add()
            ep.fqdn = ep_data['fqdn']
            ep.port = ep_data['port']
            if 'ip6_address' in ep_data:
                ep.ip6_address = ep_data['ip6_address']
            else:
                ep.ip6_address = '::1'
            ep.ready = ep_data.get('ready', True)

        self.endpoint_sets[(cluster_name, endpoint_set_id)] = endpoint_set

    def get_endpointset(self, cluster_name, endpoint_set_id):
        return self.endpoint_sets[(cluster_name, endpoint_set_id)]


class SDCacheConfig(server.http.HTTPConfig):
    HANDLER_TYPE = SDCacheMock
    STATE_TYPE = SDCacheState

    def __init__(self):
        super(server.http.HTTPConfig, self).__init__()
