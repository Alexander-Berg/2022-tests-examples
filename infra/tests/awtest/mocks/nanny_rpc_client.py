from nanny_repo import repo_api_pb2


class NannyRpcMockClient(object):
    def __init__(self, *_, **__):
        pass

    @staticmethod
    def has_snapshot_been_active(*_, **__):
        return True

    @staticmethod
    def get_replication_policy(*_, **__):
        return repo_api_pb2.GetReplicationPolicyResponse()

    @staticmethod
    def get_cleanup_policy(*_, **__):
        return repo_api_pb2.GetCleanupPolicyResponse()

    def update_cleanup_policy(self, *_, **__):
        return

    def update_replication_policy(self, *_, **__):
        return
