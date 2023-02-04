import requests
import sys
import yt.wrapper as yt

sys.path.append('./proto')
from proto.realty.offer import common_pb2, RealtySchema_pb2


YT_TABLE = '//home/verticals/realty/production/export/offers/snapshot[:#100]'
yt.config["proxy"]["url"] = "hahn"

TESTING_HOST = 'lb-int-01-myt.test.vertis.yandex.net'
PRICE_ESTIMATOR_REST_API = '/api/v2/get_price_offer'
HEADERS = {
    'Host': 'realty-price-estimator.vrts-slb.test.vertis.yandex.net',
    'Connection': 'close',
    'Content-Type': 'application/protobuf'
}


def acceptable_for_prediction(o):
    offer_type = o.offerTypeInt
    offer_category = o.categoryTypeInt
    subject_federation_id = o.location.subjectFederationId

    is_sell = offer_type == common_pb2.SELL
    is_monthly_rented = offer_type == common_pb2.RENT & o.transaction.price.periodInt == 4

    is_apartment = offer_category == common_pb2.APARTMENT
    is_room = offer_category == common_pb2.ROOMS
    is_house = offer_category == common_pb2.HOUSE

    is_msc = subject_federation_id == 1
    is_spb = subject_federation_id == 10174

    is_new_flat = o.apartmentInfo.flatTypeInt == common_pb2.FLAT_TYPE_NEW_FLAT

    return (~is_new_flat & (is_apartment | is_room) & (is_monthly_rented | is_sell)) | \
           (is_house & is_sell & (is_msc | is_spb))


def result_request(request):
    headers = ''.join('{0}: {1}\r\n'.format(k, v) for k, v in request.headers.items())
    req = "{method} {path_url} HTTP/1.1\r\n{headers}\r\n".format(
        method=request.method,
        path_url=request.path_url,
        headers=headers)
    byte_string = bytes(req, encoding='utf-8') + request.body
    req_size = len(byte_string)
    final_byte_string = bytes(str(req_size) + '\n', encoding='utf-8') + byte_string + bytes('\r\n', encoding='utf-8')
    return final_byte_string


def post_binary(host, port, namespace, headers, protobuf_obj):
    req = requests.Request(
        'POST',
        'https://{host}:{port}{namespace}'.format(
            host=host,
            port=port,
            namespace=namespace,
        ),
        headers=headers,
        data=protobuf_obj.SerializeToString()
    )
    prepared = req.prepare()
    return result_request(prepared)


def write_ammo_binary_to_file(output_file='patrons_protobuf.file'):
    proto_request = RealtySchema_pb2.OfferMessage()

    for row in yt.read_table(YT_TABLE, format=yt.YsonFormat(encoding=None)):
        proto_request = RealtySchema_pb2.OfferMessage()
        proto_request.ParseFromString(row[b'offer'])
        if acceptable_for_prediction(proto_request):
            break

    result = post_binary(host=TESTING_HOST, port=80, namespace=PRICE_ESTIMATOR_REST_API, headers=HEADERS,
                         protobuf_obj=proto_request)
    file = open(output_file, 'w+b')
    file.write(result)
    file.close()


write_ammo_binary_to_file('./stress-test/patrons_protobuf.file')
