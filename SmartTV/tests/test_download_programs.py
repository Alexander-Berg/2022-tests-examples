import io
import zipfile
import json
from mock import patch, Mock

from django.core.management import call_command

from smarttv.droideka.proxy.management.commands.download_programs import Command as DownloadProgramsCommand
from smarttv.droideka.tests import mock as mock_data


MOCK_CHANNELS_REGIONS_RESPONSE = {'set': [{'content_id': '123', 'has_schedule': 1}]}


class TestDownloadPrograms:

    @patch('smarttv.droideka.proxy.api.vh.client.channels_regions', Mock(return_value=MOCK_CHANNELS_REGIONS_RESPONSE))
    @patch('smarttv.droideka.proxy.api.vh.client.episodes', Mock(return_value=mock_data.ProgramsRawTestData.raw_input))
    @patch('smarttv.droideka.utils.caching_date.split_time_interval', Mock(return_value=[(1, 2)]))
    def test_all_data_presented(self):
        custom_stdout = io.StringIO()
        call_command('download_programs', stdout=custom_stdout)
        programs_zip_file = custom_stdout.getvalue().strip()
        programs_zip_file = zipfile.ZipFile(programs_zip_file)
        with programs_zip_file.open(DownloadProgramsCommand.TEMP_JSON_FILE_NAME) as f:
            contents = json.loads(f.read().decode())
            assert mock_data.ProgramsRawTestData.serializer_output == contents
