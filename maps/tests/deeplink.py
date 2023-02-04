from collections import namedtuple
import base64
import urllib.parse as urlparse


PARAMS = {
    'version': 1,
    'route_id': 2,
    'courier_id': 3,
    'tracking_begin': 100,
    'tracking_end': 200,
}


Location = namedtuple('Location', ['lat', 'lon'])


def encode_decode(encoder, decoder, params):
    yacourier = encoder._encode_yacourier(params)
    decoded = decoder.verify(yacourier)
    assert {key: str(value) for key, value in params.items()} == decoded


def test_deeplink_1(encoder):
    locations = [
        Location(10.1, 10.2)
    ]
    return encoder.gen_deeplink({**PARAMS, 'key': 'value'}, locations)


def test_deeplink_2(encoder):
    locations = [
        Location(10.1, 10.2),
        Location(11.11, 22.22)
    ]
    return encoder.gen_deeplink({**PARAMS, 'param': 'value'}, locations)


def test_deeplink_no_locations(encoder):
    return encoder.gen_deeplink(PARAMS, [])


def test_deeplink_parse(encoder):
    locations = [
        Location(10.1, 10.2)
    ]
    deeplink = urlparse.urlparse(encoder.gen_deeplink(PARAMS, locations))
    assert 'yandexnavi' == deeplink.scheme
    assert 'build_route_on_map' == deeplink.netloc
    assert '' == deeplink.path
    params = {key: value[0] for key, value in urlparse.parse_qs(deeplink.query).items()}
    del params['signature']
    del params['yacourier']
    return params


def test_yacourier_1(encoder, decoder):
    encode_decode(encoder, decoder, PARAMS)


def test_yacourier_2(encoder, decoder):
    encode_decode(encoder, decoder, {'a-b': '=', 'c+d': '-'})


def test_yacourier_3(encoder, decoder):
    encode_decode(encoder, decoder, {**PARAMS, 'version': '100500'})


def test_yacourier_empty(encoder, decoder):
    encode_decode(encoder, decoder, {})


def test_yacourier_bad_key(bad_encoder, decoder):
    yacourier = bad_encoder._encode_yacourier(PARAMS)
    return decoder.verify(yacourier)


def test_yacourier_trash(decoder):
    return decoder.verify('not-base64-but-some-trash')


def test_yacourier_no_signature(decoder):
    return decoder.verify(urlparse.quote(base64.urlsafe_b64encode(b'param=12')))


def test_yacourier_empty_signature(decoder):
    return decoder.verify(urlparse.quote(base64.urlsafe_b64encode(b'some=8&signature=')))


def test_yacourier_signature_not_last(encoder, decoder):
    yacourier = encoder._encode_yacourier(PARAMS)
    decoded = decoder.verify(yacourier)
    assert 'error' not in decoded
    bad_yacourier = urlparse.quote(base64.urlsafe_b64encode(
        base64.urlsafe_b64decode(urlparse.unquote(yacourier)) + b'&added=value'
    ))
    return decoder.verify(bad_yacourier)
