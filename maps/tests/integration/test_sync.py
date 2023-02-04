from maps.analytics.tools.lama.lib.commands.sync import SyncCommand
from utils import build_input_lama_file


def test_sync(tmp_path, request):
    input_filename = f'{tmp_path}/{build_input_lama_file(request.node.name)}'
    SyncCommand().exec(input_filename, {'reactor': False}, False)
