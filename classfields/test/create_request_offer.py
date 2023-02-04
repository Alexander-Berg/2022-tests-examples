import sys
import subprocess
import os
import logging
from collections import defaultdict
from argparse import ArgumentParser

from tqdm import tqdm
import yt.wrapper as yt

sys.path.append('./proto')
from realty.prediction import price_prediction_pb2
from realty.prediction.price_prediction_pb2 import PricePredictionResponse
from realty.offer import common_pb2, RealtySchema_pb2
from google.protobuf.message import DecodeError

yt.config["proxy"]["url"] = "hahn"

TESTING_HOST = 'realty-price-estimator.vrts-slb.test.vertis.yandex.net'
LOCALHOST = 'localhost:8895'

logging.basicConfig(
    format=u'[# %(levelname)-8s [%(asctime)s]  %(message)s',
    level=logging.INFO
)


def write_request_proto_to_file(request, output_file='proto.request.example.binary'):
    global result
    result = request.SerializeToString()
    with open(output_file, 'w+b') as file:
        file.write(result)


def test_one_request_api(price_pred_request,
                         request_filepath='proto.request.rent.example.binary',
                         response_filepath='proto.response.rent.example.binary',
                         hostname=LOCALHOST,
                         path='get_price_offer',
                         quantile_type=None) -> PricePredictionResponse:
    write_request_proto_to_file(price_pred_request, output_file=request_filepath)
    model_type = ""
    if quantile_type is not None:
        model_type = f"?model_type={quantile_type}"
    curl_call_string = f'curl -s --header "Content-Type: application/protobuf" --request POST --data-binary @{request_filepath} http://{hostname}/api/v2/{path}{model_type} > {response_filepath}'

    subprocess.check_call(curl_call_string, shell=True)

    price_resp = price_prediction_pb2.PricePredictionResponse()
    with open(response_filepath, 'r+b') as read_file:
        binary_string = read_file.read()
    price_resp.ParseFromString(binary_string)

    os.remove(request_filepath)
    return price_resp


def acceptable_for_prediction(o):
    offer_type = o.offerTypeInt
    offer_category = o.categoryTypeInt
    is_sell = offer_type == common_pb2.SELL
    is_monthly_rented = (offer_type == common_pb2.RENT) & \
                        (o.transaction.price.periodInt == 4)

    is_apartment = offer_category == common_pb2.APARTMENT
    is_new_flat = o.apartmentInfo.flatTypeInt == common_pb2.FLAT_TYPE_NEW_FLAT
    is_ok = len(o.offerState.error) == 0
    return ~is_new_flat & is_apartment & is_sell & is_ok


def test_on_yt_offers(table_path="//home/verticals/realty/testing/export/offers/snapshot",
                      sampling_rate=0.001,
                      hostname=LOCALHOST,
                      path="get_price_offer",
                      mode="raw"):
    if mode == "raw":
        print("offer_id", "offer_type", "rooms", "real_value", "old_prediction", "new_prediction")
    iter_table = yt.read_table(table_path,
                               format=yt.YsonFormat(encoding=None),
                               table_reader={
                                   "sampling_seed": 0,
                                   "sampling_rate": sampling_rate
                               })

    if mode == "stat":
        result = dict({1: defaultdict(int),
                       10174: defaultdict(int),
                       -1: defaultdict(int)})

    for row in tqdm(iter_table):
        request = RealtySchema_pb2.OfferMessage()
        request.ParseFromString(row[b'offer'])

        if acceptable_for_prediction(request):
            try:
                price_resp_plain = test_one_request_api(request,
                                                       request_filepath='proto.request.sell.msc.example.binary',
                                                       response_filepath='proto.response.sell.msc.example.binary',
                                                       hostname=hostname,
                                                       path=path)
            except DecodeError as e:
                logging.info(e)
            if mode == "raw":
                print(request.offer_id,
                      request.offerTypeInt,
                      request.apartmentInfo.rooms,
                      request.transaction.price.float_value,
                      request.predictions.price.value,
                      price_resp_plain.predicted_price.value)
            elif mode == "stat":
                subject_federation_id = request.location.subjectFederationId
                if subject_federation_id not in [1, 10174]:
                    subject_federation_id = -1

                price_min_plain = price_resp_plain.predicted_price.min
                pred_price_plain = price_resp_plain.predicted_price.value
                price_max_plain = price_resp_plain.predicted_price.max

                price_min_offer = getattr(request.predictions.price, "from")
                pred_price_offer = getattr(request.predictions.price, "value")
                price_max_offer = getattr(request.predictions.price, "to")

                result[subject_federation_id]['min_plain'] += float(price_min_offer / price_min_plain)
                result[subject_federation_id]['value_plain'] += float(pred_price_offer / pred_price_plain)
                result[subject_federation_id]['max_plain'] += float(price_max_offer / price_max_plain)
                result[subject_federation_id]['cnt'] += 1

    if mode == "stat":
        for key in [1, 10174, -1]:
            if result[key]['cnt'] == 0:
                continue
            print("SF ID", key)
            print('min_plain', result[key]['min_plain'] / result[key]['cnt'])
            print('value_plain', result[key]['value_plain'] / result[key]['cnt'])
            print('max_plain', result[key]['max_plain'] / result[key]['cnt'])


def read_args():
    parser = ArgumentParser()
    parser.add_argument("-p", "--path", default="get_price_offer")
    # parser.add_argument("-h", "--hostname", default="get_price_offer")
    args = parser.parse_args()
    return args


if __name__ == "__main__":
    args = read_args()
    path = args.path
    test_on_yt_offers(
        # table_path="//home/verticals/realty/price_estimator/VSQUALITY-3920/active_snapshot",
        table_path="//home/verticals/realty/production/export/offers/snapshot",
        # table_path="//home/verticals/realty/production/export/offers/active",
        sampling_rate=0.001,
        # hostname=TESTING_HOST,
        hostname=LOCALHOST,
        path=path,
        mode="stat"
    )
