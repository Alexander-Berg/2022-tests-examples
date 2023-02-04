import json
from pathlib import Path

import pytest


@pytest.fixture(scope="module")
def resources():
    resources_path = Path(__file__).resolve().parent / "resources"
    with open(resources_path / "haraba.json") as reader:
        haraba_json = json.load(reader)
    with open(resources_path / "haraba_changes.json") as reader:
        haraba_changes_json = json.load(reader)
    yield haraba_json, haraba_changes_json


@pytest.fixture(scope="class")
def resources_class(request, resources):
    haraba_json, haraba_changes_json = resources
    request.cls.haraba_json = haraba_json
    request.cls.haraba_changes_json = haraba_changes_json
