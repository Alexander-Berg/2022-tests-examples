from yandex.maps.proto.factory import release_pb2

from common import canonical_message, set_time, set_bbox


def test_release():
    msg = release_pb2.Release()
    msg.id = "1"
    msg.name = "test_release"
    msg.currentStatus = release_pb2.Release.DRAFT
    msg.targetStatus = release_pb2.Release.TESTING
    msg.etag = "abc"
    msg.issueId = 1
    return canonical_message(msg, 'release')


def test_release_status_transitions():
    msg = release_pb2.ReleaseStatusTransitions()
    st_msg = msg.statusTransitions.add()
    st_msg.transitionStatus = release_pb2.ReleaseStatusTransition.RUNNING
    st_msg.initialStatus = release_pb2.Release.DRAFT
    st_msg.targetStatus = release_pb2.Release.TESTING
    st_msg.progress = 66

    log_msg = st_msg.logs.add()
    set_time(log_msg.createdAt, 1594310100)
    log_msg.text = "Starting"

    set_time(st_msg.startedAt, 1594310099)
    return canonical_message(msg, 'status_transitions')


def test_validation():
    msg = release_pb2.Validation()
    msg.id = "1"
    msg.releaseId = "2"
    msg.status = release_pb2.Validation.RUNNING
    set_time(msg.createdAt, 1594310099)

    r_msg = msg.results.add()
    err_msg = r_msg.errors.add()
    err_msg.wrongZoomMaxValue.mosaicId = "100"
    err_msg.wrongZoomMaxValue.mosaicZoomMax = 18
    err_msg.wrongZoomMaxValue.conflictedMosaicId = "50"
    err_msg.wrongZoomMaxValue.conflictedMosaicZoomMax = 19
    return canonical_message(msg, "validation")
