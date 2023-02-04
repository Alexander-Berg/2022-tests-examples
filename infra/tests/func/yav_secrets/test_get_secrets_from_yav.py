from __future__ import unicode_literals

import base64
import subprocess
from ConfigParser import SafeConfigParser

import gevent
import json
import pytest
from yp_proto.yp.client.hq.proto import hq_pb2, types_pb2
from flask import Flask, request as flask_request
from instancectl import common
from sepelib.flask.server import WebServer
from utils import must_start_instancectl, must_stop_instancectl


def get_env():
    return {
        "BSCONFIG_INAME": "sas1-1956.search.yandex.net:17319",
        "BSCONFIG_IHOST": "sas1-1956.search.yandex.net",
        "BSCONFIG_IPORT": "17319",
        "BSCONFIG_SHARDDIR": "rlsfacts-000-1393251816",
        "BSCONFIG_SHARDNAME": "rlsfacts-000-1393251816",
        "BSCONFIG_ITAGS": "newstyle_upload a_dc_sas a_geo_sas parallel_autofacts a_ctype_isstest enable_hq_poll",
        "tags": "newstyle_upload a_dc_sas a_geo_sas parallel_autofacts a_ctype_isstest enable_hq_poll",
        "annotated_ports": "{\"main\": 8080, \"extra\": 8081}",
        "NANNY_SERVICE_ID": "parallel_rlsfacts_iss_test"
    }


FILE_FIELD = 'file-field'
FILE_VALUE = 'secret-value-file'
ENV_FIELD = 'fake-secret-field'
ENV_VALUE = 'secret-value-env'
ONLY_ONE_KEY_VALUE = 'one-key-secret-value'

NEW_SECRET = [
    {
        "value": [
            {
                "key": FILE_FIELD,
                "value": FILE_VALUE,
            },
            {
                "key": ENV_FIELD,
                "value": ENV_VALUE,
            },
        ],
        "status": 'ok',
    }
]

ONLY_ONE_KEY_SECRET = [
    {
        "value": [
            {
                "key": ENV_FIELD,
                "value": base64.b64encode(ONLY_ONE_KEY_VALUE),
                "encoding": "base64",
            },
        ],
        "status": 'ok',
    }
]


@pytest.fixture
def hq_and_yav(request, ctl):
    delegation_token_1 = 'fake-token-1'
    delegation_token_2 = 'fake-token-2'
    delegation_token_3 = 'fake-token-3'
    secret_id = 'fake-secret-id'
    secret_ver_1 = 'fake-ver-1'
    secret_ver_2 = 'fake-ver-2'
    only_one_key_ver = 'only-one-key-ver'
    service_id = 'parallel_rlsfacts_iss_test'

    with ctl.dirpath('dump.json').open() as fd:
        dump_json = json.load(fd)

    service, conf = dump_json['configurationId'].rsplit('#')

    app = Flask('instancectl-test-fake-its')
    app.processed_requests = []

    @app.route('/rpc/instances/GetInstanceRev/', methods=['POST'])
    def get_instance():
        try:
            req = hq_pb2.GetInstanceRevRequest()
            req.ParseFromString(flask_request.data)
            assert req.rev == conf
            resp = hq_pb2.GetInstanceRevResponse()
            resp.revision.id = conf
            c = resp.revision.container.add()
            c.name = 'test_yav'

            # Secret
            e = c.env.add()
            e.name = 'FAKE_SECRET'
            e.value_from.type = types_pb2.EnvVarSource.VAULT_SECRET_ENV
            e.value_from.vault_secret_env.vault_secret.secret_id = secret_id
            e.value_from.vault_secret_env.vault_secret.secret_ver = secret_ver_1
            e.value_from.vault_secret_env.vault_secret.delegation_token = delegation_token_1
            e.value_from.vault_secret_env.field = ENV_FIELD

            # Secret containing only one key-value pair should be retrieved even if no field given
            e = c.env.add()
            e.name = 'ONLY_ONE_KEY_SECRET'
            e.value_from.type = types_pb2.EnvVarSource.VAULT_SECRET_ENV
            e.value_from.vault_secret_env.vault_secret.secret_id = secret_id
            e.value_from.vault_secret_env.vault_secret.secret_ver = only_one_key_ver
            e.value_from.vault_secret_env.vault_secret.delegation_token = delegation_token_2

            # Secret volume
            v = resp.revision.volume.add()
            v.type = types_pb2.Volume.VAULT_SECRET
            v.name = 'mount_path'
            v.vault_secret_volume.vault_secret.delegation_token = delegation_token_3
            v.vault_secret_volume.vault_secret.secret_id = secret_id
            v.vault_secret_volume.vault_secret.secret_ver = secret_ver_2
            return resp.SerializeToString(), 200
        except Exception:
            import traceback
            traceback.print_exc()

    @app.route('/2/keys', methods=['GET'])
    def tvm_keys():
        try:
            return '1:CpoBCpUBCMczEAAahwEwgYQCgYEArciGfyJUbLpWeFROaQdzoV0NOiUMt8WNQfljqDQgKuIklGBwZrN1PVhzUrfERpuFje_4_8ziTiVGRI6ywsgeOPWzFp2XnthdMT6r48es_TIwqUJrPdCv0X5Ds8yrubWVf61A-txQ0PGoL4keC0cuxSHQmmIbElygHP4oDzhpWtUgndur4AUQAAqZAQqUAQjTMxAAGoYBMIGDAoGAR78NUcviET77w8ubfwluLVuULBv65dF6o-zp8rvlvxltpLHYbmpTc6gSWYENvxj3rQRwZQNHxArUQD00vz5CD_yS9_BzNrYE9ksYa03RkqDkXBsdA-OGuYgPbCLICwTjCO13TlyQ58xIGNLckHNdGuv6JBHm3ZMpAiJjtmSjAuUgnf6w4AUQAAqaAQqVAQjfMxAAGocBMIGEAoGBANSxMs_xcY7za9V1h99M2Q3UiKQPvWs0hWIcRd0-lSMcbBmFGGD0dDtIYAiP3EoCTjjZLTj_HlPnVZBCPRneRnMUtej-jqfkrd0lMdeVfi-0fYeG19FP7wU1Bp9LvMo8I07nOhgBsr3Bze3cesxpqSjb0C-w9c281qAKcqom3JP9IJ2htuAFEAAKmgEKlQEI6zMQABqHATCBhAKBgQChl8IEjAZn6_kIH54V18pwLvfxx9KHiDI-XE9mg5oKgPaHeHgiOUUHzf6efI9N093Brng05Z2iiHYmUAsGj03T1brhBYsiaN_oFmrlW82q1aQXfFTYJCU3momSX2zfs4l_rqU2wnO_Qo3omnEVJqphU2PXDWLGmfo0kyxk88f6tSCdxLvgBRAACpoBCpUBCPszEAAahwEwgYQCgYEAkvMQxlKAGe5sMLhHhqx7Xzf-6ILt-oaXMjOMgKuHplBR5_VnlniD3lux69y-EQ9kGOzGirb4hMPJRoXU5o5guNM2jFpeumh56T9iXKNBC0dftzB2JW1PE4rBnz87wKsTCtro0RncC8q6zNJiCdTdSwtRxGZ-DcitMAsW6rb-WI0gnufA4AUQAAqaAQqVAQiHNBAAGocBMIGEAoGBAIGnyJEtOKwLx3Oh2HrcTF-wXMfG4RVqLqEgZlUTiWnTEvYahyxHIP1hp3BHl4dAWbjMAtK4x1apgGyKnw14TeeQs3AA7-5qqSVBca-BiY_Dj2hv4wmXPAdeW6lNc5xLyJoKZtZhonXLED18gTmBA2IPGzP2726iF2Lscw3JWrxFIJ2KxuAFEAAKmQEKlAEIkzQQABqGATCBgwKBgH_AsO3ogNf-gRteu7Bry-dmY9ER3wZKFDKnqZR46a0USuABJoOYfSMp1anpWJnIRoiQ8MEmptH8eLoEZN7Xx1gQySpVT8pKPENpHXPFwOk57s6x22gJMNkKXEjYDs1-w02ExSs0F5EmF_GVQyzLi5ScgwtQgeXfPeefBZ5cJzAFIJ2ty-AFEAAKmgEKlQEInzQQABqHATCBhAKBgQCajhU38puBOSp7oflWetQfP8hnLyLFZR5GGTi5h2hRjVX126_jnr75Vd3Gt-h3JWlYYIac7cVrgQ85Avlx5Q6a17vozIdTOP2-qqwJK2y18e7EfQn3eL7CiWFLfNmW4V41LT5suvvb3Krlcq6j5Bz2Ts2bvxSO9-WyOQtItUVyjSCd0NDgBRAACpoBCpUBCKs0EAAahwEwgYQCgYEAoGqOqbk6SFpC3LTF7AXOU3wtwDC80lpMdbuSuNE31DUDrrlSWLzKZI9eQEyw3mu6T76mNL5QNrNV_UzgIhu5IdyXInSmsn-PZhHciM8wbG_T24JBR8YTLMs_65aUbjUf9kMmaiTEFDM5xDmte9Y8c1zzXrcoBypJDXXP6frCRJUgnfPV4AUQAAqaAQqVAQi3NBAAGocBMIGEAoGBAIgVKrP9uVqj3R18khJEhmeDG7IOQqb8ln7u0FvRd-We5-DwTRZ_CXEbOwGSGlA51JI0S-XboHEnex3CdWHDqN0cmjzgox-k3aRT_GFDBePnoJgxHhd49BiXHBRmxhJG5klcDymMJOdbF4jl4Y940zUEwLYnzjGIvbzaoc3OSHLlIJ2W2-AFEAAKmQEKlAEIwzQQABqGATCBgwKBgHNYdZaOk6bBUBcukP7MRXjftSPcw54-DXqvlFYjFjX2Pvn2locztdy2rSh38wwQ_K7EMqSrjYO4EbI6Q26iWA67jRoNqlj_efRsRZ62VlLGV7kNNpoXBpKrZhQolkhGizd2fXSGIlVkLjruqi4EZJYqW90XDV55_qDGS--M5JrtIJ654OAFEAAKmgEKlQEIzzQQABqHATCBhAKBgQCOtbeVSpmVGFbdXlgswKSa7QGRz0EPu7Hukc4nQO7JZl5wWkY7Ri74H-YqbOXXynLanDFtThzUEQ2ls0flNNa_jHBgH3dmyz-KQR0n4l3CGA1tCsYuuFutdSDkMPJ5K8S3B36EzqdaYpODP_2kPs9dzZtpanNRilUKY59inI0UPSCd3OXgBRAACpoBCpUBCNs0EAAahwEwgYQCgYEAqladDB4V6xLbrGfUA1sxznS-8gVeUIzi12YORLKcQuFacMcEdqIfy-LZ-G1-ZcFvpVRn5y8OLpYBMjG_3lJpLJEmCUO2fMo7fNQzDX_uvgTN3vFu8t1OdrLHgDc_Wn0QDvPaF9p0tKpUdVIC5UMmZiDukiuhe-iPs5Z-3ZuLmCUgnf_q4AUQAAqaAQqVAQjnNBAAGocBMIGEAoGBANftsGP07ZDQ_NLUPNp_mgEdl-kTMEPxVUj5FeEiYxNiiBI6Khd3-V4pu2wUVfN2QkXkPwcuUZDvb4Uh6UaJjhCaSqyK7EZHR1vglHib91L-_sTSjQ3VMX4GVxUZ0chynozoY06pBIEmFfBFIctlcvEHlgj9qnWeY1Bfyb9dIY7tIJ2i8OAFEAAKmQEKlAEIyTMQABqGATCBgwKBgHfndWmFq8eAks1HSx0WScC61ZXUxvHuUVa60MFY40EwxPgG6z6YnQCJ0lVj3iTz7sxXiLb3BLrfq4ycCUPsSmd0PVPPIhfngi0-nL34IKvLEY3ggEV4SRZ3p7_B5H0AgejoUcCbr3MWAgS7lCDwfsb1_uV1zbrRim2PIJsgS7oFIJ7bq-AFEAIKmgEKlQEI1TMQABqHATCBhAKBgQCn2eupx7v8Py2S3suEf8zd8TfgYd6wl1zvlHWicBVHYJyY2z8ezT-n5_w8DHw-KzzSbSBxM80FlgC0ulL4FpKRtjVuP4Q64kiLhHEzMSJg28kYy4emc78wHZxStBjSi1goeDXY0Io_hQ0RTDj0ygMkx6nlPg0ujnQe5IUY29eP7SCd_rDgBRACCpkBCpQBCOEzEAAahgEwgYMCgYBL-YebJByrpYRE8bC3_kQ6fEx4my_HYrM15_YGmVl0HeEZ6-gU2DsFR3Z5DvNEtfUY68bKWMh3bunKK3fPjn9FYUEiiuWc157__ItZhlK7ARdbE4wtHNFmY-Rg9fuHLYTeJJsbxYJu0g7WJifm01VJf6Qok-IKr3kXURJ5OaYtlSCeobbgBRACCpoBCpUBCO0zEAAahwEwgYQCgYEAmpb9UAzL_nkOA-LswFkRUqS-c2PTvaTuszA5pP3Udkw17KWBJTyaFOYfRW97bHHUdALT2dfN4GKIkUKJ4cMBLrPJiD6hyLPZubGVByspHj90wQUQvbYdq1BD9Y-XI2h3eh0WcOZxb-NS4GF-h7LW_8LTgzt_4bJ8fPeBME1Qm20gnsS74AUQAgqaAQqVAQj9MxAAGocBMIGEAoGBAJ0LuTKh3gzBxt49WuABYFtrjMipxl5Q2C3WGds_4twNucnOypfrwB8cAHJfAUSVS-eMmqkiY3u3ABt1fCWI2IgbzloUIXSyHmtXFUM1QXpX-LGvlNVUwcn2eAJWpSLjYareUOhPdulXxQ5eHiTnLrZzNJlOQ4LFsNkeAzGnIRLdIJ7nwOAFEAIKmgEKlQEIiTQQABqHATCBhAKBgQCRCNF5JmSCHa8LhB30IapAq3pMzWb90RJ0h4fTJQf5G_S0utxIoGBAto1ZtR0J_XCUWIELevDancPc6pJgE9urVOfjh5htcvGNAZK0Ak3tmf2GE63bsotUooplXOj5CVXYdP0Q7Y6-7TS_OBR2w1_ZOjtkbf92wTr5BkP6cH8RzSCeisbgBRACCpoBCpUBCJU0EAAahwEwgYQCgYEAlxzokLDSdpwOtwM-aV-yJKngKq10CVy35XDlrgkyLXgsqs3z9YulP0YGhsqDEuBqwMCHjWmXH16nD_rhg3mmRhqbwu0Q09dPkzuBHE_sp7I3ZN8l1wWlOHh2IrG6FRTDvUyn0qP5gOno8XS_vDinOXXjQUnC5LE7OE64smU74wUgnq3L4AUQAgqZAQqUAQihNBAAGoYBMIGDAoGAU6b6ARK1uj9hzMUunx6rCnPXJTqNzkXuDGQZAAW-zv9PmT0pZQt0d_6Osgz6OnZS5gZYxIBesTsKdBMLbYgbfhaYcB7kcFYSs5r6fUF1iZo-nwcjS5kH3a_lMWqfW2_5uWwW6JhoF9kpY0H7P9gbFIoD3_LybzaVIyxL_tJ7JRUgntDQ4AUQAgqaAQqVAQitNBAAGocBMIGEAoGBAK90CvTndygFq7Rn6jy4AOZ_pVPU-hX9LWJYcWnRbOOvWE_gnJl0rbPovZV9j2UBCzgqsZ9Wk5c1dGKXry9DtYBWtPh0chRiC3DHpHvKO7mOO7eHzuHWGU07WXM6lJLL3TnmERoPCV7BlSlK3kvhUX50LPAj2FSXwk6NyPEbT3ZNIJ7z1eAFEAIKmgEKlQEIuTQQABqHATCBhAKBgQDbdHLegD0XOS9avJwSOIHg4BwB41NWSeoHiYMPf8RNWrlS1guYWKPvDUyR7JOp_lGjU1X8a5TpPjOBfs8SDZWf72-6YtAOuNFX3YxNpO3ndgTKgRsPKHk2FqKY4tob7dIg6QAWncLNXFSuA2YxU3Ow_OtIaqxB7Rn2qKemm35Y7SCeltvgBRACCpoBCpUBCMU0EAAahwEwgYQCgYEA1AUhiaTNONAc4Vm7ZI81rqNttrKyq0Gbc5N6IyR3F8KR2w8U-l_NIhg91Hoi915z4AbYCtMcso-9PQvtR7_EO5mftEhq8Y-3lxCoHtkmi95LBDrS2oz2GNlPxT7z6MY8_sOv56bW69A9CdDdAAS4hxfcj7oPWYFIXvuEwrJ6RKUgnrng4AUQAgqZAQqUAQjRNBAAGoYBMIGDAoGAaeG-yJUN0s7cSimeEUqKUXFjfJIFUInIRJHCRY1WNnggeP2t-6pdfc6sEU1ykCXxgI4SNvAP3qCF5tySrlHAQIVEDbJxVw2knH1muGHPm0V8JVq9RApp4WNgBdaoZLEJRa95ES18NR7QZIKHFHia4P9qRWPKSbZPCRrS1LME03Ugntzl4AUQAgqZAQqUAQjdNBAAGoYBMIGDAoGAfmgU896Jvx3kmbYhLXPF-FfFw_wDGBLDM01kdFP1mT0r91hr978iDz--tXhR-sjpkR4KnEE2y0Q39V-WP-eFpE-wWtGUOoK2WEO5fBuU0DLNkFar8wxGccUPgApRurR7iRiHoh4KhNjuT-wnFhV5kdAtpBlwtJTU1QwwDTi7ec0gnv_q4AUQAgqZAQqUAQjpNBAAGoYBMIGDAoGAYPxCSIOeAR_d-vwqLLc_xvwF9-8kW2WVnBT_GgSQBM5NQIqXqTBRlxBmDdnLYnI_yZlNS5EqEBt42ZKsH_cRXpSne0ICmiEre0pgwqN7L0cqkUdUK_IdkzM5UyXQqYhloM6ee-68wi8DrR55oDVUbwgeFJJC5PHm97DgfLLQsR0gnqLw4AUQAgqaAQqVAQjLMxAAGocBMIGEAoGBAJShrlyuO0B8QCtotqaANFLr_3qIs138JqVF48SSivHcxDGtaAFgIfT-upI6kQVQAM0ilkIfcHsci3yTkt0WqlPMxEg71T_B0KKD1dcYXqnnKF_BDlsjhJbpPZ-BDqmPc-f0FS628zoYGrP_6_YTB77sFCcdVambs2gLrZd_vVC9IJ7bq-AFEAEKmQEKlAEI1zMQABqGATCBgwKBgHtiQO3dzz0Nj4KUbeqovHa8zLA0bq9pJAIpNa9e5zAIHjjQvW_K3btVlLtOzAvyz-YM1wkxNbee5dOv2yTuQvA2qj-HjeUh4h_sSorN44GIOOk7FDjh5iOpF2Ar9I4D2WnpVRuUA7i8LgtHT_pYgvyi0yc4ZPt1AVx-XLrajV6NIJ7-sOAFEAEKmgEKlQEI4zMQABqHATCBhAKBgQC47AlKx0tyEO2n5ctvvBK5wOw10kyUIps92WWmvfvuXs8utMAaqVY4itb7leBIXmXWiKT7X0Zpmrq2OtZTlW0zRtkOdVpw4_caXAyiIem2Cd4nESkJFuTHVSX1NxJjO0cOblgSks_4ERWSvu1puLQZobjnteAaGuvE7hM-BOVVZSCeobbgBRABCpkBCpQBCO8zEAAahgEwgYMCgYBKJwjNpvKjxxeySpFVvcTuzAgav0Xv_vUHywXwqSYImkzB0bkxsuMM8S0PrFd7kxwvRaK5son1zp0NZG7UbS2pVl3bdY4Wt5VfnZUQW1jsnel9gMwmSAOu-bzfwpR1ySzmD2VNMreKYjOskpVCXImNmPprckcAHcli3tPJpUy9ZSCexLvgBRABCpkBCpQBCP8zEAAahgEwgYMCgYB8pZ7sKlM06Yqdydq5am53ML3A3Wba5WntjM0fS7QQZiC2JhgIaVqDS5mtdkXAG07hBQE53bGsQOfQ_uUgriWtQLE3NtL7kechxrX23mxaxWNyvpahy4cn0Cos-GxBepY0oxD3MRpyrzG7X6_4UQ7Z1CJ0DfcF9jN0KTTI-e0r9SCe58DgBRABCpoBCpUBCIs0EAAahwEwgYQCgYEAuIjZqOGQPsref6lHMmsULeyC3V4mLz1S1PtOWc39JampZ0bYnepY8dxnzN_Xl3pFJphf8OZdHo783O5jIw5K7qIy2o0kX2R-K2kx9qW5Jxv3ZjfTIb7IW7Z6Dx0HevUHIWlVhRqXjQ3mwFgu6f_68oQ5Y1vRA4FekuTaBHofFG0gnorG4AUQAQqZAQqUAQiXNBAAGoYBMIGDAoGAfc7vIwupAPnodA6Gmmg3UJtiaC83C2H8AevhyiDEmfMN8rj8RWzSEKRGk9pU8EixU9kFkDuxxc2VZdFtbs57PcVdaY0eKEW8dtkD05PguBFHmBbIffBLXh-Sqz3mKoOPzZF5m4Ybf6tqs4xx8z4C1TaQtBLu0N2aQi3craRQOtUgn63L4AUQAQqaAQqVAQijNBAAGocBMIGEAoGBAJAHknLCFi0Xgvi2DU285xvObtiEckfGdL8Rm2LEJrjQTIINMaX5UEDVlhAZ5r7KxJknoD7yUFrgzchgtL_ZDsAmTwCkuNVDKcFhCGFoPCtsBsPrRmEHL4hcbXJIcluzIeS0XXsXVC5Sx72c7dWzrGeTchFij5nKfP4K412tev11IJ7Q0OAFEAEKmQEKlAEIrzQQABqGATCBgwKBgHT0XcUJ0A_DRcqTrTkWbPTnXZyGBYfZiPDZ7mM9GnYspa8B8gHJtBNHHK2GgzhVbqNWuumtsmFeYheDXfiaSBsKkb1RIxbg7-qyj3DH1U8_66MDZNX1AYKYl_6DC8fCzqVTboig0Oin7dDAdsPn2A-ohODFwoxFsJU_qHWf3pp9IJ7z1eAFEAEKmgEKlQEIuzQQABqHATCBhAKBgQCxnJ4zFc9IY1YctVR5iy0MKwrrAhYnxQdEfOypS6td1-uNajutcwTPZf_-A-3UrD8-yJDXmly6qQWcUYAJjMrSEtOByqrTioXIfy5unxMPWN1FKkI4el3Qax_cMzP10ZNfcWCMfxNLKT3h8lxQwA7J0Oo6wB2KhwkCwvmMyTVtjSCeltvgBRABCpoBCpUBCMc0EAAahwEwgYQCgYEAm9_075bPv3bzNoeutOAiYm-ghkLthLZfD3UQ8c2KSf1TLBtMLZBQzaVLRZS1mmDW5vr-Jf57iuefQBaNTpw1UBXoW-B9ks0P83H0TeLriRigEN4hlG-cF0_LCI4Fkj1DKCqI0nLrS1dgE1jMPoG0EucDqrb6cHZCasvTgvoQ3m0gn7ng4AUQAQqZAQqUAQjTNBAAGoYBMIGDAoGAecF9kEIrZuuFOcBmDmSL1LT86htTjfpYiM_KPAt5RtnpVY7pY6yA8XPMAOOPLuWqu8KeinnB3cztDf5IaTIswi6HtTcGb8TSg0exwD44G4IRE_WM6cyTWG0IO1cYwAMMvzigCUy1sYh44WL0lZtvLYOXIHy_49F9ihBT0iGMTj0gntzl4AUQAQqaAQqVAQjfNBAAGocBMIGEAoGBALQMi_WTHLwZJ2hipmAH7XhydhuSEFyCrSD-q4V6_GsQvpAoC3SAI6whP99hNw9ACx_Z3qHq1-gd-o4_SahhLBJA9U91fa5vg57dqo2dBHZlDkTvWFKUuMA4EP3Ai62YyJhN6oFZw-6fzWP9_JuEFnnW4-OJXrHh53yPCuC3Ig_NIJ7_6uAFEAEKmQEKlAEI6zQQABqGATCBgwKBgHASMmq1d3R59__HqQ90fw1hEMy_JIOaYT6ndEd_qQnwSjfFpS18u50PHvSIsjAqebsIHW85rEjqCExGzvTAloCQN_vPVGPuIUWiqOY6ISOIyaiXIHBvmRTZUI09reSSktYaSipSBDI-g2FjY60JWGuLV8KyxdDAkCNWlmp8K6JFIJ6i8OAFEAEKmgEKlQEIzTMQABqHATCBhAKBgQC29b1rkFFCce1G91wRgGgwU2lwfH4N7asDnzuzo6o7TzikyxI0J0pvVuIMQC7LdwZ-vQywIfSXVusUU-WFxed4F4KJRMEj-L1Bh59oehjLB74fshoYAQKOHG4fSlub2DxSqJlzEpcndEOjRSMS-SWoIluc8ssZfzSiX4MMJymVDSCf26vgBRADCpkBCpQBCNkzEAAahgEwgYMCgYBmWJ6Ls_OHXJFghUXA9-dNBjxyT_uCkVHV38v55m8cgYors0JsxTbNykhPNzms2Jje5mOi2LqQLkAJsqP6gQxln3ZxpK9RhD4lAy9DFsT3e2_JH_SHUr1dXKkNUaliubndOjTGHb1sfTanuo8T260Herf-YpxDg7okdVyixIZCTSCe_rDgBRADCpkBCpQBCOUzEAAahgEwgYMCgYB4Ck2TdjkcmY0rztUW7RjVkjEo-X7RdhsBK6XimcjIR_-f4fdIN2iSeqVN38ToZ2ATlu9pPqOcjUe0UEAieN4DWK1p47VTlDqbWRRqyHc6fmPX7ksAVxvCYMLAtYcl3Q3WqwFP3XgC2GuM0Vt0UDYG-_g_DYPw-jpgGgzYaToALSCfobbgBRADCpoBCpUBCPEzEAAahwEwgYQCgYEAuWhHXxBqklrquMZ2j44RrxtctLFV9bLBMFWNzcr1YKnEZk7a9ItCzNPRxJYQNvDhJUMIacZ-2sLYedyppnP92yznjQrbATO9rn9G5T5tjNrItjGBKIknLLwE8SV_-Dx7Mp4kb4mztLJu1aNsQfK9yxTXRZ-CE7bf6LFj5itCwP0gnsS74AUQAwqaAQqVAQiBNBAAGocBMIGEAoGBAKdu6jjSpuq4x3CA39rSBYIpV0oYAjryIC4k1rho5QjDewED9XoPPSYnbNXpStASfZuMolB3bcAD62EVg7rsPyZBNoXXxDsPsx5uQ_t80Vt0mu7HCg8B4VtzkSpCKwOVInAbFcGHAvhIJIDA28d7lLDEFGyhoChYb9h5nAXs9g2VIJ7nwOAFEAMKmQEKlAEIjTQQABqGATCBgwKBgG9fcRHtMLceJsgNEO7X38oWg3Vr2waG8avFsJ5qkwgrz445NqhpkQXbC2SuWgxR60LkhjroxyOrpzlOMQWJOB3BO-Orn2cwNBZ0t2WGm34TRukNmXmt0DJ3xnG1ayggTIkGtlbCqto1627h954W7T6uBGq6vZLGzgXnJbG7L8Y1IJ-KxuAFEAMKmQEKlAEImTQQABqGATCBgwKBgFPuRxgAjotzOinuK3zL0qTfMp8ZBfV59K_tp_TGPMtd3dosl4pVURp7r0tbAiKV3nvwgfsBYNMR5tb4SPRQ0eJ4Uu0oAMJmNPiTdM5i9Js26oQW6Mg6eMW-vvtD7iO4dz-iJoT6q_lOd-C8MxF0i_-pJfj-8HGWRei84kuj2n9tIJ-ty-AFEAMKmgEKlQEIpTQQABqHATCBhAKBgQCCBPOj8eBQKNMgzCUxgGL7wykPFXHjqOpqNqka2Lo6zEXQl-DszxRLpLKcCKnp3jh5WPwkpcjNRUtkhu1gNsd4yCyS4bfwcV3cEwH3T_W3iooMZntzUdvnfKO0JyLBk9BMYBCFhEcK9WjoTLQcOu2tvQv3xevJAedjQr0x_cw9PSCe0NDgBRADCpkBCpQBCLE0EAAahgEwgYMCgYB2qt3Jb9TY38-njanu30TuI1dchezXsTvNudg8nQKehpdKno7WKObvpYTRBj3lFyeqm_9vzNyastUfZ_AOETe5Lp28FTul7XOmrtPn1ko8xhPhwv0RVkfVsJe2FYe6H43u4-FWUf4wuvkSnBUvVRnHKJmZAqA7LAALJMy8L_mWBSCe89XgBRADCpoBCpUBCL00EAAahwEwgYQCgYEAqt5xjh419aTGlhUPyqlZeS7UqOahcihGvoVBPvtJjWz4rx5MumLP00i-zirUUvnHNgzFglSkf53UoFME3kWWmWCGeRaB-W0UG0UfqB5h1DODQnEbV5GkiOgBSkINprxyuyw_qRqgbuBiLP3UTkt6YumnZkhTguHbMSWnFYFgmk0gn5bb4AUQAwqaAQqVAQjJNBAAGocBMIGEAoGBAIhpe8T2rJdvWGmd_8dXImn0K5p_6ayblVvYv1zHDisYENAW6f44a1xiDmtOxxVw0B86frtVU8dK2DitqrkHFYsbJ3IlfseswKKFa8qrFTSRGGW5sAvBuGiFr8rS7y1Qbc2qiCjNSUfK9ijkiz6J77Vk9xb_tA5woGgWJ6OpQk1lIJ-54OAFEAMKmgEKlQEI1TQQABqHATCBhAKBgQCUGw8fKmADX2EWCjCmOcKEunwBjGMe0ofNg9PLNOJbINYnY1YaBur8LH9eOp0F6AvZYcSTLrSxTeZAOGTsJrghgBNuu-OEkwhvc2wv9_IJfZ6IN8tZSl1qED5iAJM6YSV-5LzOmMbQpuKerrCovdkuhT-i353gWu28d-slwl7X5SCe3OXgBRADCpkBCpQBCOE0EAAahgEwgYMCgYBwHf7Hs7peXPlVazNZXvdFIWrmzFh960OQ_uSDELCHX0cphtDgqA5yX4GGRAmJ5HUziDY3g7qbyWV105hZz2KHF1Fe1IVFNO5zVM2PAvUK3UdN3E5enJvD5yPsYsrOw69F1hD8QeLwRUVRXcyOKHtFjnaZC84SBy6duHiDMN-svSCf_-rgBRADCpkBCpQBCO00EAAahgEwgYMCgYB1dLd66hF3vGTgoWYkOhySpN5yo9-4dcXzHKBLM7zO2qiBhAwslcE5PqLWNM3Qn22IuO6Qi6zjMiWTlXvmAJWuIx7n8P0Xr1sp3kEEaAdLsRhQzHlXtG_Swk3i-cN4B4xg7PGic28JomgC_nLXyWGqQTzwiT2lJpg4GveLw1D3LSCfovDgBRADCpoBCpUBCM8zEAAahwEwgYQCgYEAiwcwxNojVJxvIodBQoDXkEj4CUqgcHJtjNKDShpAEgDtfrIm6S1xUzkADueXdbj5peXXdU3tH_G5V5Zn6S6ufEZc0oZPPnQX0-zm1IdXPMWluWaNU4qzbwDChgtqlOAnp9TcbmUmY-v-Fl0U_W8vGNfzEvbfQvw_yK4swf0Sf3Ugn9ur4AUQBAqZAQqUAQjbMxAAGoYBMIGDAoGAWkafIDmxvTYguXgRmfbC10fkqTqx8esDuEoeR6X_GvQxVmsJz3X4W5r9pos8nRURODDg_-4wudvL7l8O0vWn5pqb5enjiyymKDpq2SkdwTp280bYZlp2eLiBh8fCpS46qVbN_eKCtT8oSfiBydLn4TGEyLOoZFfF-onpDn8wb-0gnv6w4AUQBAqaAQqVAQjnMxAAGocBMIGEAoGBALS_i3ha57B_-ru4uMkJ2RDrP1w4dsLcMcSF2n5TauN084CEqm0bmCkg4SapkhuZlEJUn5a_yQ-AROJ6MTPhpadUjQCVjcreZChxYaadc746HqtkH_Uk5ciVVTpZ0S7oLW7Pm59tTbbRuOJ7JpFT2MKw_8fMl0A97WX_IAAo8_EtIJ-htuAFEAQKmgEKlQEI8zMQABqHATCBhAKBgQCBgd9o-TKuVTL7EeS91_yxW2cBP0P34CPlyV8OSx70_qIEw7Clca9Hvqp_lxSX8i9EoxkzBmFrL3QcDIk3GXjsQW0gjwN8DIRSnQ4iRQT3QKgYuBRiakAR8AJcWuDyzCUZYzEdiBRhC2WopdqDz-uezOT-Bkbms_FWzKzeEMwnlSCexLvgBRAECpoBCpUBCIM0EAAahwEwgYQCgYEAhiAuG0JfKGqAiIonsc0totKWwP9s28dwaFnP1AUgIMImquGf5qf1-P-rcX1kyg_OlQxRSIYRoqt4_aSU9UPbEX2fJ2xUiAtu99Ld98QDYQrCAG6LHleUgiGI8UzFHaip6360U_ObaVpcIyqfY4S7onuiUIeGHt20xFezcVK0H_Ugn-fA4AUQBAqaAQqVAQiPNBAAGocBMIGEAoGBAMxPMyFBm7r6HF9yr5pxs44XUS9Lr1zrB68t5A_6Iw4tct-XFyajHSZRjMbC4wqdNFqNKm4yR6j3rthtavJqXHiCqjyXzFkYgeVDlaR6Htmqtcf61-TdMaW9UW25NNo6Su28rqHL5Pz1EWfp83sSwVgPzox-q1RHOnSOrUYfU5XlIJ-KxuAFEAQKmgEKlQEImzQQABqHATCBhAKBgQCjYCInpv4KQX1Fd7SPewrXgagYOw6Wg_uBGro3T4C4Q2CGNLpGsndUsjSOHp2m2FmoS2PdGkG5rF_h5KKkbrIAaLkac63IlY0Npv_87w7QTnfW5gSRWov9pAUr9HjjraFE-8TnOoF5yLB49Hkmweuml9roue2KJDA20mfOFBeRFSCfrcvgBRAECpoBCpUBCKc0EAAahwEwgYQCgYEAmc0lcO6O0LpVad6hqmY2LL55sK7GAX4VLXzLHP6IAv41fWHsMSpTze_slmFIuROuvQkFgg9TyG47Nsx112PTeCvGEVKaBLvP9BbAOIxQEKARDnDyUUuECuLjGOkGDOAkq5-i-9wYNXVr2s9uaGe1Trh4fKW4jc1FGCotZe-rH70gn9DQ4AUQBAqZAQqUAQizNBAAGoYBMIGDAoGAd71LJ6rbo030k1BT1LcLAq5ufz-mkbxFMaAhUvJj2Nm-DZHhUcsOK__E0zr_HRUQ1pgerf0BIfXQIbm37giYgdb1BA1CDQ3WyO4UGqhpt0IZ0HQJmkzSZCw1eQTZOjNdjUCCzeTi83PkFI3ir6YpM-K2Nob7sp7RCsmVxHF5550gnvPV4AUQBAqZAQqUAQi_NBAAGoYBMIGDAoGAcatR9JpCQfEIp2f8_3O8mISnJiL_W0-7TX19LpovIqTw1tJv6Mp4B7n8pmBEqd6GMTLgifIMP1S-p4Sx5YIKCjCZMK_xLJxpYY9xfpcXp5fWqgvSIQGXUp4PS77L7Q1K2-fi_oLDNIXaToxY1OsOncf_OfqCWtLn-wOpZsHRzLUgn5bb4AUQBAqZAQqUAQjLNBAAGoYBMIGDAoGAayi6IvcdulmAeBrhdNhfpQVdYyuoNuG37F9ZoB70zgHyR_2fz9WXk9s727MarrxTgypykNVar9pvHH9CYy-hT004rKytwhhma_EHzOe52GNYDF2i54Cus6DIO6TvaSvRUBFiN61wPiM9_8IgZZoSpfe-694Qvl5mL6k-tL8F9uUgn7ng4AUQBAqaAQqVAQjXNBAAGocBMIGEAoGBAIYnbhXOvu7ST7tHuuX7eM_-RlKH7HeXtIgWTUm6cY0ho6gsRCPP6sc1gzxq0hX9pQNU-OO2gVt6bw1IiUl2-YZBrWR8oPmmN235T87tK-JwmQSFgdrL_x8tEFev5P7BUY1gPPH_T-ZHLAldqK46wyRFMRI44Fj7J9oH1IrEzGNFIJ_c5eAFEAQKmgEKlQEI4zQQABqHATCBhAKBgQDkWt5SDKE921hcaY1eX6YKYVErRQdhI8mfs0BBq813BLgepHU8UNXgjBDLT13_FwOfck4Wxtan3fnwsFd0cr1sZawdceKfs2hEMHyDla52Rss87YRRboKfuxjFfNwrLbY4k7Ux9_-1KMBMNHqLySZlSn8IVNkGpOP9mcPDr_uPpSCf_-rgBRAECpkBCpQBCO80EAAahgEwgYMCgYBl6c6gcPtciUODp77TFMeMJYj9GrErLrzDRzhHG6v03BfVS3-DfdIf7Erotvm7TG2CqF0yGTGHW_l5aIajSzhIG8b-v2rUKYDHSfdt1hjoFj8UMzI3dAe9ilYjw-ohZ5GpYSWklZ7vo7-PfDmsIPX1DjasbhB6RRpwG4M4OIUG9SCfovDgBRAEEpcBCpQBCNEzEAAahgEwgYMCgYB2mewguPv97UIP2RYCMyjzao_ihvII02qIR4dZkXHA7aL5LsIy3FtpP8HiTTOYBI77Ik8lTd9oBT56XlYJXp5U5SjMkJurlxKvizEK7T5w9zRzmD84Ui9klN4gD531wtv6Gwa7ULFLoz4qoE9zY0HO1dSKyNNSG3FL1jlo7fEPBSCf26vgBRKYAQqVAQjdMxAAGocBMIGEAoGBAJdPQ6WXZgZAaxBHb_nEqfWcvOlct7-N552F3XTgQct7MspbNWGa2TCyB9_W51tRhIyMtOK2AYWpuP3uYERJOmt_cnYidWfMmLH6hbeNTiIzuyJR9PKHMEKAQhnUkbY3cGAGbX_hNkG-i2KykmSUk5C_4_9pNfI89FzhtpvKunztIJ7-sOAFEpgBCpUBCOkzEAAahwEwgYQCgYEAr9fWn33kh_XwOnspkU2656uSZFwBYTSeOZP97wGq5e-aAwULhawtREYV8eO8HZolY8g9D5MfdLpxfr1qXdqqL7JWIFClZnYVclGcBckzEBq1i5PIqvmNKOuP2pzQANBRuje_trvQYfrRdfx9Z_4NaCai1ufgNj259M5RCCM0JUUgoKG24AUSmAEKlQEI9TMQABqHATCBhAKBgQDZdR9pb5C028Iyqs1bgJ95_lstkU2Y_q9wUbPzubc0c7svU81D4dT0XPzcRKoVpuFB6tjQKuAVy4w2CDZ9cYB9SdutGUTXjZoyO8ZEMdIDPEr8gHB27ECP3YhMSjIWf3dequzzpuGjQ7kgXJi3duYkllr315rzXx3hOKnhCzLd9SCexLvgBRKYAQqVAQiFNBAAGocBMIGEAoGBAKkbx8HPL-d-7N9RqVeu8FMo-m94FzykGkL9SkaB5NWj15RT8TXsJ8NC0KsRXKRz5fGx5HmmdIhxfsjaMXG676G1NfPGo7RZUCECf6R6r_sySOzgh-4fPmAmnoJoy5iJV4i8EXWRSz1ptqxTA_LIUvXK07jzWmcR_ar67hbYJEKVIJ_nwOAFEpcBCpQBCJE0EAAahgEwgYMCgYBrdarng_dcu_7b1_rMjdXlb0dkvTHiQ-yS83ijqQVjN7zFQj1oOvGBWjxvLcBH-W-oI4Z82tbB2mKnvadYTlLHwkMsIxWtar1TpgqJnf4pW_w6XhrRqzai-aKPAtERvTvfA6ZLPjegt4L9KsYpt5b5xxSazIwkq8AT5oKVnPaXbSCfisbgBRKYAQqVAQidNBAAGocBMIGEAoGBAIx_5tpAwDfAxdIoRas5jLcV4G3NwfU3CIi0glrjUtiulp4voHtfWOdVZOmI4Q_Ct9IPWvXGDHRz3uL344uopiER4OCed-BdweWYv5_U4ig2DmJIvCMg2qZ5rl4bocnQNKxWg3TQUNdrfGLLJhhwJEaqr_c5_eFzrGp0YfPC9VZdIKCty-AFEpcBCpQBCKk0EAAahgEwgYMCgYBMVKrTDUOS2HxyBRWsIXgX9hxtKY5y7tWpEVp0s-9YmGVst5bWVOo8wnwvUf7_GmP7gybqhBVnfO3-xljPOcZqRLrRoyThoOFKhb7-NAHFRO1pLb2yPvWIJm5xjKJsnenWcjo13_b0HYAn80DoQQuv6vxPCKog29IQ_CSs7gMGvSCf0NDgBRKYAQqVAQi1NBAAGocBMIGEAoGBAIWhO4not678zUNPhrKlVyYW9wDXq7BLcQiwxKB5dVW1TkXop_te_Q8E04uHuM6L4UsPCWqKs3711oTkbAq2_1rdE0FRAYIacvpO5ZSbvG_PM8e7ERoFqeTjx6xuGWvrsHWsV0ORfBXRmKtKZm00zJ6-PGs5WdJympLW2VbZTPJtIJ7z1eAFEpgBCpUBCME0EAAahwEwgYQCgYEAzu0BuV8Cls0fBxqlHuFVTQ0mnPsmILYBWJAuiY5PbTMg8UYZj5lrHTc0G-hmYnTZh3b1IQJyrPMVZVGXhB7U_JkpHr62PBzElghMgclBD6htWrWoX4V9UjQp1Be6zU6Pz05CVZRfzkIfi8OOW9VtVcfglXUEL4MQuZvmi_nOTmUgn5bb4AUSmAEKlQEIzTQQABqHATCBhAKBgQDG8NuBfkLNZ3NGLYQtGrXYAKi4CcvVvX1S_OU38IrFcRpA9cZ0ednMLRCrfjEGiRaLJPaI7t4t6OYvszpMSRDVmG35JwkUCrNsfXfDOzwYF4hRF0Ss5vzh2tFwmwiT9Wohp_t8IWBflgvYCZ5TuyGuqf-Pyf6912HTpUNwBhoy1SCgueDgBRKYAQqVAQjZNBAAGocBMIGEAoGBAJYJpcGWitNYyHnBFjMO48MmWSIJ40dICaN2HrQ4Btf_p18Amijt1qT6Rot6ouLOhKKni8oCQ4o9M12qMsYcKVTx7j-gxMarLZyF_cWa-1kcJzoFHQapj7e0aVgslyKGy5byaT3uY1ryGn_J7SjN1Agy6SePSgrpdGBwLLnf5SHFIJ_c5eAFEpcBCpQBCOU0EAAahgEwgYMCgYB0ZT6v0Gppx85qPy3fnRfH9lvOwr0YruLvkLIaW1G7PzAYNUmFhjmazqL70s3mctikTogZaq3DzBEI5PR2SC0rBE2DRJxUt_vFKNvTf8euJHK4k1gtMIF1UDM3PIoAo8ntZ312QDuAXhcGc9TgdRx7iYI7byINi03l080nV0b0lSCf_-rgBRKYAQqVAQjxNBAAGocBMIGEAoGBAIqBZxcYt5z3jY5UqnEDqoaDZNY1hGMT2fai_66I7TBz65bHhnQ0IYHKJQQPSsqRJqfB3ySOrXkGhBHLyUEznDsiH3ugUUxcAopQIXjOxP0DoZeBgjud0VmQO2UpEzmYp7sPbv37kqL2TFegh3pWaXjosMD5Dw3VsrbQJI6kfNINIJ-i8OAF', \
                   200
        except BaseException:
            import traceback
            traceback.print_exc()

    @app.route('/2/ticket/', methods=['POST'])
    def tvm_service_ticket():
        try:
            dst_tvm_id = flask_request.form['dst']
            return json.dumps({
                str(dst_tvm_id): {'ticket': 'fake-ticket'}
            }), 200
        except BaseException:
            import traceback
            traceback.print_exc()

    @app.route('/1/tokens', methods=['POST'])
    def secret():
        data = flask_request.get_json()['tokenized_requests'][0]
        assert data['signature'] == service_id

        if data['token'] in (delegation_token_1, delegation_token_3):
            return json.dumps({'status': 'ok', 'secrets': NEW_SECRET}), 200
        elif data['token'] == delegation_token_2:
            return json.dumps({'status': 'ok', 'secrets': ONLY_ONE_KEY_SECRET}), 200
        else:
            raise AssertionError('unknown delegation token')

    web_cfg = {'web': {'http': {
        'host': 'localhost',
        'port': 0,
    }}}

    web_server = WebServer(web_cfg, app, version='test')
    web_thread = gevent.spawn(web_server.run)

    request.addfinalizer(web_server.stop)
    request.addfinalizer(web_thread.kill)

    conf_file = ctl.dirpath('loop.conf').strpath
    parser = SafeConfigParser()
    parser.read(conf_file)
    port = web_server.wsgi.socket.getsockname()[1]

    parser.set('defaults', 'hq_default_url_sas', 'http://localhost:{}/'.format(port))
    parser.set('defaults', 'yav_url', 'http://localhost:{}'.format(port))
    parser.set('defaults', 'tvm_api_url', 'http://localhost:{}'.format(port))

    with open(conf_file, 'w') as fd:
        parser.write(fd)

    return web_server


def _get_port(web_server):
    return web_server.wsgi.socket.getsockname()[1]


def test_get_secrets_from_yav(ctl, request, hq_and_yav):
    port = _get_port(hq_and_yav)
    env = get_env()
    env['NANNY_TVM_SECRET'] = 'zzd9iTnS3T6T28sFhWHVzz'
    must_start_instancectl(ctl, request, ctl_environment=env, console_logging=True,
                           add_args=['--hq-url', 'http://localhost:{}/'.format(port)])
    gevent.sleep(10)

    # Check env secrets
    assert ctl.dirpath('secret.txt').read().strip() == ENV_VALUE
    assert ctl.dirpath('secret_from_install.txt').read().strip() == ENV_VALUE
    assert ctl.dirpath('secret_with_only_key.txt').read().strip() == ONLY_ONE_KEY_VALUE

    # Check secrets from volumes
    assert ctl.dirpath('mount_path').stat().mode % 0o1000 == 0o700
    d = {field['key']: field['value'] for field in NEW_SECRET[0]['value']}
    for f in ctl.dirpath('mount_path').listdir():
        assert f.stat().mode % 0o1000 == 0o600
        assert f.read().strip() == d[f.basename]

    must_stop_instancectl(ctl, check_loop_err=False)

    content = ctl.dirpath('loop.conf').read()
    content = content.replace('[test_yav]', '[some_fake_section]')
    ctl.dirpath('loop.conf').write(content)
    ctl.dirpath('state', 'instance.conf').remove()
    p = subprocess.Popen([ctl.strpath, 'start', '--hq-url', 'http://localhost:{}/'.format(port)],
                         cwd=ctl.dirname, env=get_env())
    gevent.sleep(10)
    assert p.poll() == common.INSTANCE_CTL_CANNOT_INIT
