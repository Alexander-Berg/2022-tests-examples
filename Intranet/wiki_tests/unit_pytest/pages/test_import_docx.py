import mock
from docx import Document

from wiki.pages.logic.import_file.doc import _process_file, to_wiki_text
from intranet.wiki.tests.wiki_tests.common.data_helper import read_test_asset_as_stream


def test_pr_bug():
    input_stream = read_test_asset_as_stream('bomb0.docx')
    doc = Document(input_stream)
    _process_file(doc)


def test_docx_images():
    with mock.patch('wiki.pages.logic.import_file.doc.files_logic') as patched_files:
        input_stream = read_test_asset_as_stream('doc_with_picture.docx')
        file = mock.MagicMock()
        file.name = 'img.jpg'
        patched_files.upload_file_data_to_storage.return_value = 1
        patched_files.add_file_to_page.return_value = file
        text = to_wiki_text(input_stream, mock.MagicMock(), mock.MagicMock())
        assert (
            text.strip()
            == """Мистер кот

== <<Imported images>>
file:img.jpg"""
        )
