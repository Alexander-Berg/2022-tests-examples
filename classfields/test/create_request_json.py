import sys
sys.path.append('./proto.out')
from realty.prediction import price_prediction_pb2
from google.protobuf.json_format import MessageToJson
from realty.offer import common_pb2
import subprocess
import os
import json
from time import time
import datetime


TESTING_HOST = 'realty-price-estimator.vrts-slb.test.vertis.yandex.net'
LOCALHOST = 'localhost:8895'


def get_price_request(offer_type,
                      offer_category,
                      locality_name,
                      subject_federation_id,
                      latitude,
                      longitude,
                      address
                       ):
    price_prediction_request = price_prediction_pb2.PricePredictionRequest()
    price_prediction_request.offer_id = "6066490924906983915"
    price_prediction_request.offer_type = offer_type#common_pb2.SELL
    price_prediction_request.offer_category = offer_category#common_pb2.APARTMENT
    price_prediction_request.apartment_info.balcony = 2
    price_prediction_request.apartment_info.renovation = 2
    price_prediction_request.rooms_total = 3
    price_prediction_request.apartment_info.floors = 5
    price_prediction_request.apartment_info.apartments = False
    price_prediction_request.apartment_info.studio = False
    price_prediction_request.apartment_info.area = 80## проверить
    price_prediction_request.apartment_info.flat_type = 3
    price_prediction_request.building_info.floors_total = 10
    price_prediction_request.building_info.build_year = 1938
    price_prediction_request.building_info.building_series = 'неизвестно'
    price_prediction_request.location.locality_name = locality_name#'Сочи'
    price_prediction_request.location.subject_federation_id = subject_federation_id#10995
    price_prediction_request.location.geocoder_point.latitude = latitude
    price_prediction_request.location.geocoder_point.longitude = longitude
    price_prediction_request.location.geocoder_address = address#'Россия, Краснодарский край, Сочи, микрорайон Дагомыс, Батумское шоссе, 14А'
    return price_prediction_request

def write_request_json_to_file(pred_request, output_file='proto.request.example'):
    global result
    result = MessageToJson(pred_request,
        preserving_proto_field_name=True,
        including_default_value_fields=True)
    with open(output_file, 'w') as file:
        file.write(result)

def test_one_request_api(price_pred_request,
                         request_filepath = 'proto.request.rent.example',
                         response_filepath = 'proto.response.rent.example',
                         hostname = LOCALHOST):
    write_request_json_to_file(price_pred_request, output_file=request_filepath)
    start_time = time()
    curl_call_string = 'curl -s --header "Content-Type: application/json" --request POST --data @' + request_filepath + ' http://' + hostname + '/api/v2/get_price_json > ' + response_filepath

    subprocess.check_call(curl_call_string, shell='True')

    # price_resp = price_prediction_pb2.PricePredictionResponse()
    with open(response_filepath, 'r') as read_file:
        binary_string = json.load(read_file)

    os.remove(request_filepath)
    os.remove(response_filepath)
    end_time = time()
    print(binary_string)
    print((end_time - start_time)*1000)

def test_all_requests_api(hostname = LOCALHOST):

    price_prediction_request_sell_msc = get_price_request(common_pb2.SELL,
                                                          common_pb2.APARTMENT,
                                                          'Москва',
                                                          1,
                                                          55.753132, 37.583834,
                                                          'Россия, Москва, Новинский бульвар, 12'
                                                          )
    price_prediction_request_rent_msc = get_price_request(common_pb2.RENT,
                                                          common_pb2.APARTMENT,
                                                          'Москва',
                                                          1,
                                                          55.753132, 37.583834,
                                                          'Россия, Москва, Новинский бульвар, 12'
                                                          )
    price_prediction_request_sell_msc_grid = get_price_request(common_pb2.SELL,
                                                          common_pb2.APARTMENT,
                                                          'Москва',
                                                          1,
                                                          55.753132, 37.583834,
                                                          '-'
                                                          )
    price_prediction_request_rent_msc_grid = get_price_request(common_pb2.RENT,
                                                          common_pb2.APARTMENT,
                                                          'Москва',
                                                          1,
                                                          55.753132, 37.583834,
                                                          '-'
                                                          )

    ################################

    price_prediction_request_sell_reg = get_price_request(common_pb2.SELL,
                                                          common_pb2.APARTMENT,
                                                          'Самара',
                                                          11131,
                                                          53.202185, 50.119574,
                                                          'Россия, Самара, Садовая улица, 256'
                                                          )
    price_prediction_request_rent_reg = get_price_request(common_pb2.RENT,
                                                          common_pb2.APARTMENT,
                                                          'Самара',
                                                          11131,
                                                          53.202185, 50.119574,
                                                          'Россия, Самара, Садовая улица, 256'
                                                          )
    price_prediction_request_sell_reg_grid = get_price_request(common_pb2.SELL,
                                                          common_pb2.APARTMENT,
                                                          'Самара',
                                                          11131,
                                                          53.202185, 50.119574,
                                                          '-'
                                                          )
    price_prediction_request_rent_reg_grid = get_price_request(common_pb2.RENT,
                                                          common_pb2.APARTMENT,
                                                          'Самара',
                                                          11131,
                                                          53.202185, 50.119574,
                                                          '-'
                                                          )

    ################################

    price_prediction_request_sell_spb = get_price_request(common_pb2.SELL,
                                                          common_pb2.APARTMENT,
                                                          'Санкт-Петербург',
                                                          10174,
                                                          59.936037, 30.359478,
                                                          'Россия, Санкт-Петербург, улица Жуковского, 28'
                                                          )
    price_prediction_request_rent_spb = get_price_request(common_pb2.RENT,
                                                          common_pb2.APARTMENT,
                                                          'Санкт-Петербург',
                                                          10174,
                                                          59.936037, 30.359478,
                                                          'Россия, Санкт-Петербург, улица Жуковского, 28'
                                                          )
    price_prediction_request_sell_spb_grid = get_price_request(common_pb2.SELL,
                                                          common_pb2.APARTMENT,
                                                          'Санкт-Петербург',
                                                          10174,
                                                          59.936037, 30.359478,
                                                          '-'
                                                          )
    price_prediction_request_rent_spb_grid = get_price_request(common_pb2.RENT,
                                                          common_pb2.APARTMENT,
                                                          'Санкт-Петербург',
                                                          10174,
                                                          59.936037, 30.359478,
                                                          '-'
                                                          )

    print("="*30)
    print("Msc Sell:")
    test_one_request_api(price_prediction_request_sell_msc, request_filepath ='proto.request.sell.msc.example',
                        response_filepath = 'proto.response.sell.msc.example', hostname=hostname)
    print("="*30)
    print("Msc Sell grid:")
    test_one_request_api(price_prediction_request_sell_msc_grid, request_filepath ='proto.request.sell.msc.example',
                        response_filepath = 'proto.response.sell.msc.example', hostname=hostname)
    print("="*30)
    print("Msc Rent:")
    test_one_request_api(price_prediction_request_rent_msc, request_filepath = 'proto.request.rent.example',
                       response_filepath = 'proto.response.rent.example', hostname=hostname)
    print("="*30)
    print("Msc Rent grid:")
    test_one_request_api(price_prediction_request_rent_msc_grid, request_filepath = 'proto.request.rent.example',
                       response_filepath = 'proto.response.rent.example', hostname=hostname)


    print('\n'+"="*30)
    print("Spb Sell:")
    test_one_request_api(price_prediction_request_sell_spb, request_filepath ='proto.request.sell.msc.example',
                        response_filepath = 'proto.response.sell.msc.example', hostname=hostname)
    print("="*30)
    print("Spb Sell grid:")
    test_one_request_api(price_prediction_request_sell_spb_grid, request_filepath ='proto.request.sell.msc.example',
                        response_filepath = 'proto.response.sell.msc.example', hostname=hostname)
    print("="*30)
    print("Spb Rent:")
    test_one_request_api(price_prediction_request_rent_spb, request_filepath = 'proto.request.rent.example',
                       response_filepath = 'proto.response.rent.example', hostname=hostname)
    print("="*30)
    print("Spb Rent grid:")
    test_one_request_api(price_prediction_request_rent_spb_grid, request_filepath = 'proto.request.rent.example',
                       response_filepath = 'proto.response.rent.example', hostname=hostname)


    print('\n'+"="*30)
    print("Reg Sell:")
    test_one_request_api(price_prediction_request_sell_reg, request_filepath ='proto.request.sell.msc.example',
                        response_filepath = 'proto.response.sell.msc.example', hostname=hostname)
    print("="*30)
    print("Reg Sell grid:")
    test_one_request_api(price_prediction_request_sell_reg_grid, request_filepath ='proto.request.sell.msc.example',
                        response_filepath = 'proto.response.sell.msc.example', hostname=hostname)
    print("="*30)
    print("Reg Rent:")
    test_one_request_api(price_prediction_request_rent_reg, request_filepath = 'proto.request.rent.example',
                       response_filepath = 'proto.response.rent.example', hostname=hostname)
    print("="*30)
    print("Reg Rent grid:")
    test_one_request_api(price_prediction_request_rent_reg_grid, request_filepath = 'proto.request.rent.example',
                       response_filepath = 'proto.response.rent.example', hostname=hostname)

test_all_requests_api(hostname=LOCALHOST)
# test_all_requests_api(TESTING_HOST)
