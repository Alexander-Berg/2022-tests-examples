import sys

sys.path.append("../schema")

import json
from collections import defaultdict
from dataclasses import dataclass, field
from typing import Dict, List

import pytest
import numpy as np
import gzip as gz
from dateutil.parser import isoparse
from tqdm import tqdm

from rest_api import Call
from model_fitting.model_utils import Features, calculate_features, load_model, predict, UserFeatures


@dataclass
class OfferUserObject:
    features: Features = None
    proba: float = -1


@dataclass
class OfferObject:
    calls: List[Call] = field(default_factory=list)
    users: Dict[str, OfferUserObject] = field(default_factory=lambda: defaultdict(OfferUserObject))


@pytest.fixture(scope="module")
def sample_dataset() -> Dict[str, OfferObject]:
    dataset = defaultdict(OfferObject)
    with gz.open('history_sample.json.gz', 'r') as fin:
        for line in fin:
            line = json.loads(line)
            offer_id = line['offer_id']
            dataset[offer_id].calls.append(Call(
                talk_duration=line['talk_duration'] / 1000,
                time=int(isoparse(line['call_time']).timestamp()),
                call_result="???",
                caller_phone=line['caller_id']))

    with gz.open('features_sample.json.gz', 'r') as fin:
        for line in fin:
            line = json.loads(line)
            offer_id = line.pop('offer_id')
            caller_id = line.pop('caller_id')
            dataset[offer_id].users[caller_id].features = Features(**line)

    with gz.open('probas_sample.json.gz', 'r') as fin:
        for line in fin:
            line = json.loads(line)
            offer_id = line.pop('offer_id')
            caller_id = line.pop('caller_id')
            dataset[offer_id].users[caller_id].proba = line['proba']

    return dataset


@pytest.fixture(scope="module")
def calculated_dataset(sample_dataset) -> Dict[str, OfferObject]:
    res = {}
    for offer_id, offer in tqdm(sample_dataset.items()):
        features = calculate_features(offer.calls)
        users = {f.user_phone: OfferUserObject(features=f.features) for f in features}
        calculated_offer = OfferObject(calls=offer.calls,
                                       users=users)
        res[offer_id] = calculated_offer
    return res


@pytest.fixture(scope="module")
def model():
    model = load_model(local_path="pytest_vin_tf.cbm")
    return model


def _test_offer(true_offer: OfferObject, calculated_offer: OfferObject, feature_name: str):
    true_features = true_offer.users
    true_features = {user_id: getattr(user.features, feature_name)
                     for user_id, user in true_features.items()}
    true_features = dict(sorted(true_features.items()))

    calculated_features = calculated_offer.users
    calculated_features = {user_id: getattr(user.features, feature_name)
                           for user_id, user in calculated_features.items()}
    calculated_features = dict(sorted(calculated_features.items()))
    for user_id in calculated_features:
        gt_value = true_features[user_id]
        calc_value = calculated_features[user_id]
        assert np.isclose(calc_value, gt_value, 1e-4)


def _test_feature(sample_dataset, calculated_dataset, feature_name):
    for offer_id, offer in tqdm(sample_dataset.items()):
        calculated_offer = calculated_dataset[offer_id]
        try:
            _test_offer(offer, calculated_offer, feature_name=feature_name)
        except Exception:
            print(offer_id)
            raise


def test_structure(sample_dataset, calculated_dataset):
    for offer_id in tqdm(sample_dataset):
        offer = sample_dataset[offer_id]
        calculated_offer = calculated_dataset[offer_id]
        assert offer.calls
        for uid in offer.users:
            user_features = offer.users[uid]
            calc_features = calculated_offer.users[uid]
            assert set(user_features.features.__dict__) == set(calc_features.features.__dict__)


def test_feature_user_last_call_time_rel(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_last_call_time_rel')


def test_feature_user_succ_calls_after_last(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_succ_calls_after_last')


def test_feature_offer_calls_interval(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'offer_calls_interval')


def test_feature_user_n_calls_after(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_n_calls_after')


def test_feature_user_succ_calls_count(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_succ_calls_count')


def test_feature_user_calls_count(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_calls_count')


def test_feature_user_calls_duration(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_calls_duration')


def test_feature_user_first_call_time_rel(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_first_call_time_rel')


def test_feature_user_calls_duration_rel(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_calls_duration_rel')


def test_feature_user_last_call_no_rel(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_last_call_no_rel')


def test_feature_user_longest_call(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_longest_call')


def test_feature_user_succ_calls_after_first(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_succ_calls_after_first')


def test_feature_user_last_call_time_amp(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_last_call_time_amp')


def test_feature_offer_calls_duration(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'offer_calls_duration')


def test_feature_user_calls_count_rel(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_calls_count_rel')


def test_feature_user_unsucc_calls_count_rel(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_unsucc_calls_count_rel')


def test_feature_user_n_calls_before(sample_dataset, calculated_dataset):
    _test_feature(sample_dataset, calculated_dataset, 'user_n_calls_before')


def test_model_prediction(sample_dataset, calculated_dataset, model):
    for offer_id in tqdm(sample_dataset):
        offer = sample_dataset[offer_id]
        calculated_offer = calculated_dataset[offer_id]
        gt_user_features = []
        calc_user_features = []
        gt_probas = {}
        for uid in offer.users:
            # last call is fake, but is is not used anyway
            user = offer.users[uid]
            gt_user_features.append(UserFeatures(features=user.features,
                                                 user_phone=uid,
                                                 last_call=offer.calls[-1]))
            gt_probas[uid] = user.proba

            calc_user = calculated_offer.users[uid]
            calc_user_features.append(UserFeatures(features=calc_user.features,
                                                   user_phone=uid,
                                                   last_call=offer.calls[-1]))

        assert predict(gt_user_features, model) == predict(calc_user_features, model)
        calc_probas = {pred.user_phone: pred.buyer_probability
                       for pred in predict(calc_user_features, model)}
        for uid in gt_probas:
            assert np.isclose(calc_probas[uid], gt_probas[uid])
