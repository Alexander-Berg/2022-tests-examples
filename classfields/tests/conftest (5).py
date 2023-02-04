from pathlib import Path

import pytest


@pytest.fixture(scope="module")
def resources():
    resources_path = Path(__file__).resolve().parent / "resources"
    with open(resources_path / "feed.xsd") as reader:
        feed_parser = reader.read()
    with open(resources_path / "parsing.json") as reader:
        parse_result = reader.read()
    yield feed_parser, parse_result


@pytest.fixture(scope="class")
def resources_class(request, resources):
    feed_parser, parse_result = resources
    request.cls.feed_parser = feed_parser
    request.cls.parse_result = parse_result
