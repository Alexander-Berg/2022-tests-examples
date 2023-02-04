import pytest


@pytest.mark.parametrize(
    ("url", "signed_url"),
    [
        (
            "yandexnavi://action",
            (
                "yandexnavi://action?client=261&"
                "signature=ObA3jzgj%2Bip9Egjbi%2B98uX4lzFqCBtDzmgSi%2FCktvz"
                "%2Bf5F79SEP%2FZupT4xCCVHHSChJf%2BmzanPLPc2wwwj9EXw%3D%3D"
            ),
        ),
        (
            "yandexnavi://other.action",
            (
                "yandexnavi://other.action?client=261&"
                "signature=WHswbjfSoLDGhTOOkKTJTPxgJykV%2B2HyC6VNRPCaUdy"
                "fbJ6Azm4nxtp3bUtgRVf1pOgYOFNwfy%2F69hmkzQYCaw%3D%3D"
            ),
        ),
        (
            "yandexnavi://action?a=b",
            (
                "yandexnavi://action?a=b&client=261&"
                "signature=S%2Bgr67QeTuOJV7V68E7abTerTyHWRsnszu%2B0tXNt"
                "XkrjP4kMD6ToY%2B9B2uoxsyyOPbdM%2Bjpxp2xY1YmIp9wglg%3D%3D"
            ),
        ),
        (
            "yandexnavi://action?a=b&c=d",
            (
                "yandexnavi://action?a=b&c=d&client=261&"
                "signature=dxb9Pr6jYlTf3I3EIx0HR8m4dyfVwUZPjgWUGOqf14A"
                "GnCBNoChh0ctqflywUPnFct81G63ozaJXcu198Al9fA%3D%3D"
            ),
        ),
    ],
)
def test_sign_url_modifies_url(navi_uri_signer, url, signed_url):
    result = navi_uri_signer.sign_url(url)

    assert result == signed_url
