from infra.swatlib import sandbox


class MockSandboxClient(sandbox.SandboxClient):
    def get_task(self, task_id):
        if task_id == '637214020':
            return {'type': 'BUILD_BALANCER_BUNDLE'}
        if task_id == '639956062':
            return {'type': 'BUILD_INSTANCE_CTL'}
        return {'type': 'RANDOM_TASK'}

    def get_resource(self, resource_id):
        if resource_id == '1416163086':
            return {
                'type': 'BALANCER_EXECUTABLE',
                'skynet_id': 'rbtorrent:8616184c36d85d294d2bb2e7aa6906a45089680d'
            }
        if resource_id == '1423168226':
            return {
                'type': 'INSTANCECTL',
                'skynet_id': 'rbtorrent:54af0d45b0349d85c260ebdef64306768b6eed5c'
            }
        return {
            'type': 'RANDOM_RESOURCE',
            'skynet_id': 'rbtorrent:0000000000000000000000000000000000000000'
        }
