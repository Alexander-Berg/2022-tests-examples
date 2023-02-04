import yandex.maps.geolib3 as geo
from yandex.maps.geolib3.proto import encode_proto_polyline
from yandex.maps.proto.factory import mosaics_pb2

from common import canonical_message, set_time, set_bbox


def make_closed_polyline(points):
    polygon = geo.Polyline2()
    for lon, lat in points:
        polygon.add(geo.Point2(lon, lat))
    polygon.add(geo.Point2(points[0][0], points[0][1]))
    return polygon


def set_polygon(msg_polygon, points):
    polyline = make_closed_polyline(points)
    poly_proto = encode_proto_polyline(polyline)
    msg_polygon.outer_ring.lons.CopyFrom(poly_proto.lons)
    msg_polygon.outer_ring.lats.CopyFrom(poly_proto.lats)


def test_mosaic_source():
    msg = mosaics_pb2.MosaicSource()
    msg.id = "1"
    msg.name = "012191381010_08_P001"
    msg.satellite = "WV02"

    set_polygon(msg.geometry.add(), [(55, 37), (56, 37), (56, 38), (55, 38)])
    set_bbox(msg.boundingBox, 55, 37, 56, 38)

    kv_msg = msg.metadata.add()
    kv_msg.key = "sun_elev"
    kv_msg.value = "54.4"

    msg.resolutionMeterPerPx = 0.5
    msg.offnadir = 15
    msg.heading = 90
    msg.tags.append("bad_quality")
    set_time(msg.collectionDate, 1594310100)
    msg.project = "Moscow"
    msg.deliveryId = "10"
    msg.etag = "213213"

    return canonical_message(msg, 'mosaic_source')


def test_mosaic():
    msg = mosaics_pb2.Mosaic()
    msg.id = "1"
    msg.mosaicSourceId = "2"
    msg.releaseId = "3"
    msg.mercatorShift.x = 10
    msg.mercatorShift.y = -0.5
    msg.zoomMin = 10
    msg.zoomMax = 19
    msg.zIndex = 100500

    msg.colorCorrectionParams.hue = 100
    msg.colorCorrectionParams.saturation = 80
    msg.colorCorrectionParams.lightness = 50

    msg.sharpingParams.sigma = 0.5
    msg.sharpingParams.radius = 1
    msg.etag = "sadsadhdjka"
    return canonical_message(msg, 'mosaic')
