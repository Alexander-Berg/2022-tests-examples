import datetime
import json
import logging
import os
import time
import uuid
from decimal import Decimal
from typing import List
from xmlrpc import client

import paramiko
import pytest
import requests
from bs4 import BeautifulSoup
from requests.adapters import HTTPAdapter
from selenium import webdriver
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.remote.remote_connection import RemoteConnection
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait
from urllib3.util.retry import Retry

try:
    import library.python
    import pkgutil

    ARCADIA_RUN = True

except ImportError:
    ARCADIA_RUN = False

TEST_HOST = os.environ.get('TEST_HOST', 'https://balalayka-test.paysys.yandex-team.ru')
TEST_LOGIN = 'testuser-balance3'


def get_yav_token():
    logging.debug(f'Getting Vault token ...')

    token = os.environ.get('YAV_OAUTH', '')

    if token:
        if os.path.exists(token):
            with open(token) as f:
                token = f.read().strip()

    return token


class Yav:
    """Интерфейс для доступа к данным Yav."""

    def __init__(self):
        try:
            from vault_client import instances

        except ImportError:
            from library.python.vault_client import instances

        init_kwargs = {}

        if ARCADIA_RUN:
            oauth_login, oauth_token = 'testuser-balance1', get_yav_token()

            if oauth_token:
                init_kwargs.update(dict(
                    rsa_login=oauth_login,
                    authorization=oauth_token,
                ))

            else:
                logging.warning(
                    'No token found in YAV_OAUTH env var. '
                    'Will try to access secrets using local credentials.')

        self.yav = instances.Production(**init_kwargs)

    def get_secret(self, secret_id):
        secret = self.yav.get_version(secret_id)
        return secret['value']


YAV = Yav()

SEC_BCL_SFTP = YAV.get_secret('sec-01ct0mhtfh39288cbaf9m0ab9x')
SEC_BCL_TEST_TVM_SURFWAX = YAV.get_secret('sec-01d9n2nk0qmf5h2hqe0ggxjnby')
SEC_PAYPAL_SFTP = YAV.get_secret('sec-01ct0mvttqrwjy6jafgyjj0hn6')
SEC_PAYPAL_AUTH = YAV.get_secret('sec-01dvqs8exazh0c5f82cesn6pf6')
SEC_ROBOT_PWD = YAV.get_secret('sec-01ct0ns9w94qgxr67xjzjxz488')


@pytest.fixture(scope='session')
def yav():
    """Предоставляет доступ к серкретам из YAV."""
    yield YAV


class BCL:

    web_url = TEST_HOST
    xmlrpc_url_tvm = TEST_HOST + '/rpc'
    payment_url_template = TEST_HOST + '/associates/{aid}/payments/'
    bundle_url_template = TEST_HOST + '/associates/{aid}/bundles/'
    statement_url_template = TEST_HOST + '/associates/{aid}/statements/'
    salary_url_template = TEST_HOST + '/associates/{aid}/salary/'
    balance_report_page = TEST_HOST + '/reports/balance/'


@pytest.fixture(scope='session')
def requests_session():
    return requests.Session()


class OAuthRemoteConnection(RemoteConnection):
    """
    https://clubs.at.yandex-team.ru/life-of-qa/583

    Токен: https://oauth.yandex-team.ru/authorize?response_type=token&client_id=630b6794f55a4d9abaa4511eb06d2c5e

    """
    def __init__(self, remote_server_addr, oauth_token, keep_alive=False, resolve_ip=True):
        super(OAuthRemoteConnection, self).__init__(remote_server_addr, keep_alive, resolve_ip)
        self.oauth_token = oauth_token

    def get_remote_connection_headers(self, parsed_url, keep_alive=False):
        headers = super(OAuthRemoteConnection, self).get_remote_connection_headers(parsed_url, keep_alive)

        headers.update({
            'Authorization': 'OAuth %s' % self.oauth_token
        })

        return headers


class DriverWrapper(object):

    capabilities = webdriver.DesiredCapabilities.CHROME.copy()
    capabilities['version'] = '48.0'

    def __init__(self):
        self.driver = None

    def __enter__(self):
        chromedriver_path = '/usr/local/bin/chromedriver'

        if not os.path.exists(chromedriver_path):
            self.driver = webdriver.Remote(
                command_executor=OAuthRemoteConnection(
                    'https://sw.yandex-team.ru:443/wd/v1/quotas/bcl',
                    oauth_token=SEC_BCL_TEST_TVM_SURFWAX['surfwax'],
                    resolve_ip=False,
                ),

                desired_capabilities=self.capabilities
            )

        else:
            chrome_options = Options()
            chrome_options.add_argument('--headless')
            self.driver = webdriver.Chrome(chromedriver_path, chrome_options=chrome_options)

        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.driver.quit()

    def auth_passport(self, login='testuser-balance3'):
        self.driver.get(BCL.statement_url_template.format(aid=1))
        WebDriverWait(self.driver, timeout=30).until(EC.presence_of_element_located((By.NAME, 'login')))
        self.driver.find_element(By.NAME, 'login').send_keys(login)
        self.driver.find_element(By.NAME, 'passwd').send_keys(SEC_ROBOT_PWD[login])
        self.driver.find_element(By.XPATH, "//button[@type='submit']").click()

    def check_errors_absence(self, warning=False):
        ALERT_BLOCK = (By.XPATH, "//div[@class='alert alert-danger']")
        WARNING_BLOCK = (By.XPATH, "//div[@class='alert alert-warning alert-dismissible']")

        errors = self.driver.find_elements(*ALERT_BLOCK)
        warnings = self.driver.find_elements(*WARNING_BLOCK) if warning else []

        if errors or warnings:
            raise Exception(f'На странице присутствует ошибка:\n{(errors or warnings)[0].text}')


@pytest.fixture(scope='session')
def get_cookies(requests_session):
    url = BCL.statement_url_template.format(aid=1)
    with DriverWrapper() as wrap_driver:
        wrap_driver.auth_passport()
        driver_cookies_dict = {cookie['name']: cookie['value'] for cookie in wrap_driver.driver.get_cookies()}
    soup = BeautifulSoup(
        requests_session.get(url, cookies=driver_cookies_dict, verify=False).text, 'html.parser'
    )
    csrf = soup.find('input', {'name': "csrfmiddlewaretoken"})
    driver_cookies_dict.update({'csrftoken': csrf.attrs['value']})
    return driver_cookies_dict


@pytest.fixture
def read_fixture(dir_fixtures):

    def read_fixture_(filename, decode='utf-8'):
        file_path = dir_fixtures(filename)
        if ARCADIA_RUN:
            data = pkgutil.get_data(__package__, file_path)
        else:
            with open(file_path, 'rb') as f:
                data = f.read()
        if decode:
            data = data.decode(decode)
        return data

    return read_fixture_


@pytest.fixture
def dir_fixtures(dir_module):

    def dir_fixtures_(filename=None):
        path_chunks = [dir_module, 'fixtures']
        filename and path_chunks.append(filename)
        return os.path.join(*path_chunks)

    return dir_fixtures_


@pytest.fixture
def dir_module(request):
    filename = request.module.__file__
    if not ARCADIA_RUN:
        filename = os.path.abspath(filename)
    return os.path.dirname(filename)


@pytest.fixture
def export_payment_selenium(requests_session, get_cookies):
    def wrapper(aid, bcl_number):
        with DriverWrapper() as wrap_driver:
            wrap_driver.auth_passport()
            payment_url = f'{BCL.payment_url_template.format(aid=aid)}?number={bcl_number}'
            wrap_driver.driver.get(payment_url)
            wrap_driver.driver.add_cookie({'name': 'csrftoken', 'value': get_cookies['csrftoken']})

            select_button_xpath = "//button[@class='btn btn-light btn-sm buttons-select-all']"
            WebDriverWait(
                wrap_driver.driver, timeout=10).until(EC.presence_of_element_located((By.XPATH, select_button_xpath)))
            wrap_driver.driver.execute_script(
                "arguments[0].click();", wrap_driver.driver.find_element(By.XPATH, select_button_xpath)
            )

            download_button_xpath = "//button[@class='btn btn-light btn-sm buttons-selected bundle-download']"
            WebDriverWait(
                wrap_driver.driver, timeout=10).until(EC.presence_of_element_located((By.XPATH, download_button_xpath)))

            wrap_driver.driver.execute_script(
                "arguments[0].click();", wrap_driver.driver.find_element(By.XPATH, download_button_xpath)
            )
            wrap_driver.check_errors_absence()

    return wrapper


@pytest.fixture
def check_download_button(requests_session, get_cookies):
    def wrapper(aid, bundle_id):
        with DriverWrapper() as wrap_driver:
            wrap_driver.auth_passport()
            wrap_driver.driver.get(f'{BCL.bundle_url_template.format(aid=aid)}?bundle_number={bundle_id}')

            wrap_driver.driver.find_element(
                By.XPATH, f"//tr[@id='{bundle_id}']//a[@class='btn btn-light btn-sm btn-download']"
            )

    return wrapper


@pytest.fixture
def check_download_bundle(get_cookies):
    def wrapper(bundle_id):
        response = requests.get(
            f'{BCL.web_url}/get_file?n=pbundle_{bundle_id}', cookies=get_cookies, verify=False
        )
        assert response.status_code == 200

    return wrapper


@pytest.fixture
def get_soup(requests_session, get_cookies):
    def wrapper(url):
        return BeautifulSoup(
            requests_session.get(url, cookies=get_cookies, verify=False).text, 'html.parser'
        )

    return wrapper


def project_file(relative_path):
    return os.path.join(os.path.realpath(__file__).split('tests')[0], os.path.normpath(relative_path))


@pytest.fixture
def path_to_file(tmp_path):
    def wrapper(relative_path):
        return tmp_path / relative_path

    return wrapper


def prepare_secrets(cert_path, secret_data, keys_list):
    def save_secret(path, key):
        path.write_text(secret_data[key])
        return path

    return list(save_secret(cert_path / key, key) for key in keys_list)


class ReportingServerProxy(object):
    def __init__(self, url, namespace, context=None, transport=None):
        self.server = client.ServerProxy(uri=url, allow_none=1, context=context, use_datetime=1, transport=transport)
        self.namespace = namespace

    def __getattr__(self, name):
        attr = '{}.{}'.format(self.namespace, name) if self.namespace else name
        return self.server.__getattr__(attr)


class SFTPClient(object):
    def __init__(self, url, login, password=None, port=22, pkey_path=None):
        transport = paramiko.Transport((url, port))
        key = paramiko.RSAKey.from_private_key_file(project_file(pkey_path)) if pkey_path else None
        transport.connect(username=login, password=password, pkey=key)
        self.client = paramiko.SFTPClient.from_transport(transport)

    def __getattr__(self, name):
        return getattr(self.client, name)

    def upload_file(self, source_file_path, target_dir, rename_file=False):
        target_file_path = target_dir + '/' + (rename_file if rename_file else str(source_file_path).split(os.sep)[-1])

        self.client.put(str(source_file_path), target_file_path)

    def clear_dir(self, path):
        file_names = [single_file.filename for single_file in self.client.listdir_attr(path)]

        for file_name in file_names:
            self.client.remove(path + '/' + file_name)


@pytest.fixture
def bcl_test_sftp():

    def bcl_test_sftp_():
        return SFTPClient(
            url='gate-test.paysys.yandex.net',
            login=SEC_BCL_SFTP['login'],
            password=SEC_BCL_SFTP['password'],
            port=11002
        )

    return bcl_test_sftp_


@pytest.fixture
def bcl_test_sftp_paypal(yav):

    def bcl_test_sftp_paypal_():
        return SFTPClient(
            url='gate-test.paysys.yandex.net',
            login=SEC_PAYPAL_SFTP['login'],
            password=SEC_PAYPAL_SFTP['password'],
            port=11002
        )

    return bcl_test_sftp_paypal_


@pytest.fixture
def bcl_test_sftp_jp(tmp_path):

    def bcl_test_sftp_jp_():
        pkey_path = prepare_secrets(tmp_path, SEC_BCL_SFTP, ['ssh_key_jp'])[0]
        return SFTPClient(
            url='gate-test.paysys.yandex.net',
            login=SEC_BCL_SFTP['login_jp'],
            pkey_path=pkey_path,
            port=11002
        )

    return bcl_test_sftp_jp_


def get_tvm_ticket(dst_client_id, tvm_client_id, secret):
    from tvmauth import TvmClient, TvmApiClientSettings
    tvm_client = TvmClient(
        settings=TvmApiClientSettings(
            self_tvm_id=tvm_client_id,
            self_secret=secret,
            dsts=[dst_client_id],
        )
    )
    return tvm_client.get_service_ticket_for(tvm_id=dst_client_id)


class BclTvmTransport(client.Transport):
    def __init__(self, tvm_ticket, service_alias=None):
        self.service_alias = service_alias
        super().__init__()
        self.tvm_ticket = tvm_ticket

    def request(self, host, handler, data, verbose=False):
        headers = {
            "Content-Type": "text/xml",
            "Accept-Encoding": "gzip",
            "X-Ya-Service-Ticket": self.tvm_ticket
        }
        if self.service_alias:
            headers["X-BCL-SERVICE-ALIAS"] = self.service_alias
        url = "https://%s%s" % (host, handler)
        response = requests.post(url, data=data, headers=headers, verify=False)
        response.raise_for_status()
        return self.parse_response(response)

    def parse_response(self, resp):
        parser, unmarshaller = self.getparser()
        parser.feed(resp.text)
        parser.close()
        return unmarshaller.close()


@pytest.fixture
def bcl_json_api(yav):

    def bcl_json_api_(url, json=None, service_alias=None):
        headers = {
            "Content-Type": "application/json",
            "X-Ya-Service-Ticket": get_tvm_ticket(
                dst_client_id=2011732, tvm_client_id=2011732,
                secret=SEC_BCL_TEST_TVM_SURFWAX['default_value']
            )
        }
        if service_alias:
            headers['X-BCL-SERVICE-ALIAS'] = service_alias
        resp = requests.post(url=f'{BCL.web_url}/api/{url}', json=json, headers=headers, verify=False)
        return resp

    return bcl_json_api_


@pytest.fixture
def bcl_xmlrpc(yav):

    def bcl_xmlrpc_(service_alias=None):
        transport = BclTvmTransport(
            tvm_ticket=get_tvm_ticket(
                dst_client_id=2011732, tvm_client_id=2011732,
                secret=SEC_BCL_TEST_TVM_SURFWAX['default_value']
            ),
            service_alias=service_alias
        )
        return ReportingServerProxy(
            url=BCL.xmlrpc_url_tvm, namespace='Balalayka', transport=transport
        )

    return bcl_xmlrpc_


@pytest.fixture
def wait_processing_payment(bcl_xmlrpc):
    def wait_payments(transaction_ids, timeout=120, sleep_time=5, source_oebs=True):

        def get_payments():
            method_name = 'GetMultipleStatus' if source_oebs else 'GetPayments'
            paramethers = transaction_ids if source_oebs else {'transaction_ids': transaction_ids}
            return getattr(bcl_xmlrpc(), method_name)(paramethers)

        until = time.time() + timeout
        while time.time() < until:
            result = get_payments()
            try:
                assert result[0].get('status', None) not in {'processing', 'bundled', 'for_delivery'}
                return result
            except AssertionError:
                time.sleep(sleep_time)
                sleep_time += 2
        result = get_payments()
        assert result[0].get('status', None) not in {'processing', 'bundled', 'for_delivery'}
        return result

    return wait_payments


@pytest.fixture
def bcl_load_card_registry(bcl_xmlrpc):
    def load_card_registry(params):
        return bcl_xmlrpc().LoadCardRegistry(params)

    return load_card_registry


@pytest.fixture
def bcl_load_salary_registry(bcl_xmlrpc):
    def load_salary_registry(params):
        return bcl_xmlrpc().LoadSalaryRegistry(params)

    return load_salary_registry


@pytest.fixture
def bcl_receive_card_registry(bcl_xmlrpc):
    def receive_card_registry(params):
        return bcl_xmlrpc().ReceiveSalaryRegistryResponse(params)

    return receive_card_registry


@pytest.fixture
def bcl_send_payment(bcl_xmlrpc):
    def send_payment(
            transaction_id, f_acc, f_bik, t_acc, summ='2.01', currency='RUB', t_acc_type='wallet', params=None,
            service_alias=None, ground='test payment'):
        parameters = {
            'transaction_id': transaction_id,
            'f_acc': f_acc,
            'f_bik': f_bik,
            'summ': summ,
            't_acc': t_acc,
            'currency': currency,
            'ground': ground,
            't_acc_type': t_acc_type,
            'metadata': '{"client_id": 12345}'
        }
        if params:
            parameters.update({'params': params})
        result = bcl_xmlrpc(service_alias=service_alias).SendPayment(parameters)
        assert result.get('status', '') != 'error'
        return result

    return send_payment


@pytest.fixture
def bcl_probe_payment(bcl_xmlrpc):
    def probe_payment(
        transaction_id, f_acc, f_bik, t_acc, summ='2.01', currency='RUB', t_acc_type='wallet', params=None,
        ground='test payment'
    ):
        parameters = {
            'transaction_id': transaction_id,
            'f_acc': f_acc,
            'f_bik': f_bik,
            'summ': summ,
            't_acc': t_acc,
            'currency': currency,
            'ground': ground,
            't_acc_type': t_acc_type,
            'metadata': '{\'client_id\': 12345}'
        }
        if params:
            parameters.update({'params': params})
        return bcl_xmlrpc().ProbePayment(parameters)

    return probe_payment


@pytest.fixture
def bcl_get_payments(bcl_xmlrpc):
    def get_payments(transaction_ids):
        return bcl_xmlrpc().GetPayments({'transaction_ids': transaction_ids})

    return get_payments


@pytest.fixture
def bcl_get_multiple_status(bcl_xmlrpc):
    def get_payments(transaction_ids):
        return bcl_xmlrpc().GetMultipleStatus(transaction_ids)

    return get_payments


@pytest.fixture
def bcl_set_pd(bcl_xmlrpc):
    def send_payment(
            num_src, f_acc, t_acc, f_bic='', f_bik='', num_oebs='111', summ=17.0, f_inn='7736207543',
            t_inn='7725713770', t_name='test_name', currency='RUB', priority='5', t_addr='Moscow Yandex', n_period='',
            con_curr='', con_dt='', i_bic='', purp='', n_status='', n_ddate='', n_ground='', con_num='', cur_op='',
            n_kod='', con_sum=0, paid_by='', tr_dt='', n_dnum='', n_type='', t_bic='INGBNL2A', t_bik='', t_cacc='',
            opertype=1, f_cacc='', t_bnk='ING Bank N.V', t_kpp='772501001', n_kbk='', com_acc='', n_okato='',
            f_name='Yandex', f_bnk='', f_kpp='997750001', tr_pass='', t_bnk_city='', t_country='', t_iban='', f_iban='',
            payout_type=None, service_alias=None, login='TESTUSER-BALANCE3', autobuild=None, t_first_name=None,
            t_last_name=None, t_middle_name=None, t_ogrn=None
    ):
        parameters = {
            'number': num_oebs,
            'ground': purp,
            'summ': summ,
            'currency': currency,
            'ic_id': num_src,
            'docdate': datetime.datetime.now().strftime('%Y-%m-%d'),

            'i_swiftcode': i_bic,
            'paytype': 1,
            'oper_code': cur_op,
            'paid_by': paid_by,
            'ocherednostplatezha': priority,
            'sod_id': '',
            'index': '1',
            'sendtype': 'Электронно',
            'opertype': opertype,
            'write_off_account': com_acc,
            'login': login,

            'f_acc': f_acc,
            'f_inn': f_inn,
            'f_cacc': f_cacc,
            'f_name': f_name,
            'f_bankname': f_bnk,
            'f_swiftcode': f_bic,
            'f_kpp': f_kpp,
            'f_bic': f_bik,
            'f_iban': f_iban,

            't_acc': t_acc,
            't_name': t_name,
            't_inn': t_inn,
            't_address': t_addr,
            't_swiftcode': t_bic,
            't_bic': t_bik,
            't_cacc': t_cacc,
            't_bankname': t_bnk,
            't_kpp': t_kpp,
            't_bank_city': t_bnk_city,
            't_country': t_country,
            't_iban': t_iban,

            'n_pokazatelnalperioda': n_period,
            'n_pokazateldatydocumenta': n_ddate,
            'n_stat1256': n_status,
            'n_pokazatelosnplatezha': n_ground,
            'n_kod': n_kod,
            'n_pokazatelnomerdocumenta': n_dnum,
            'n_pokazateltipaplatezha': n_type,
            'n_kbk': n_kbk,
            'n_okato': n_okato,

            'contract_curr': con_curr,
            'contract_dt': con_dt,
            'contract_num': con_num,
            'contract_sum': con_sum,
            'trans_pass': tr_pass,
        }
        if tr_dt:
            parameters.update({'expected_dt': tr_dt})
        if payout_type:
            parameters.update({'payout_type': payout_type})
        if autobuild:
            parameters.update({'autobuild': autobuild})
        if t_first_name:
            parameters.update(
                {'t_first_name': t_first_name, 't_last_name': t_last_name, 't_middle_name': t_middle_name}
            )
        if t_ogrn:
            parameters.update({'t_ogrn': t_ogrn})
        return bcl_xmlrpc(service_alias=service_alias).SetPD(parameters)

    return send_payment


@pytest.fixture
def bcl_get_account_info(bcl_xmlrpc):
    def wrapper(number):
        return bcl_xmlrpc().GetAccountInfo({'number': number})

    return wrapper


@pytest.fixture
def bcl_cancel_payments(bcl_xmlrpc):
    def cancel_payments(transaction_ids):
        return bcl_xmlrpc().CancelPayments(transaction_ids)

    return cancel_payments


@pytest.fixture
def bcl_payoneer_get_login_link(bcl_xmlrpc):
    def payoneer_get_login_link(program_id, payee_id):
        return bcl_xmlrpc(service_alias='market').PayoneerGetLoginLink({
            'program_id': program_id,
            'payee_id': payee_id,
        })

    return payoneer_get_login_link


@pytest.fixture
def bcl_payoneer_get_payee_status(bcl_xmlrpc):
    def payoneer_get_payee_status(program_id, payee_id):
        return bcl_xmlrpc(service_alias='market').PayoneerGetPayeeStatus({
            'program_id': program_id,
            'payee_id': payee_id,
        })

    return payoneer_get_payee_status


@pytest.fixture
def bcl_pingpong_get_login_link(bcl_xmlrpc):
    def pingpong_get_login_link(payee_id, currency, country, store_name, store_url, notify_url):
        return bcl_xmlrpc(service_alias='market').PingPongGetOnboardingLink({
            'seller_id': payee_id,
            'currency': currency,
            'country': country,
            'store_name': store_name,
            'store_url': store_url,
            'notify_url': notify_url
        })

    return pingpong_get_login_link


@pytest.fixture
def bcl_pingpong_get_payee_status(bcl_xmlrpc):
    def pingpong_get_payee_status(seller_id):
        return bcl_xmlrpc(service_alias='market').PingPongGetSellerStatus({
            'seller_id': seller_id
        })

    return pingpong_get_payee_status


@pytest.fixture
def bcl_qiwi_get_config(bcl_xmlrpc):
    def qiwi_get_config(shop_id):
        return bcl_xmlrpc(service_alias='toloka').QiwiGetConfig({
            'shop_id': shop_id,
        })

    return qiwi_get_config


@pytest.fixture
def bcl_qiwi_payment_calculation(bcl_xmlrpc):
    def qiwi_payment_calculation(shop_id, shop_currency, payway, amount, amount_type):
        return bcl_xmlrpc(service_alias='toloka').QiwiPaymentCalculation({
            'shop_id': shop_id,
            'shop_currency': shop_currency,
            'payway': payway,
            'amount': amount,
            'amount_type': amount_type,
        })

    return qiwi_payment_calculation


@pytest.fixture
def bcl_get_creditors(bcl_xmlrpc):
    def get_creditors(bik, name=None, inn=None, ogrn=None, statuses=None):
        if statuses:
            params = {
                'statuses': statuses,
                'page': 0
            }
        else:
            params = {
                'name': name,
                'inn': inn,
                'ogrn': ogrn
            }
        params.update({'bik': bik})

        return bcl_xmlrpc().GetCreditors({**params})

    return get_creditors


@pytest.fixture
def bcl_send_creditor(bcl_xmlrpc):
    def send_creditor(bik, name, ogrn, inn):
        return bcl_xmlrpc(service_alias='toloka').SendCreditorToCheck({
            'bik': bik,
            'name': name,
            'ogrn': ogrn,
            'inn': inn,
        })

    return send_creditor


@pytest.fixture
def bcl_get_fps_banks(bcl_xmlrpc):
    def get_fps_banks(account):
        return bcl_xmlrpc(service_alias='toloka').FPSBankList({
            'acc_num': account,
        })

    return get_fps_banks


@pytest.fixture
def bcl_load_card_registry(bcl_xmlrpc):
    def load_card_registry(reg_data):
        return bcl_xmlrpc().LoadCardRegistry(reg_data)

    return load_card_registry


@pytest.fixture
def bcl_load_salary_registry(bcl_xmlrpc):
    def load_salary_registry(reg_data):
        return bcl_xmlrpc().LoadSalaryRegistry(reg_data)

    return load_salary_registry


@pytest.fixture
def get_payment_id_by_number(get_soup):
    def wrapper(associate_id, number):
        from_dt = datetime.datetime.now().strftime('%d-%m-%Y')

        soup = get_soup(f'{BCL.payment_url_template.format(aid=associate_id)}?number={number}&dt_from={from_dt}')
        return soup.find('td', text=number).parent['id']

    return wrapper


@pytest.fixture
def export_payments(get_cookies):
    def wrapper(payment_ids, associate_id, method='download', status_code=200):
        data = [
            ('action', method), ('table_request', '1'),
            ('items', json.dumps(payment_ids)), ('realm', 'table-payments'),
            ('associate_id', str(associate_id)),
            ('csrfmiddlewaretoken', get_cookies['csrftoken'])
        ]
        response = requests.post(
            BCL.payment_url_template.format(aid=associate_id), cookies=get_cookies, data=data, verify=False
        )
        assert response.status_code == status_code
        return response

    return wrapper


@pytest.fixture
def export_h2h_and_wait_processing(
        get_cookies, bcl_set_pd, export_payments, wait_processing_payment, get_payment_id_by_number, edit_payment_data):
    def wrapper(associate_id, set_pd_parameters, payment_count=1, method='download', edit_params=None):
        result = []
        for _ in range(payment_count):
            transaction_id = str(uuid.uuid4()).upper()
            bcl_number = bcl_set_pd(transaction_id, **set_pd_parameters)
            payment_id = get_payment_id_by_number(associate_id, bcl_number)
            result.append((transaction_id, bcl_number, payment_id))
            if edit_params:
                edit_payment_data(payment_id, associate_id, edit_params)

        export_payments(
            payment_ids=[pay_id for _, _, pay_id in result],
            associate_id=associate_id, method=method
        )
        wait_processing_payment([result[0][0]])
        return result

    return wrapper


@pytest.fixture
def edit_payment_data(get_cookies):
    def wrapper(payment_id, associate_id, field_list):
        data = [
                   ('p_id', payment_id),
                   ('csrfmiddlewaretoken', get_cookies['csrftoken'])
               ] + field_list

        response = requests.post(
            f'{BCL.payment_url_template.format(aid=str(associate_id))}{payment_id}/edit/',
            cookies=get_cookies, data=data, verify=False)

        assert response.status_code == 200

    return wrapper


@pytest.fixture
def get_bundle_id(get_soup):
    def wrapper(payment_number, associate_id):
        from_dt = datetime.datetime.now().strftime('%d-%m-%Y')
        url = (
            f'{BCL.payment_url_template.format(aid=associate_id)}?'
            'status=4&status=10&status=7&status=6&status=104&status=0&status=103&status=5&status=2&'
            'status=12&status=11&status=1&'
            f'number={payment_number}&dt_from={from_dt}'
        )
        soup = get_soup(url)
        bundle_id = soup.find('td', text=payment_number).parent.findAll('td')[2].find('a')['href'].split('=')[-1]
        return bundle_id

    return wrapper


@pytest.fixture
def get_bundle_status(get_soup):
    def wrapper(bundle_id, associate_id):
        soup = get_soup(f'{BCL.bundle_url_template.format(aid=associate_id)}?bundle_id={bundle_id}')
        bundle_status = soup.find('tr', id=bundle_id).findAll('td')[4].text
        return bundle_status

    return wrapper


@pytest.fixture
def run_force_task(get_cookies):
    def wrapper(task_name, status_code=200):
        response = requests.get(
            f'{BCL.web_url}/rerun?proc=force_task&name={task_name}',
            cookies=get_cookies, verify=False)
        assert response.status_code == status_code
        return response

    return wrapper


@pytest.fixture
def run_force_task_with_retries(get_cookies):
    def wrapper(task_name, retries=3, session=None, status_code=200):
        session = session or requests.Session()
        retry = Retry(
            total=retries,
            read=retries,
            connect=retries,
            backoff_factor=0.3,
            status_forcelist=(500, 502, 504),
        )
        adapter = HTTPAdapter(max_retries=retry)
        session.mount('https://', adapter)
        response = session.get(
            f'{BCL.web_url}/rerun?proc=force_task&name={task_name}',
            cookies=get_cookies, verify=False)
        assert response.status_code == status_code
        return response

    return wrapper


@pytest.fixture
def rerun_task(get_cookies):
    def wrapper(aid, proc_name='automate_statements', on_date=None, additional_params='', status_code=200):
        response = requests.get(
            f'{BCL.web_url}/rerun?assoc={aid}&proc={proc_name}%s{additional_params}' % (
                ('&on_date=' + on_date.strftime('%Y-%m-%d') if on_date else ''),
            ),
            cookies=get_cookies, verify=False)
        assert response.status_code == status_code
        return response

    return wrapper


@pytest.fixture
def statement_start_amount(get_soup):
    def wrapper(account):
        soup = get_soup(f'{BCL.balance_report_page}?number={account}')
        opening_balance = Decimal(
            soup.find('td', text=account).parent.findAll('td')[5].text.replace(',', '').replace(u'\xa0', '')
        )/100
        return opening_balance

    return wrapper


@pytest.fixture
def get_last_uploaded_statement(get_soup):
    def wrapper(aid, account_id=None):
        time.sleep(5)
        soup = get_soup(
            '%s?dt_from=%s&acc=%s' % (
                BCL.statement_url_template.format(aid=aid), datetime.datetime.now().strftime('%d-%m-%Y'),
                account_id or ''
            )
        )
        try:
            statement_number = soup.find('td').parent.findAll('td')[3].text.strip()
        except AttributeError:
            raise Exception(
                'Проверьте, пожалуйста, наличие права Поддержка в IDM у пользователей testuser-balance3 и robot-bcl'
            )
        return statement_number

    return wrapper


@pytest.fixture
def get_statement_data(get_soup):
    def wrapper(aid, statement_number):
        soup = get_soup('%s?number=%s' % (BCL.statement_url_template.format(aid=aid), statement_number))
        return soup.find(lambda tag: (tag.name == 'a' or tag.name == 'td') and statement_number in tag.text)

    return wrapper


@pytest.fixture
def get_satetment_id(get_cookies, get_statement_data):
    def wrapper(aid, statement_number):
        return get_statement_data(aid, statement_number).parent['id']

    return wrapper


@pytest.fixture
def get_statement_status(get_cookies, get_statement_data):
    def wrapper(aid, statement_number):
        statement_data = get_statement_data(aid, statement_number)
        if statement_data:
            return statement_data.parent.findAll('td')[4].text.strip()
        return None

    return wrapper


@pytest.fixture
def wait_processing_statement(get_statement_status, get_last_uploaded_statement):
    def wait_statement(aid, account_id=None, statement_number=None, timeout=120, sleep_time=2):
        statement_number = statement_number or get_last_uploaded_statement(aid, account_id)

        def assert_all(check_result):
            assert check_result is not None
            assert 'Обрабатывается' not in check_result
            assert 'Ожидает обработки' not in check_result
            assert 'К обработке' not in check_result

        until = time.time() + timeout
        while time.time() < until:
            result = get_statement_status(aid, statement_number)
            try:
                assert_all(result)
                return result
            except AssertionError:
                time.sleep(sleep_time)
                sleep_time += 2
        result = get_statement_status(aid, statement_number)
        assert_all(result)
        return result

    return wait_statement


@pytest.fixture
def get_statement(bcl_xmlrpc):
    def wrapper(account, on_date, is_intraday=False):
        return bcl_xmlrpc().GetStatement(account, on_date, is_intraday)

    return wrapper


@pytest.fixture
def delete_statement(get_cookies, get_satetment_id):
    def wrapper(aid, statement_number):
        resp = requests.post(
            BCL.statement_url_template.format(aid=aid),
            data=[
                ('action', 'delete'),
                ('table_request', '1'),
                ('items', '["{}"]'.format(get_satetment_id(aid, statement_number))),
                ('realm', 'table-statements'),
                ('csrfmiddlewaretoken', get_cookies['csrftoken'])
            ],
            cookies=get_cookies, verify=False
        )
        assert resp.status_code == 200

    return wrapper


@pytest.fixture
def upload_statement(get_cookies, get_satetment_id):
    def wrapper(aid, files: List[str]):
        files = {'statement_file': open(files[0], 'rb')} if len(files) == 1 else list(
                map(lambda file_path: ('statement_file', open(
                    file_path, 'rb')), files))
        resp = requests.post(
            BCL.statement_url_template.format(aid=aid),
            files=files,
            data=[
                ('csrfmiddlewaretoken', get_cookies['csrftoken'])
            ],
            cookies=get_cookies, verify=False
        )
        assert resp.status_code == 200

    return wrapper


@pytest.fixture
def get_all_uploaded_statements(get_soup):
    def wrapper(aid, account, on_date=None, is_intraday=False):
        on_date = datetime.datetime.now() or on_date
        soup = get_soup(
            '%s?dt_from=%s&acc=%s&type=%s' % (
                BCL.statement_url_template.format(aid=aid), on_date.strftime('%d-%m-%Y'), account,
                2 if is_intraday else 1
            )
        )
        table_line = soup.find('td')
        if table_line:
            table = table_line.parent.parent.findAll('tr')
            return [line.findAll('td')[3].text.strip() for line in table]
        return []

    return wrapper


@pytest.fixture
def wait_condition_and_do_something():
    def wrapper(condition, run_func, time_sleep=5):
        until = time.time() + 60
        result = run_func()
        while condition(result) and until > time.time():
            time.sleep(time_sleep)
            time_sleep += 2
            result = run_func()
        assert not condition(result)
        return result

    return wrapper


@pytest.fixture
def wait_condition_and_run_task(run_force_task):
    def wrapper(condition, task, time_sleep=5):
        until = time.time() + 60
        while condition() and until > time.time():
            time.sleep(time_sleep)
            time_sleep += 2
            run_force_task(task)

    return wrapper


@pytest.fixture
def apply_filters_payment(requests_session, get_cookies):
    def wrapper(aid, dynamic_elements, input_fields, pay_sum, pay_dt):
        with DriverWrapper() as wrap_driver:
            wrap_driver.auth_passport()
            wrap_driver.driver.get(BCL.payment_url_template.format(aid=aid))

            for option in dynamic_elements:
                option = wrap_driver.driver.find_element(
                    By.XPATH, f'//select[@id="{option}"]//option[@value="{dynamic_elements[option]}"]'
                )
                option.click()

            for field in input_fields:
                wrap_driver.driver.find_element(By.XPATH, f'//input[@id="{field}"]').send_keys(input_fields[field])

            date_from_element = wrap_driver.driver.find_element(By.XPATH, '//input[@id="dt_from"]')
            date_from_element.clear()
            date_from_element.send_keys(pay_dt.strftime('%d-%m-%Y'))

            wrap_driver.driver.find_element(
                By.XPATH, '//input[@id="dt_till"]'
            ).send_keys((pay_dt + datetime.timedelta(1)).strftime('%d-%m-%Y'))

            wrap_driver.driver.find_element(By.XPATH, '//input[@id="sum_from"]').send_keys(pay_sum - 1)

            wrap_driver.driver.find_element(By.XPATH, '//input[@id="sum_to"]').send_keys(pay_sum + 1)

            wrap_driver.driver.find_element(
                By.XPATH, '//form[@class="form-inline"]//button[@type="submit"]'
            ).submit()

            wrap_driver.check_errors_absence()

            return wrap_driver.driver.find_elements(
                By.CSS_SELECTOR, 'tr[class*="payment-row selectable"]'
            )

    return wrapper


@pytest.fixture
def apply_filters_bundle(requests_session, get_cookies):
    def wrapper(aid, dynamic_elements, input_fields, bundle_dt):
        with DriverWrapper() as wrap_driver:
            wrap_driver.auth_passport()
            wrap_driver.driver.get(BCL.bundle_url_template.format(aid=aid))

            for option in dynamic_elements:
                option = wrap_driver.driver.find_element(
                    By.XPATH, f'//select[@id="{option}"]//option[@value="{dynamic_elements[option]}"]'
                )
                option.click()

            for field in input_fields:
                wrap_driver.driver.find_element(By.XPATH, f'//input[@id="{field}"]').send_keys(input_fields[field])

            date_from_element = wrap_driver.driver.find_element(By.XPATH, '//input[@id="dt_from"]')
            date_from_element.clear()
            date_from_element.send_keys(bundle_dt.strftime('%d-%m-%Y'))

            wrap_driver.driver.find_element(
                By.XPATH, '//input[@id="dt_till"]'
            ).send_keys((bundle_dt + datetime.timedelta(1)).strftime('%d-%m-%Y'))

            wrap_driver.driver.find_element(
                By.XPATH, '//button[@type="submit"]'
            ).submit()

            wrap_driver.check_errors_absence()

            return wrap_driver.driver.find_elements(
                By.XPATH, '//table[@id="table-bundles"]//tbody/tr[@role="row"]'
            )

    return wrapper


@pytest.fixture
def bcl_paypal_info_from_token(bcl_xmlrpc):
    def wrapper(token):
        return bcl_xmlrpc().PayPalUserInfoFromToken(token)

    return wrapper


@pytest.fixture(scope='session')
def get_paypal_auth_data(yav):
    result = SEC_PAYPAL_AUTH
    return result['client_id'], result['secret']


@pytest.fixture
def sber():
    return {'id': 9, 'bid': '044525225', 'alias': 'sber'}


@pytest.fixture
def tinkoff():
    return {'id': 25, 'bid': '044525974', 'alias': 'tinkoff'}


@pytest.fixture
def ing_nl():
    return {'id': 18}


@pytest.fixture
def jpmorgan():
    return {'id': 23, 'bid': 'CHASLULX'}


@pytest.fixture
def paypal():
    return {'id': 6}


@pytest.fixture
def raiff():
    return {'id': 8, 'bid': '044525700'}


@pytest.fixture
def unicredit():
    return {'id': 10, 'bid': '044525545'}
