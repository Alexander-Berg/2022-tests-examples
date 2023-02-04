import sys
import subprocess
import os
import logging
from collections import defaultdict
from argparse import ArgumentParser

from tqdm import tqdm
import pandas as pd
import numpy as np
import yt.wrapper as yt

sys.path.append('./proto')
from realty.prediction.price_prediction_pb2 import PricePredictionResponse, PricePredictionLandingRequest
from realty.offer import common_pb2, RealtySchema_pb2
from realty import model_pb2
from realty.offer.unified_offer_parts import address_part_pb2
from google.protobuf.message import DecodeError

yt.config["proxy"]["url"] = "hahn"

LOCALHOST = 'localhost:8895'

logging.basicConfig(
    format=u'[# %(levelname)-8s [%(asctime)s]  %(message)s',
    level=logging.INFO
)


def make_landing_message(row):
    message = PricePredictionLandingRequest()

    message.apartment.apartment_area = row["area"]
    message.apartment.rooms_total = row["rooms_offered"]
    # message.apartment.highrise_apartment_info.floors.append(data_nirvana["floor"])
    message.apartment.general_apartment_info.ceiling_height.value = row['ceiling_height']
    message.apartment.building_info.built_year.value = row['build_year']
    message.apartment.building_info.flats_count.value = row['flats_count']
    message.apartment.building_info.expect_demolition.value = bool(row['expect_demolition'])
    message.apartment.building_info.has_lift.value = bool(row['has_elevator'])
    message.apartment.building_info.building_id.value = int(row['building_id'])
    message.apartment.building_info.building_series.id = row['building_series_id']
    message.apartment.building_info.building_type = row['building_type']
    message.apartment.building_info.floors_total.value = row['floors_total']

    message.location.geocoder_coordinates.latitude = row['lat']
    message.location.geocoder_coordinates.longitude = row['lon']

    c = address_part_pb2.Address.Component()
    c.region_type = model_pb2.RegionType.Value('CITY')
    c.value = row['locality_name']
    message.location.geocoder_address.component.append(c)
    message.location.geocoder_address.unified_oneline = row["unified_address"]

    message.location.subject_federation_geoid = row["subject_federation_id"]
    return message


def create_protobuf(row, protobuf_model="offer"):
    request = RealtySchema_pb2.OfferMessage()
    request.ParseFromString(row[b'offer'])
    if protobuf_model == "offer":
        return request
    elif protobuf_model == "landing":
        data = {
            "area": request.transaction.area.value,
            "rooms_offered": request.apartmentInfo.roomsOffered,
            'ceiling_height': request.apartmentInfo.ceilingHeight,
            'build_year': request.buildingInfo.buildYear,
            'flats_count': request.buildingInfo.flatsCount,
            'expect_demolition': request.buildingInfo.expectDemolition,
            'has_elevator': request.buildingInfo.new_model.has_lift.value or None,
            'building_id': request.buildingInfo.buildingId,
            'building_series_id': request.buildingInfo.buildingSeriesId,
            # 'building_type': request.buildingInfo.buildingTypeInt,
            'building_type': request.buildingInfo.buildingTypeInt,
            'floors_total': request.buildingInfo.new_model.building_type,
            'lat': request.location.geocoderPoint.latitude,
            'lon': request.location.geocoderPoint.longitude,
            'locality_name': request.location.localityName,
            "unified_address": request.location.geocoderAddress,
            "subject_federation_id": request.location.subjectFederationId,
        }
        request = make_landing_message(data)
    else:
        raise ValueError("protobuf_model must be 'offer' or 'landing'")
    return request


def write_request_proto_to_file(request, output_file='proto.request.example.binary'):
    global result
    result = request.SerializeToString()
    with open(output_file, 'w+b') as file:
        file.write(result)


def one_request_api(price_pred_request,
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

    price_resp = PricePredictionResponse()
    with open(response_filepath, 'r+b') as read_file:
        binary_string = read_file.read()
    price_resp.ParseFromString(binary_string)

    os.remove(request_filepath)
    return price_resp


def process_yt_offers(table_path, hostname, path, protobuf_model, data_path=None):
    logging.info("Start quality estimation")
    logging.info(f"table_path: {table_path}")
    logging.info(f"hostname: {hostname}")
    logging.info(f"path: {path}")
    logging.info(f"protobuf_model: {protobuf_model}")
    iter_table = yt.read_table(
        table_path,
        format=yt.YsonFormat(encoding=None)
    )

    results = []
    for row in tqdm(iter_table):
        request = create_protobuf(row, protobuf_model)
        try:
            price_resp_plain = one_request_api(request,
                                                   request_filepath='proto.request.sell.msc.example.binary',
                                                   response_filepath='proto.response.sell.msc.example.binary',
                                                   hostname=hostname,
                                                   path=path)
        except DecodeError as e:
            logging.info(e)

        results.append({
            "coarse_subject_id": row[b'coarse_subject_id'],
            "subject_id": row[b'subject_id'],
            "price": row[b'price'],
            "old_predicted_price": row[b'predicted_price'],
            "new_predicted_price": price_resp_plain.predicted_price.value,
            "new_predicted_q05": price_resp_plain.predicted_price.q05,
            "new_predicted_q25": price_resp_plain.predicted_price.q25,
            "new_predicted_q75": price_resp_plain.predicted_price.q75,
            "new_predicted_q95": price_resp_plain.predicted_price.q95,
        })

    df_result = pd.DataFrame(results)
    df_result["old_mape"] = np.abs(df_result["old_predicted_price"] - df_result["price"]) / df_result["price"]
    df_result["new_mape"] = np.abs(df_result["new_predicted_price"] - df_result["price"]) / df_result["price"]
    df_result["q25_q75_weight"] = ((df_result["price"] > df_result["new_predicted_q25"]) & \
                                  (df_result["price"] < df_result["new_predicted_q75"])).astype("double")
    df_result["q05_q95_weight"] = ((df_result["price"] > df_result["new_predicted_q05"]) & \
                                  (df_result["price"] < df_result["new_predicted_q95"])).astype("double")

    result = df_result.groupby("coarse_subject_id")[["old_mape", "new_mape", "q25_q75_weight", "q05_q95_weight"]].mean()
    print(result)
    if data_path is not None:
        df_result.to_csv(data_path)


def read_args():
    parser = ArgumentParser()
    parser.add_argument("--path", default="get_price_offer")
    parser.add_argument("--hostname", default=LOCALHOST)
    parser.add_argument("--table_path")
    parser.add_argument("--data_path", nargs='?')
    parser.add_argument("--protobuf_model", default="offer")
    args = parser.parse_args()
    return args


if __name__ == "__main__":
    args = read_args()
    process_yt_offers(
        table_path=args.table_path,
        hostname=args.hostname,
        path=args.path,
        protobuf_model=args.protobuf_model,
        data_path=args.data_path
    )
