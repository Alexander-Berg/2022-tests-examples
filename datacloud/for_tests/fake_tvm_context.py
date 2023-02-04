class FakeServiceContext:
    def __init__(self, tvm_src, secret, tvm_keys):
        pass

    def sign(self, ts, tvm_dst):
        return 'some-fake-sign'


def get_fake_service_context(tvm_src, tvm_keys):
    return FakeServiceContext(tvm_src, 'fake-secret', tvm_keys)
