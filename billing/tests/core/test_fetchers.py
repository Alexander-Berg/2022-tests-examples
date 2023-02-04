import os

import pytest

from refs.core.fetchers import FetcherBase, HttpFileFetcher, FetcherFileResult, FetcherResult, FetcherException
from requests import RequestException


def test_fetcher_basics():
    Fetcher = type('Fetcher', (FetcherBase,), {})
    with pytest.raises(NotImplementedError):
        # run_ not implemented
        Fetcher(None).run()


def test_http_file_fetcher(mocker, response_mock):

    url = 'http://localhost/test'
    filename = 'file.txt'

    def mock_fetcher_result(filename):
        filepath = f'/tmp/{filename}'

        try:
            os.unlink(filepath)

        except OSError:
            pass

        synchronizer.get_media_path.return_value = filepath
        synchronizer.get_latest_log_record.return_value = mocker.Mock()
        synchronizer.get_latest_log_record.return_value.fetcher_result = filename


    def add_head_request(mock, filename, filesize=0):
        mock.add(
            'HEAD', url,
            adding_headers={
                'content-disposition': f'attachment; filename={filename}',
                'content-length': f'{filesize}'
            }
        )

    synchronizer = mocker.Mock()
    mock_fetcher_result(filename)

    Fetcher = type('Fetcher', (HttpFileFetcher,), {'url': url})

    fetcher = Fetcher(synchronizer)

    synchronizer.bootstrap = False
    assert not fetcher.bootstrap

    synchronizer.bootstrap = True
    assert fetcher.bootstrap

    with response_mock(f'HEAD {url} -> 404:'):
        with pytest.raises(Exception):
            fetcher.run()  # Исключение по статусу.

    with response_mock(f'HEAD {url} -> 200:'):

        with pytest.raises(FetcherException):
            fetcher.run()  # Нет имени файла.

    with response_mock('') as http_mock:
        add_head_request(http_mock, filename, 0)

        # Файл не изменился.
        result = fetcher.run()

    assert not result.has_changed
    assert result.get_repr() == '{"file.txt": ""}'

    with response_mock('') as http_mock:
        mock_fetcher_result('file_new.txt')
        add_head_request(http_mock, filename, 0)
        http_mock.add('GET', url, body='contentbody', stream=True)

        # Файл изменился.
        result = fetcher.run()

    assert result.has_changed
    assert result.get_repr() == '{"file.txt": ""}'

    contents_dict = {}
    for name, contents in result.iter_items():
        contents_dict[name] = contents.read()

    assert len(contents_dict) == 1
    assert b'contentbody' in contents_dict[filename]

    with response_mock('') as http_mock:
        mock_fetcher_result('file_new.txt')
        add_head_request(http_mock, filename, 10)
        http_mock.add('GET', url, body='contentbody', stream=True)

        def dump_contents_to_file(response, filepath, append):
            raise RequestException

        fetcher.dump_contents_to_file = dump_contents_to_file
        fetcher.run()

    assert synchronizer.log.mock_calls[-1][1][0] == 'Connection reset by peer.'


def test_fetcher_result():
    result = FetcherResult(stringio_items=True)
    result.add_item('', 'data')

    data = []
    for name, contents in result.iter_items():
        data.append(contents.read())

    assert len(data) == 1
    assert data[0] == 'data'


def test_fetcher_file_result(extract_fixture):

    def read(filename):
        filepath = extract_fixture(filename)

        result = FetcherFileResult(repr={filename: ''})
        result.add_item(filename, filepath)

        contents_dict = {}
        for name, contents in result.iter_items():
            contents_dict[name] = contents.read()

        return contents_dict

    contents_dict = read('zipped.zip')
    assert len(contents_dict) == 2
    assert contents_dict['some1.txt'] == b'some11\nsome111\n'
    assert contents_dict['some2.txt'] == b'some22\nsome222\n'

    contents_dict = read('bare.txt')
    assert len(contents_dict) == 1
    assert contents_dict['bare.txt'] == b'bare1\nbare2'

