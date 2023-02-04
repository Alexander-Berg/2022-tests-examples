import config
import re

PAYSYS_REGEX = config.RE_PAYSYS_TOKENS
PAYSYS_START_REGEX = config.RE_PAYSYS_STARTS
PAYSYS_END_REGEX = config.RE_PAYSYS_ENDS

QUEUE_REGEX = config.RE_QUEUE_TOKENS
QUEUE_START_REGEX = config.RE_QUEUE_STARTS
QUEUE_END_REGEX = config.RE_QUEUE_ENDS

def get_context_from_message(regexps, st):
    regexps = {re.compile(key): val for key, val in regexps.items()}
    result = {}
    for regex, vals in regexps.items():
        matching_res = regex.match(st)
        if matching_res is not None:
            result.update({token: matching_res.group(num+1) for num, token in enumerate(vals)})
    return result


def test_get_context_from_message():
    regexps = {r"([0-9]+)gt([0-9]+)gt([a-z]*)": ["uid", "more_id", "text"]}
    assert get_context_from_message(regexps, "124gt1244gtgtgt").get("more_id") == "1244"


class TestPaysysRegex:
    def test_method_starts_0(self):
        assert get_context_from_message(PAYSYS_START_REGEX, "Method some started. ctx_id=safa1525").get("method_name") == "some"

    def test_method_starts_1(self):
        assert get_context_from_message(PAYSYS_START_REGEX, "Method some started. ctx_id=safa1525").get("nginx_request_id") == "safa1525"

    def test_method_ends(self):
        assert get_context_from_message(PAYSYS_END_REGEX, "Method some ended. Execution time: 142.002s").get("method_name") == "some"

    def test_0_0(self):
        assert get_context_from_message(PAYSYS_REGEX, "dump params Trust.ListPaymentMethodsEx('31241241', {'user_ip': "
                                      "'222.13.23.30', 'balance_service_id': 212, 'show_all': False, 'show_removed': "
                                      "False, 'service_token': '1333322_dsss'})").get("service_id") == "212"

    def test_0_1(self):
        assert get_context_from_message(PAYSYS_REGEX, "dump params Trust.ListPaymentMethodsEx('31241241', {'user_ip': "
                                      "'222.13.23.30', 'balance_service_id': 212, 'show_all': False, 'show_removed': "
                                      "False, 'service_token': '1333322_dsss'})").get("uid") == "31241241"

    def test_1_0(self):
        assert get_context_from_message(PAYSYS_REGEX, "dump params BalanceSimple.CheckBasket("
                                      "'adsad11d21d2', {'user_ip': "
                                      "'4124:212:1233:e9d7:b500:13132:8f1a:85d3', 'with_promocodes': 1, "
                                      "'trust_payment_id': '1241241244', 'uid': '124124', "
                                      "'purchase_token': '12412412412'})").get("service_id") == "adsad11d21d2"

    def test_1_1(self):
        assert get_context_from_message(PAYSYS_REGEX, "dump params BalanceSimple.CheckBasket("
                                      "'adsad11d21d2', {'user_ip': "
                                      "'1242:2333:8624b:2333:1331:1332:2222:85d3', 'with_promocodes': 1, "
                                      "'trust_payment_id': '123321', 'uid': '13213', "
                                      "'purchase_token': '123132213'})").get("uid") == "13213"

    def test_2(self):
        assert get_context_from_message(PAYSYS_REGEX, "dump params Trust.CreatePayment('uid=1323123', {'payment_method': "
                                      "'afsfs-asfs', 'user_phone': '+312323', 'currency': "
                                      "'BYN', 'orders': [{'product_type': None, 'fiscal_inn': '', 'fiscal_title': '', "
                                      "'developer_payload': '', 'qty': '', 'amount': '3.70', 'service_order_id': "
                                      "'1241412f1fsf1w', 'fiscal_nds': ''}], 'discounts': [], "
                                      "'sum': '3.70', 'fiscal_data': {'taxation_type': '', 'service_email': "
                                      "'yyy@hh.yandex.ru', 'partner_inn': '', 'partner_phone': '', "
                                      "'agent_type': 'agent', 'service_url': 'taxi.yandex.ru', "
                                      "'client_email_or_phone': '+124424'}, 'back_url': "
                                      "'https://s-sasddsadsdst:8038/asf/asdf/asss', "
                                      "'user_ip': '23.23.12.22', 'commission': '0.00', 'balance_service_id': 33, "
                                      "'payment_method_cc': 'card', 'mcc': None, 'bind_after_payment': 0, "
                                      "'payment_mode': 'dasdds', 'purchase_token': "
                                      "'asdsdadsad', 'lang': '', 'pass_params': {"
                                      "'taxi_order_id': 'asfaf231', "
                                      "'taxi_user_phone_id").get("uid") == "1323123"

    def test_3(self):
        assert get_context_from_message(PAYSYS_REGEX, "dump call bind_apple_token -> {u'payment_method': "
                                      "u'adsfasf-s-afssfas',#012 u'rrn': u'',"
                                      "#1342 u'status': u'success',#012 u'status_desc': u'asfsss ok',"
                                      "#213 u'trust_payment_id': u'14221fsfa424'}; http status 11").get("trust_payment_id") == "14221fsfa424"

    def test_4(self):
        assert get_context_from_message(PAYSYS_REGEX, "method: post_refunds, params: {"
                                      "u'fake_refund': 0, u'fiscal_force': None, u'transaction_request_id': None, "
                                      "u'user_email': u'sdad@yandex.ru', u'paymethod_markup': None, "
                                      "u'user_phone': u'', u'terminal_id': 31441, u'card_id': "
                                      "u'saffsa1241', u'fiscal_data': {u'service_email': "
                                      "u'asd@ss.yandex.ru', u'partner_inn': u'', u'partner_phone': u'', "
                                      "u'agent_type': u'agent', u'client_email_or_phone': u'adsd223@asds.ru', "
                                      "u'service_url': u'https://sda.sddd.ru/', u'taxation_type': u''}, "
                                      "u'back_url': "
                                      "u'https://sads-sdad.sad.asd.net:8028/asd/s/sss', "
                                      "u'currency': u'RUB', u'amount': u'231.50', u'passport_id': u'1345', "
                                      "u'payment_amount': u'11.50', u'reason_desc': u'aff', u'orders': [{"
                                      "u'product_type': u'prod', u'fiscal_inn': u'1422', u'fiscal_title': "
                                      "u'\\u041e\\u043f\\u043b\\u0430\\u0442\\u0430 "
                                      "\\u0442\\u043e\\u043f\\u043b\\u0438\\u0432\\u0430 \\u0410\\u0418-95, "
                                      "\\u0422\\u0430\\u0442\\u043d\\u0435\\u0444\\u0442\\u044c \\u21164}").get("uid") == "1345"

    def test_5(self):
        assert get_context_from_message(PAYSYS_REGEX, "method: post_fiscals, params: {u'payment_info': {u'payment_method': u'card', "
                                      "u'sum': u'23.95', u'terminal_id': 3123, u'fiscal_data': {u'service_email': "
                                      "u'asd@sds.yandex.ru', u'partner_inn': u'', u'partner_phone': u'', "
                                      "u'agent_type': u'agent', u'client_email_or_phone': u'adsd@ss.ru', "
                                      "u'service_url': u'https://sda.sda.ru/', u'taxation_type': u''}, "
                                      "u'orders': [{u'product_type': u'prod', u'fiscal_inn': u'13231', "
                                      "u'fiscal_title': u'\\u041e\\u043f\\u043b\\u0430\\u0442\\u0430 "
                                      "\\u0442\\u043e\\u043f\\u043b\\u0438\\u0432\\u0430 \\u0410\\u0418-95, "
                                      "\\u0415\\u041a\\u0410 \\u21162', u'developer_payload': u'', u'qty': u'1234.88', "
                                      "u'amount': u'599.95', u'service_order_id': "
                                      "u'asdasd123', u'fiscal_nds': u'nds_20'}], "
                                      "u'service_id': 1344451, u'is_delivery': True, u'purchase_token': "
                                      "u'asdaf21f23sda', u'user_email': u'asd@ss.ru'}, "
                                      "u'obj_type': u'delivery', u'external_id': u'asfafs2131', "
                                      "u'back_url': u'https://sda-asd.sda.asd.net:8028/sd/s}").get("service_id") == "1344451"

    def test_6(self):
        assert get_context_from_message(PAYSYS_REGEX, "method: post_fiscals, params: {u'payment_info': {u'payment_method': u'card', "
                                      "u'sum': u'23.95', u'terminal_id': 3123, u'fiscal_data': {u'service_email': "
                                      "u'asd@sds.yandex.ru', u'partner_inn': u'', u'partner_phone': u'', "
                                      "u'agent_type': u'agent', u'client_email_or_phone': u'adsd@ss.ru', "
                                      "u'service_url': u'https://sda.sda.ru/', u'taxation_type': u''}, "
                                      "u'orders': [{u'product_type': u'prod', u'fiscal_inn': u'13231', "
                                      "u'fiscal_title': u'\\u041e\\u043f\\u043b\\u0430\\u0442\\u0430 "
                                      "\\u0442\\u043e\\u043f\\u043b\\u0438\\u0432\\u0430 \\u0410\\u0418-95, "
                                      "\\u0415\\u041a\\u0410 \\u21162', u'developer_payload': u'', u'qty': u'1234.88', "
                                      "u'amount': u'599.95', u'service_order_id': "
                                      "u'asdasd123', u'fiscal_nds': u'nds_20'}], "
                                      "u'assdd': 134saf4451, u'is_delivery': True, u'purchase_token': "
                                      "u'asdaf21f23sda', u'user_email': u'asd@ss.ru'}, "
                                      "u'obj_type': u'delivery', u'external_id': u'asfafs2131', "
                                      "u'back_url': u'https://sda-asd.sda.asd.net:8028/sd/s}").get("purchase_token") == "asdaf21f23sda"

    def test_7(self):
        assert get_context_from_message(PAYSYS_REGEX, "method: post_refunds_start, result: \"{'status': 'success', 'fiscal_status': "
                                      "'', 'trust_refund_id': 'afsafssf2312', 'amount': '22.82', "
                                      "'balance_service_id': 132, 'trust_payment_id': "
                                      "'asfasfas2131'}\"").get("trust_refund_id") == "afsafssf2312"

    def test_8(self):
        assert get_context_from_message(PAYSYS_REGEX, "dump call Trust.DoPaymentEx('681437116', 'asdasdasd21313', {}) -> {"
                                      "'status': 'wait_for_notification', 'payment_id': 'afsasf12313', "
                                      "'uid': '421412', 'fiscal_status': '', 'masked_pan': u'1222****333', "
                                      "'payment_method': u'card', 'user_phone': u'+12314222', 'status_desc': 'just "
                                      "started', 'card_id': 'asdasdafas123', 'currency': u'RUB', "
                                      "'amount': '302.00', 'is_binding_payment': False, 'payment_method_full': "
                                      "u'asdsd-asdasf21312', 'cardholder': u'sad sd', "
                                      "'payment_mode': u'api_payment', 'balance_service_id': 133, 'payment_dt': "
                                      "'231-10-22:131.095000', 'payment_type': u'common_payment', "
                                      "'purchase_token': u'asfafsfsa231', 'payment_timeout': "
                                      "'133'}").get("uid") == "421412"

    def test_9_0(self):
        res = get_context_from_message(PAYSYS_REGEX,
            'Payment authorization status: {"payment_method": "trust_web_page", "payment_status": "invalid_xrf_token", '
            '"payment_type": "common_payment", "purchase_token": "fec25dfff370e85d07ad4905e1e75765", '
            '"service_id": "23", "trust_payment_id": "5ef1a79f2af6cd2c6d0f746a", "uid": "833826184"}')
        assert res.get("purchase_token") == "fec25dfff370e85d07ad4905e1e75765"
        assert res.get("service_id") == "23"
        assert res.get("trust_payment_id") == "5ef1a79f2af6cd2c6d0f746a"
        assert res.get("uid") == "833826184"

    def test_9_1(self):
        res = get_context_from_message(PAYSYS_REGEX,
            'Binding payment: {"payment_method": "new_apple_token", "payment_status": "invalid_terminal_setup", '
            '"payment_type": "binding_payment", "purchase_token": "0117c14934053c168f8829ae91558970", '
            '"service_id": "124", "trust_payment_id": "5ef1a94e792ab1676677efdf", "uid": "797621230"')
        assert res.get("purchase_token") == "0117c14934053c168f8829ae91558970"
        assert res.get("service_id") == "124"
        assert res.get("trust_payment_id") == "5ef1a94e792ab1676677efdf"
        assert res.get("uid") == "797621230"

    def test_10(self):
        assert get_context_from_message(PAYSYS_REGEX,
            'Execution context: {"call_status": "success", "ctx_id": "566a1717a0f74360928ce876b3600589", '
            '"mname": "yb_trust_paysys.get_bindings_verifications", "purchase_token": "a441244e2bf1db0e42dc1ab3ec6c20b1", '
            '"run_time": 0.020550012588500977, "service_id": "124", "trust_payment_id": "5e6a239256858200017ab762", '
            '"ts0": 1584014227.291144, "uid": "119003"}').get("purchase_token") == "a441244e2bf1db0e42dc1ab3ec6c20b1"

    def test_11(self):
        assert get_context_from_message(PAYSYS_REGEX,
            'Execution context: {"call_status": "success", "ctx_id": "None", "mname": "trust_internal.post_refunds", '
            '"run_time": 0.017446041107177734, "service_id": "610", "trust_payment_id": "5e6a238e56858200017ab745", '
            '"ts0": 1584014228.609472, "uid": "119008"}').get("trust_payment_id") == "5e6a238e56858200017ab745"

    def test_12(self):
        assert get_context_from_message(PAYSYS_REGEX,
            'Execution context: {"call_status": "success", "ctx_id": "CTTCRCVAJM", "mname": "trust_internal.get_card_list", '
            '"run_time": 0.012620925903320312, "ts0": 1584014227.90629, "uid": "116003"}').get("uid") == "116003"

    def test_13(self):
        assert get_context_from_message(PAYSYS_REGEX,
            'Execution context: {"call_status": "success", "ctx_id": "3d44fb0c33f641f084d4758f051f15f0", "mname": "Trust.ListPaymentMethodsEx", '
            '"passport_id": 119003, "run_time": 0.030858993530273438, "service_id": "118", '
            '"ts0": 1584014228.383727, "uid": "119003"}').get("service_id") == "118"

    def test_14(self):
        assert get_context_from_message(PAYSYS_REGEX, "Response from BlackBox (duration=123.025s): {'status': None, 'domain': None, "
                                      "'bruteforce_policy': {'captcha': False, 'level': None, 'password_expired': "
                                      "False}, 'login_status': None, 'uid': '52366'}").get("uid") == "52366"

    def test_15(self):
        assert get_context_from_message(PAYSYS_REGEX, "check limit for api.get_bindings_verification('57238155212')").get("uid") == '57238155212'

    def test_16(self):
        assert get_context_from_message(PAYSYS_REGEX, 'payment context: {"purchase_token": "42basfa2211a2511"}').get("purchase_token") == "42basfa2211a2511"

    def test_17(self):
        assert get_context_from_message(PAYSYS_REGEX, 'payment context: {"service_id": "bb421215qfsatscs4511"}').get("service_id") == "bb421215qfsatscs4511"

    def test_18(self):
        assert get_context_from_message(PAYSYS_REGEX, 'payment context: {"trust_payment_id": "as12124gsloi41512"}').get("trust_payment_id") == "as12124gsloi41512"

    def test_19(self):
        assert get_context_from_message(PAYSYS_REGEX, 'payment context: {"uid": "4212511"}').get("uid") == "4212511"


class TestQueueRegex:
    def test_method_starts(self):
        assert get_context_from_message(QUEUE_START_REGEX, "Something process: started").get("method_name") == "Something process"

    def test_method_ends(self):
        assert get_context_from_message(QUEUE_END_REGEX, "Something process: done").get("method_name") == "Something process"

    def test_0_0(self):
        res = get_context_from_message(QUEUE_REGEX,
            'Payment authorization status: {"payment_method": "card", "payment_status": "success", '
            '"payment_type": "common_payment", "purchase_token": "874661267313625286001b9643b4a6e4", "service_id": "124", '
            '"trust_payment_id": "5ef1a87399d6ef0b4bb33e76", "uid": "1021599265"')
        assert res.get("purchase_token") == "874661267313625286001b9643b4a6e4"
        assert res.get("service_id") == "124"
        assert res.get("trust_payment_id") == "5ef1a87399d6ef0b4bb33e76"
        assert res.get("uid") == "1021599265"

    def test_0_1(self):
        res = get_context_from_message(PAYSYS_REGEX,
            'Binding payment: {"payment_method": "card", "payment_status": "declined_by_issuer", '
            '"payment_type": "binding_payment", "purchase_token": "e743f25981d1e8d644504c79ed5a790d", '
            '"service_id": "124", "trust_payment_id": "5ef1a84bf78dba1653e5ad68", "uid": "1114791727"'
        )
        assert res.get("purchase_token") == "e743f25981d1e8d644504c79ed5a790d"
        assert res.get("service_id") == "124"
        assert res.get("trust_payment_id") == "5ef1a84bf78dba1653e5ad68"
        assert res.get("uid") == "1114791727"

    def test_1(self):
        assert get_context_from_message(QUEUE_REGEX, "sending body: mode=results{&rrn=314232}").get("rrn") == "314232"

    def test_2(self):
        assert get_context_from_message(QUEUE_REGEX, "sending body: mode=refund_results{&uid=132412}").get("uid") == "132412"

    def test_3(self):
        assert get_context_from_message(QUEUE_REGEX, "sending body: mode=refund_results{&payment_id=1221e1sds41}").get("trust_payment_id") == "1221e1sds41"

    def test_4(self):
        assert get_context_from_message(QUEUE_REGEX, "sending body: mode=refund_results{&transaction_id=314-%A%232}").get("transaction_id") == "314-%A%232"

    def test_5(self):
        assert get_context_from_message(QUEUE_REGEX, "sending body: mode=refund_results{&purchase_token=12easd21512}").get("purchase_token") == "12easd21512"

    def test_6(self):
        assert get_context_from_message(QUEUE_REGEX, "sending body: mode=refund_results{&trust_refund_id=314afsaf232}").get("trust_refund_id") == "314afsaf232"

    def test_7(self):
        assert get_context_from_message(QUEUE_REGEX, "sending body: mode=refund_results{&balance_service_id=1234152}").get("service_id") == "1234152"

    def test_8(self):
        assert get_context_from_message(QUEUE_REGEX, 'sending body: {"verification_info": {"some": 12312, "binding_id": "asd1521"}}').get("binding_id") == "asd1521"

    def test_9(self):
        assert get_context_from_message(QUEUE_REGEX, 'sending body: {"verification_info": {"some": 12312, "trust_payment_id": "asd1521"}}').get("trust_payment_id") == "asd1521"

    def test_10(self):
        assert get_context_from_message(QUEUE_REGEX, 'sending body: {"verification_info\": {"some": 12312, "uid": "124125551"}}').get("uid") == "124125551"

    def test_11(self):
        assert get_context_from_message(QUEUE_REGEX, 'sending body: {"verification_info": {"some": 12312, "authorize_rrn": "412gaf241"}}').get("rrn") == "412gaf241"

    def test_12(self):
        assert get_context_from_message(QUEUE_REGEX, "Captured a payment in {initial ,some:2} stage: asseomf2151252, 'some': 2").get("trust_payment_id") == "asseomf2151252"

    def test_13(self):
        assert get_context_from_message(QUEUE_REGEX, "Picked up Refund in {initial} stage: asds45199").get("trust_refund_id") == "asds45199"

    def test_14_0(self):
        assert get_context_from_message(QUEUE_REGEX, 'payment context: {"purchase_token": "some125255", "service_id": "142525", "trust_payment_id": "asdas4125", "uid": "52513"}').get("purchase_token") == "some125255"

    def test_14_1(self):
        assert get_context_from_message(QUEUE_REGEX, 'payment context: {"purchase_token": "some125255", "service_id": "142525", "trust_payment_id": "asdas4125", "uid": "52513"}').get("service_id") == "142525"

    def test_14_2(self):
        assert get_context_from_message(QUEUE_REGEX, 'payment context: {"purchase_token": "some125255", "service_id": "142525", "trust_payment_id": "asdas4125", "uid": "52513"}').get("trust_payment_id") == "asdas4125"

    def test_14_3(self):
        assert get_context_from_message(QUEUE_REGEX, 'payment context: {"purchase_token": "some125255", "service_id": "142525", "trust_payment_id": "asdas4125", "uid": "52513"}').get("uid") == "52513"

    def test_15(self):
        assert get_context_from_message(QUEUE_REGEX, "Peeked up a payment in \"wait_for_result\" stage: gssds2415,").get("trust_payment_id") == "gssds2415"
