from pprint import pprint

from wiki.api_v2.public.upload_sessions.views import CONTENT_TYPE_OCTET_STREAM
from wiki.uploads.consts import UploadSessionTargetType

from intranet.wiki.tools.wikiclient import EnvType, Flavor, get_contour
from intranet.wiki.tests.wiki_tests.common.e2e.decorators import e2e


@e2e
def test_upload_6mb_file():
    s = get_contour(EnvType.STAND, Flavor.INTRANET, stand_user='neofelis1')
    s.wiki_api.oauth().use_api_v2_public()

    demo_data = b'1234567890abc' * 550000

    code, response = s.wiki_api.api_call(
        'post',
        'upload_sessions/abort_active_uploads',
        json={},
    )
    assert code == 200

    code, response = s.wiki_api.api_call(
        'post',
        'upload_sessions',
        json={
            'target': UploadSessionTargetType.ATTACHMENT.value,
            'file_name': 'amogus.bin',
            'file_size': len(demo_data),
        },
    )

    pprint(response)

    assert code == 200
    session_id = response['session_id']

    ptr = 0
    part_number = 1
    chunk_size = 5 * 1024 * 1024

    while ptr < len(demo_data):
        code, response = s.wiki_api.api_call(
            'put',
            f'upload_sessions/{session_id}/upload_part?part_number={part_number}',
            data=demo_data[ptr : ptr + chunk_size],
            headers={'Content-Type': CONTENT_TYPE_OCTET_STREAM},
        )

        assert code == 200

        ptr += chunk_size
        part_number += 1

    code, response = s.wiki_api.api_call(
        'post',
        f'upload_sessions/{session_id}/finish',
        json={},
    )

    print(response)
    assert code == 200
