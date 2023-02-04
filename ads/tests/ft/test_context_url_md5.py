import pytest

from robot.rthub.yql.protos.queries_pb2 import TAdnetPageItem
from ads.bsyeti.caesar.libs.profiles.proto.context_url_md5_pb2 import TContextUrlMd5ProfileProto


from ads.bsyeti.caesar.tests.ft.common.event import make_event
from ads.bsyeti.caesar.tests.ft.common import select_profiles


def write_event(queue_writer, profile_id, url, html, timestamp):
    body = TAdnetPageItem()
    body.Url = url
    body.Html = html
    # instead of md5(body.Url)
    event = make_event(profile_id, timestamp, body)
    queue_writer.write(event)


@pytest.mark.table("ContextUrlMd5Dict")
def test_profiles(yt_cluster, caesar, tables, queue, get_timestamp):
    with queue.writer() as queue_writer:
        for profile_id in range(5):
            url = "url.ru"
            html = """<!DOCTYPE html>
            <html>
            </html>
            """.encode()
            write_event(queue_writer, profile_id, url, html, get_timestamp(60))

    profiles = select_profiles(yt_cluster, tables, "ContextUrlMd5Dict", TContextUrlMd5ProfileProto)
    assert not profiles

    expected = {}
    current_timestamp = get_timestamp(60)
    with queue.writer() as queue_writer:
        for profile_id in range(5):
            url = "alko{}.ru".format(profile_id)
            html = """<!DOCTYPE html>
            <html>
                <title> vodka beer </title>
            </html>
            """.encode()
            write_event(queue_writer, profile_id, url, html, current_timestamp)
            expected[profile_id] = url

    profiles = select_profiles(yt_cluster, tables, "ContextUrlMd5Dict", TContextUrlMd5ProfileProto)
    assert len(expected) == len(profiles)
    for profile in profiles:

        assert expected[profile.UrlMd5] == profile.NormUrl
        assert "1" == profile.FastContentData.VersionedContentData[0].Version
        # alco category
        assert [32768] == profile.FastContentData.VersionedContentData[0].Value.BrandSafetyCategories.Data

    # event for the same url
    with queue.writer() as queue_writer:
        for profile_id in range(5):
            url = "https://mail.ru/"
            html = """
            <!DOCTYPE html>
            <html>
                <head>
                    <title>Онлайн без регистрации</title>
                </head>
                <body>
                    <h1>Играем в игры</h1>
                </body>
            </html>
            """.encode()
            write_event(queue_writer, profile_id, url, html, current_timestamp + 100)

    profiles = select_profiles(yt_cluster, tables, "ContextUrlMd5Dict", TContextUrlMd5ProfileProto)
    assert 5 == len(profiles)
    for profile in profiles:
        assert "1" == profile.FastContentData.VersionedContentData[0].Version
        # games category
        assert [4096] == profile.FastContentData.VersionedContentData[0].Value.BrandSafetyCategories.Data

    with queue.writer() as queue_writer:
        for profile_id in range(5):
            url = "url.ru"
            html = """
            <!DOCTYPE html>
                <html>
                </html>
            """.encode()
            write_event(queue_writer, profile_id, url, html, current_timestamp + 150)

    profiles = select_profiles(yt_cluster, tables, "ContextUrlMd5Dict", TContextUrlMd5ProfileProto)
    for profile in profiles:
        assert not str(profile.FastContentData)
