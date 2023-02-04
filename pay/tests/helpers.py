from io import BytesIO
from urllib.parse import unquote_plus

from aiohttp import FormData
from aiohttp.abc import AbstractStreamWriter
from multidict import CIMultiDict


async def parse_form_data_naive(form_data: FormData):
    """Простой парсер form data

    Апишки многих psp - старые, и принимают/отдают старый добрый application/x-www-form-data.
    Хочется делать ассерты напротив таких данных.
    Делать ассерт напротив сырого form data - больно, порядок полей не определен.
    Сравнивать объекты FormData между собой - масло-масляное. Надо хотя бы сдампить и потом распарсить, а если
    сравнивать аргумент body с FormData, то такого преобразования не происходит.
    Поэтому давайте напишем парсер который по честному дампит и потом парсит.
    Сейчас парсер наивный. Можно будет усложнить, если начнёт стрелять.
    """

    class BufferWriter(AbstractStreamWriter):
        def __init__(self):
            self._buffer = BytesIO()

        async def write(self, chunk: bytes) -> None:
            """Write chunk into stream."""
            self._buffer.write(chunk)

        async def write_eof(self, chunk: bytes = b"") -> None:
            self._buffer.write(chunk)

        async def drain(self) -> None:
            pass

        def enable_compression(self, encoding: str = "deflate") -> None:
            pass

        def enable_chunking(self) -> None:
            pass

        async def write_headers(self, status_line: str, headers: "CIMultiDict[str]") -> None:
            pass

        def get_value(self) -> bytes:
            return self._buffer.getvalue()

    writer = BufferWriter()
    await form_data().write(writer)
    data = writer.get_value().decode('utf-8')
    return dict(map(unquote_plus, item.split('=', 1)) for item in data.split('&'))
