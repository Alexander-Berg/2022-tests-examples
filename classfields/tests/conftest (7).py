import pathlib
from typing import Dict, Generator

import pytest


@pytest.fixture()
def pages() -> Generator[Dict[str, str], None, None]:
    pages = {}
    for page_file in pathlib.Path("tests/pages").glob("*/*.html"):
        with open(page_file, "rb") as reader:
            key = f"{page_file.parts[-2]}/{page_file.parts[-1]}"
            pages[key] = reader.read()
    yield pages


@pytest.fixture()
def pages_class(request, pages: Dict[str, str]) -> None:
    request.cls.pages = pages
