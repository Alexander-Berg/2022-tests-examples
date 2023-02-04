# -*- coding: utf-8 -*-

import os
import sys
api_folder = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if api_folder not in sys.path:
    sys.path.append(api_folder)

from saas_api.message import SaasMessage
from saas_api.message_delete import SaasDeleteMessage
from saas_api.message_delete import SaasDeleteKpsMessage, SaasDeleteAllIndexMessage
from saas_api.document import SaasDocument
from saas_api.attribute import SaasAttribute
from saas_api.attribute import ATT
from saas_api.zone import SaasZone
from saas_api.annotations import SaasAnnotation
from saas_api.annotations import LANG, SaasAnnRegion
from saas_api.client import ReSendAsyncMessage, SendMessage, SetSaasApiDebugLogLevel
from saas_api.context import ToJsonContext

import json
import logging
import time
import urllib2

SetSaasApiDebugLogLevel()


def search(search_addr, cgi_part, prefixed):
    search_url = search_addr + '?service=tests&format=json&' + cgi_part
    if prefixed and 'kps=' not in cgi_part:
        search_url += '&kps=5'
    try:
        req = urllib2.urlopen(search_url)
        req_txt = req.read()
    except urllib2.HTTPError as e:
        logging.warning('HttpError %s' % e)
        req_txt = e.read()
    except Exception as e:
        logging.error('while search, error %s' % e)
        return False
    logging.info('Response %s' % req_txt)
    res = json.loads(req_txt)
    return res


def search_check_count(search_addr, query, prefixed, count):
    res = search(search_addr, query, prefixed)
    n_results = int(res['response']['found']['all'])
    assert n_results == count


def run_some_test(index_addr, search_addr, prefixed=False):
    sm = SaasMessage()
    sm.gen_document(url='123', body='sometext')
    if prefixed:
        sm.Prefix = 5
    if not SendMessage(index_addr, sm)['result']:
        return False

    search_check_count(search_addr, '&text=sometext', prefixed, 1)
    search_check_count(search_addr, '&text=url:"123"', prefixed, 1)
    return True


def run_attr_test(index_addr, search_addr, prefixed=False):
    sm = SaasMessage()
    doc = SaasDocument(url='123', body='sometext')
    doc.add_attribute(SaasAttribute('s_some_attr', ATT.search_attr_literal, 'megaattr'))
    doc.add_attribute('s_some_more_attr', '#l', 'moreattr')
    doc.add_attribute('s_some_more_attr', '#l', 'cucumber')
    doc.add_attribute('s_some_more_attr', '#l', 'огурец')
    doc.add_attribute(SaasAttribute('i_some_int_attr', ATT.search_attr_int, 90))
    doc.add_attribute('i_some_int_attr', '#i', 2)
    doc.set_attribute([SaasAttribute('i_some_int_attr', '#i', 22),
                       SaasAttribute('i_some_int_attr', '#i', 25)])
    sm.set_document(doc)
    if prefixed:
        sm.Prefix = 5
    if not SendMessage(index_addr, sm)['result']:
        return False
    search_check_count(search_addr, '&text=s_some_attr:megaattr', prefixed, 1)
    search_check_count(search_addr, '&text=s_some_more_attr:moreattr', prefixed, 1)
    search_check_count(search_addr, '&text=s_some_more_attr:cucumber', prefixed, 1)
    search_check_count(search_addr, '&text=s_some_more_attr:огурец', prefixed, 1)
    search_check_count(search_addr, '&text=s_some_more_attr:crendel', prefixed, 0)
    search_check_count(search_addr, '&text=i_some_int_attr:22', prefixed, 1)
    search_check_count(search_addr, '&text=i_some_int_attr:90', prefixed, 0)
    return True


def run_kv_test(index_addr, search_addr, prefixed=False):
    prefix = 5 if prefixed else 0
    sm_kv = SaasMessage(prefix=prefix)
    sm_kv.gen_document(url='vbgjuiti90', mime_type=None)
    sm_kv.Document.add_attribute('key_key', ATT.special_key, 'kvv')
    if not SendMessage(index_addr, sm_kv)['result']:
        return False
    search_check_count(search_addr, '&text=kvv&key_name=key_key&sgkps=' + str(prefix), prefixed, 1)
    return True


def run_zone_test(index_addr, search_addr, prefixed=False):
    sm = SaasMessage()
    doc = SaasDocument(url='123')
    doc.add_zone('z_title', 'my summer')
    doc.add_zone(SaasZone('z_title', 'chupachups'))

    doc.get_zone('z_title')[0].add_zone('z_content', 'hot august')
    doc.get_zone('z_title')[0].get_zone('z_content')[0].add_attribute(SaasAttribute('i_days', ATT.search_attr_int, 30))

    sm.set_document(doc)
    if prefixed:
        sm.Prefix = 5
    if not SendMessage(index_addr, sm)['result']:
        return False

    search_check_count(search_addr, '&text=z_title:summer', prefixed, 1)
    search_check_count(search_addr, '&text=z_title:chupachups', prefixed, 1)
    search_check_count(search_addr, '&text=z_title:fignya', prefixed, 0)
    search_check_count(search_addr, '&text=z_content:hot', prefixed, 1)
    search_check_count(search_addr, '&text=i_days:30', prefixed, 1)
    return True


def run_ann_test(index_addr, search_addr, prefixed=False):
    prefix = 5 if prefixed else 0
    doc_url = '123'
    sm = SaasMessage(prefix=prefix)
    ann = SaasAnnotation(text='ann_text', lang=LANG.LANG_ENG,
                         regions=[SaasAnnRegion(region=5, streams={'st1': '2.0f'})])
    sm.gen_document(url=doc_url)
    sm.Document.annotations = [ann]
    if not SendMessage(index_addr, sm)['result']:
        return False
    search_check_count(search_addr, '&text=url:' + doc_url, prefixed, 1)
    return True


def run_deletion_test(index_addr, search_addr, prefixed=False):
    doc_url_1 = '1qazxsw2'
    doc_url_2 = 'omnji9'
    prefix = 5 if prefixed else 0
    sm1 = SaasMessage(prefix=prefix)
    sm2 = SaasMessage(prefix=prefix)
    sm1.gen_document(url=doc_url_1)
    sm2.gen_document(url=doc_url_2, body='балда')
    sm2.Document.add_attribute('i_attr', ATT.search_attr_int, 3)
    if not SendMessage(index_addr, sm1)['result']:
        return False
    if not SendMessage(index_addr, sm2)['result']:
        return False

    search_check_count(search_addr, '&text=url:' + doc_url_1, prefixed, 1)
    search_check_count(search_addr, '&text=url:' + doc_url_2, prefixed, 1)

    sdm_by_url = SaasDeleteMessage(url=doc_url_1, prefix=prefix)
    sdm_by_req = SaasDeleteMessage(prefix=prefix, request='балда&i_attr:3')

    res_resend = ReSendAsyncMessage(index_addr, sdm_by_url.to_json(ToJsonContext()), 5, 1)
    if not res_resend['result']:
        raise Exception('%s' % res_resend)
    logging.info('%s' % res_resend)
    search_check_count(search_addr, '&text=url:' + doc_url_2, prefixed, 1)
    search_check_count(search_addr, '&text=url:' + doc_url_1, prefixed, 0)

    res_resend = ReSendAsyncMessage(index_addr, sdm_by_req, 5, 1)
    if not res_resend['result']:
        raise Exception('%s' % res_resend)
    logging.info('%s' % res_resend)
    search_check_count(search_addr, '&text=url:' + doc_url_2, prefixed, 0)

    return True


def run_special_deletion_test(index_addr, search_addr, prefixed=False):
    prefix1 = 5 if prefixed else 0
    prefix2 = 6 if prefixed else 0
    sm1 = SaasMessage(prefix=prefix1, document=SaasDocument(url='123'))
    SendMessage(index_addr, sm1)
    if prefixed:
        #check remove_kps message
        sm2 = SaasMessage(prefix=prefix2, document=SaasDocument(url='456'))
        SendMessage(index_addr, sm2)
        search_check_count(search_addr, '&text=url:"456"&kps=6', prefixed, 1)
        rm_kps = SaasDeleteKpsMessage(prefix2)
        res_rm_kps = ReSendAsyncMessage(index_addr, rm_kps, 5, 1)
        if not res_rm_kps['result']:
            raise Exception('%s' % res_rm_kps)
        search_check_count(search_addr, '&text=url:"456"&kps=6', prefixed, 0)

    search_check_count(search_addr, '&text=url:"123"', prefixed, 1)
    #check remove_all message
    rm_all = SaasDeleteAllIndexMessage()
    res_rm_all = ReSendAsyncMessage(index_addr, rm_all, 5, 1)
    if not res_rm_all['result']:
        raise Exception('%s' % res_rm_all)
    search_check_count(search_addr, '&text=url:"123"', prefixed, 0)
    return True


if __name__ == '__main__':
    iaddr = sys.argv[1]
    saddr = sys.argv[2]
    test_func_name = sys.argv[3]
    if not test_func_name.startswith('run_'):
        test_func_name = 'run_' + test_func_name
    if not test_func_name.endswith('_test'):
        test_func_name += '_test'
    res = locals()[test_func_name](iaddr, saddr)
    if not res:
        sys.exit(1)
