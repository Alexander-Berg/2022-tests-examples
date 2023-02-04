import sys
import subprocess
from io import BytesIO

from google.protobuf.internal.encoder import _EncodeVarint
import yt.wrapper as yt

sys.path.append('./proto/')
from realty.offer import common_pb2, RealtySchema_pb2
from time import time
from tqdm import tqdm

yt.config["proxy"]["url"] = "hahn"

TESTING_HOST = 'realty-embedder-api.vrts-slb.test.vertis.yandex.net'
LOCALHOST = 'localhost:8895'


def writeToDelimitedString(obj, stream=None):
    """
    Stanford CoreNLP uses the Java "writeDelimitedTo" function, which
    writes the size (and offset) of the buffer before writing the object.
    This function handles parsing this message starting from offset 0.
    @returns how many bytes of @buf were consumed.
    """
    if stream is None:
        stream = BytesIO()

    _EncodeVarint(stream.write, obj.ByteSize(), True)
    stream.write(obj.SerializeToString())
    return stream


def test_one_request_api(request_filepath='proto.request.binary',
                         response_filepath='proto.response.binary',
                         hostname=LOCALHOST):
    start_time = time()
    curl_call_string = 'curl -s --header "Content-Type: application/protobuf" --request POST --data-binary @' + request_filepath + ' http://' + hostname + '/api/v1/batch/get_embedding > ' + response_filepath

    subprocess.check_call(curl_call_string, shell=True)

    end_time = time()
    print((end_time - start_time) * 1000)

    return 0



def test_on_yt_offers(table_path="//home/verticals/realty/testing/export/offers/snapshot",
                      sampling_rate=0.001,
                      hostname=LOCALHOST):
    errors = 0
    stream = BytesIO()
    for index, row in tqdm(enumerate(yt.read_table(table_path,
                                                   format=yt.YsonFormat(encoding=None),
                                                   table_reader={"sampling_seed": 42,
                                                                 "sampling_rate": sampling_rate}))):
        request = RealtySchema_pb2.OfferMessage()
        request.ParseFromString(row[b'offer'])

        writeToDelimitedString(request, stream)
        with open('proto.request', 'w+b') as file:
            file.write(stream.getvalue())

        errors += test_one_request_api(request_filepath='proto.request',
                                       response_filepath='proto.response.binary',
                                       hostname=hostname)

    print("Total {0} errors".format(errors))


test_on_yt_offers(table_path="//home/verticals/realty/production/export/offers/snapshot", sampling_rate=0.01,
                  hostname=TESTING_HOST)
