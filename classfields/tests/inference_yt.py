import subprocess
from tqdm import tqdm
import yt.wrapper as yt
import pandas as pd

import auto.api.api_offer_model_pb2 as api_offer_model

import auto.api.stats_model_pb2 as stats_model

yt.config["proxy"]["url"] = "hahn"

TESTING_HOST = 'prediction-api-int.vrts-slb.test.vertis.yandex.net'
LOCALHOST = 'localhost:8895'

table_path = "//home/verticals/vsml/autoru_price_predict/VSML-1049/offers_auction_all"
hostname = TESTING_HOST

data = pd.read_csv('./cme_price_forecasts-2.csv', index_col=0)
cm_pred = data[(data.index.value_counts() == 1)].to_dict(orient='index')

iter_table = yt.read_table(table_path,
                           format=yt.YsonFormat(encoding=None))


def write_request_proto_to_file(request, output_file='proto.request.example.binary'):
    with open(output_file, 'w+b') as file:
        file.write(request.SerializeToString())


def get_prediction(request_filepath, model_type, normed):
    curl_call_string = f"curl -X POST \
                         -H 'Content-Type: application/protobuf' \
                         -H 'Accept: application/protobuf'  \
                         --data-binary @{request_filepath} \
                         'http://{hostname}/price/cars?model={model_type}&normed={normed}&include_features=false&' \
                          > response{model_type}{normed}"

    subprocess.check_call(curl_call_string, shell=True)

    price_resp = stats_model.PredictPrice()
    with open(f'response{model_type}{normed}', 'rb') as read_file:
        binary_string = read_file.read()
    price_resp.ParseFromString(binary_string)
    return price_resp


res = {}

for row in tqdm(iter_table):
    offer = api_offer_model.Offer()
    offer.ParseFromString(row[b'raw_offer_vos'])

    request_filepath = f"request_{offer.id}"
    write_request_proto_to_file(offer, request_filepath)

    key = f'{offer.car_info.mark}_{offer.car_info.model}_{offer.id}'
    res[key] = {'price': offer.price_info.rur_price, 'cm': cm_pred.get(offer.id, {})}

    for model_type, normed in [('general', 'true'), ('general', 'false'),
                               ('dealers', 'true'), ('dealers', 'false')]:
        price_resp = get_prediction(request_filepath, model_type, normed)
        res[key][f'{model_type}_{normed}'] = (getattr(price_resp.autoru, "from") + price_resp.autoru.to) / 2

    print(key, res[key], '\n')
