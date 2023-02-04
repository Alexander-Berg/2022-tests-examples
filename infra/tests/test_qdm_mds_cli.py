from infra.qyp.qdm.src.mds.client import QdmUploadJob
from infra.qyp.qdm.src.server import main as main_srv  # noqa


def test_fake():
    job = QdmUploadJob('fakehost', 1234, 'fakekey')
    del job
