from yp_lite_ui_repo import pod_sets_api_pb2, endpoint_sets_api_pb2


class YpLiteMockClient(object):
    def __init__(self):
        self.last_create_pod_set_request_args = ()
        self.last_remove_pod_set_request_args = ()
        self.last_list_endpoint_sets_request_args = ()
        self.last_remove_endpoint_sets_request_args = ()

    def create_pod_set(self, *args, **__):
        self.last_create_pod_set_request_args = args
        return pod_sets_api_pb2.CreatePodSetResponse(pod_set_id='test')

    @staticmethod
    def get_pod(*_, **__):
        pod_pb = pod_sets_api_pb2.GetPodResponse()
        pod_pb.pod.spec.ip6_address_requests.add(
            vlan_id='backbone',
            virtual_service_ids=['v1', 'v2']
        )
        return pod_pb

    @staticmethod
    def get_pod_set(*_, **__):
        return pod_sets_api_pb2.GetPodSetResponse()

    def remove_pod_set(self, *args, **__):
        self.last_remove_pod_set_request_args = args
        return pod_sets_api_pb2.RemovePodSetResponse()

    @staticmethod
    def get_endpoint_set(*_, **__):
        return endpoint_sets_api_pb2.GetEndpointSetResponse()

    def list_endpoint_sets(self, *args, **__):
        self.last_list_endpoint_sets_request_args = args
        resp_pb = endpoint_sets_api_pb2.ListEndpointSetsResponse()
        es = resp_pb.endpoint_sets.add()
        es.meta.id = 'user_es'
        es.meta.ownership = es.meta.USER
        es = resp_pb.endpoint_sets.add()
        es.meta.id = 'system_es'
        es.meta.ownership = es.meta.SYSTEM
        return resp_pb

    def remove_endpoint_sets(self, *args, **__):
        self.last_remove_endpoint_sets_request_args = args
        return endpoint_sets_api_pb2.ListEndpointSetsResponse()

    @staticmethod
    def create_endpoint_set(*_, **__):
        return endpoint_sets_api_pb2.CreateEndpointSetResponse()

    @staticmethod
    def validate_network_macro(*_, **__):
        return pod_sets_api_pb2.ValidateNetworkMacroResponse()

    @staticmethod
    def validate_quota_settings(*_, **__):
        return pod_sets_api_pb2.ValidateQuotaSettingsResponse()
