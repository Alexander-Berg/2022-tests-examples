import json
import responses

from datetime import datetime
from irt.artmon import ArtmonAPI
from irt.utils import open_resource_or_file
from six.moves.urllib.parse import unquote


@responses.activate
def test_artmon():
    request_query = json.dumps(
        {"filters": {
            "YandexFilter": "all", "OrderFilter": "all", "TypeFilter": "all", "ContextFilter": "0",
            "group_context_type": ["7", "8"], "UserFilter": "all", "detailed": 1, "Slice": "context", "relative": 0,
            "dont_show_total": 1, "device_type": "all", "group_device_type": None, "group_product_type": None,
            "product_type": "all", "distribution": 0, "placeid_paid_filter": "542,1542", "placeid_filter": "542,1542",
            "pageid_filter": "", "impid_filter": "", "group_select_type": None, "group_sim_distance": None,
            "group_turbo": None, "group_inapp": None, "group_ya_service": None, "group_ssp": None, "group_video": None,
            "group_video_kind": None, "group_video_format": None, "group_preferred_deals": None, "group_is_adfox": None,
            "exclude_pageid_filter": "", "cross_filter": "0", "undefined": "", "domain_ajax": "", "login_ajax": "",
            "failed_money": 0, "show_visit_info": 0, "show_ad_serp_hits": 0, "not_only_good": 0, "discount": 0,
            "hide_ext": 1, "logarithm_axis": 0, "fraction_series": 1, "fraction_series_shift": 1, "ignore_fast": 0,
            "fast_traffic": 0, "clickhouse": 0, "bsclickhouse": 1, "fraud_series": 0,
        }},
        sort_keys=True,
    )

    request_data = {
        "module": "Traffic2",
        "period_start": "2020-05-14",
        "period_end": "2020-05-14",
        "compare_start": "2020-05-07",
        "compare_end": "2020-05-07",
        "period_start_hour": "00",
        "compare_start_hour": "00",
        "period_end_hour": "23",
        "compare_end_hour": "23",
        "compare_enabled": "1",
        "timegroup": "day",
        "query": request_query,
    }

    def request_callback(request):
        data = dict()
        for body_attr in request.body.split("&"):
            key, value = body_attr.split("=")
            data[unquote(key)] = unquote(value.replace("+", " "))
        if data == request_data:
            with open_resource_or_file("artmon_response.json") as f:
                artmon_response = f.read()
            return 200, {}, artmon_response

    responses.add_callback(
        responses.POST,
        "https://artmon.bsadm.yandex-team.ru/cgi-bin/data.cgi",
        callback=request_callback,
    )

    artmon_api = ArtmonAPI("")
    req_res = artmon_api.do_request(
        main_start=datetime(2020, 5, 14),
        main_end=datetime(2020, 5, 14),
        cmp_start=datetime(2020, 5, 7),
        cmp_end=datetime(2020, 5, 7),
        timegroup="day",
        add_filters={"Slice": "context", "group_context_type": ["7", "8"]},
    )

    waiting_wow_results = {
        7: 15.9,
        8: 7.6,
    }
    for context_type in waiting_wow_results:
        curr_sum = sum(elem["cost"] for elem in req_res["items"]["rows"] if ("cost" in elem) and (str(context_type) in elem["series_id"]))
        cmp_sum = sum(elem["cost"] for elem in req_res["items"]["compared"] if ("cost" in elem) and (str(context_type) in elem["series_id"]))
        assert round(100.0 * (curr_sum - cmp_sum) / cmp_sum, 1) == waiting_wow_results[context_type]
