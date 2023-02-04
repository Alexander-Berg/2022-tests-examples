from typing import List
import sys

sys.path.append("../schema")

from rest_api import UserPrediction, Call
import model_fitting.model_utils as model_utils
from schema.auto.api.vin_tf.vin_tf_pb2 import VinTfServiceResponse
from schema.vertis.telepony.model_pb2 import CallResultEnum


def parse_response_from_json(d: dict) -> List[UserPrediction]:
    res = []
    for pred_raw in d['prediction']:
        pred = UserPrediction(
            user_phone=pred_raw['user_phone'],
            buyer_probability=pred_raw['buyer_probability'],
            features=None,
            last_call=Call(
                talk_duration=pred_raw['last_call']['talk_duration'],
                time=pred_raw['last_call']['time'],
                call_result=pred_raw['last_call']['call_result'],
                caller_phone=pred_raw['last_call']['caller_phone']
            ))
        res.append(pred)
    return res


def parse_response_from_protobuf(m: VinTfServiceResponse) -> List[UserPrediction]:
    res = []
    for pred_raw in m.prediction:
        pred = UserPrediction(user_phone=pred_raw.user_phone,
                              buyer_probability=pred_raw.buyer_probability,
                              features=None,
                              last_call=Call(
                                  talk_duration=pred_raw.last_call.talk_duration.seconds,
                                  time=pred_raw.last_call.time.seconds +
                                       pred_raw.last_call.time.nanos / 1000000000,
                                  call_result=CallResultEnum.CallResult.Name(pred_raw.last_call.call_result),
                                  caller_phone=pred_raw.last_call.caller_phone))
        res.append(pred)
    return res


def predict_mockup(*args, **kwargs) -> List[UserPrediction]:
    return [
        UserPrediction(
            user_phone="+74959743581",
            last_call=Call(talk_duration=0,
                           time=1572351545.674,
                           call_result="STOP_CALLER",
                           caller_phone="+74959743581"),
            buyer_probability=0.2542775524843258,
            features=None),
        UserPrediction(
            user_phone="+79291279705",
            last_call=Call(talk_duration=14,
                           time=1571920361,
                           call_result="SUCCESS",
                           caller_phone="+79291279705"),
            buyer_probability=0.09423016524562176,
            features=None
        )
    ]


model_utils.predict = predict_mockup

from starlette.testclient import TestClient
from fastapi_main import app

client = TestClient(app)


def test_predict_json():
    response = client.get("/predict/1?n_best=2")
    assert response.status_code == 200
    assert response.headers["content-type"] == "application/json"
    expected_result = predict_mockup(None, None)
    real_result = parse_response_from_json(response.json())
    assert len(expected_result) == len(real_result)
    for p1, p2 in zip(expected_result, real_result):
        assert p1 == p2


def test_predict_protobuf():
    response = client.get("/predict/1?n_best=2", headers={"Accept": "application/protobuf"})
    assert response.status_code == 200
    assert response.headers["content-type"] == "application/protobuf"
    expected_result = predict_mockup(None, None)
    answer = VinTfServiceResponse()
    answer.ParseFromString(response.content)
    real_result = parse_response_from_protobuf(answer)
    assert len(real_result) == len(expected_result)
    for p1, p2 in zip(real_result, expected_result):
        assert p1 == p2
