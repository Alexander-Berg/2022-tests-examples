from yandex.maps.proto.factory import delivery_pb2

from common import canonical_message


def test_delivery():
    msg = delivery_pb2.Delivery()
    msg.id = "1"
    msg.name = "DIGITAL_GLOBE"
    msg.year = 2021
    msg.copyrights.append("DG")
    msg.downloadUrl = "ftp://server/path"
    msg.downloadEnabled = True
    msg.etag = "asdghjjkl"
    return canonical_message(msg, "delivery")


def test_delivery_data():
    msg = delivery_pb2.DeliveryData()
    msg.name = "DIGITAL_GLOBE"
    msg.year = 2021
    msg.copyrights.append("DG")
    msg.downloadUrl = "ftp://server/path"
    msg.downloadEnabled = True
    return canonical_message(msg, "delivery_data")
