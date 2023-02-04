# -*- coding: utf-8 -*-
import mock
import pytest
import datetime
import ssl
import StringIO
import sqlalchemy as sa
from decimal import Decimal
import hamcrest as hm
import balance.muzzle_util as ut
from cluster_tools.currency_rate import CBR, ECB, TCMB, NBU, BOC, CBA, NBRB, NBKZ, NBGE, BNM, NBKR, \
    CBU, NBS, NBT, BOM, CBAR, CNB, NBP, BOI, NBE, CBN, BCEAO, SARB, CBK, BNR, MNB, BNB, BOE, OANDA

TODAY = ut.trunc_date(datetime.datetime.now())
YESTERDAY = TODAY - datetime.timedelta(days=1)
TOMORROW = TODAY + datetime.timedelta(days=1)

RATE_FROM = 'rate_from'
RATE_TO = 'rate_to'

ssl.SSLContext = mock.MagicMock()  # на агентах python 2.7.6, оно же появилось с 2.7.9. Используется в NbgeTest


class CurrencyRateSourceTest(object):
    """ CurrencyRateSource mixin for tests """
    base_currency_units = Decimal('1')

    def __init__(self, session):
        self._session = session

    @property
    def session(self):
        return self._session

    @property
    def requested_currencies(self):
        return {'USD'}


class Response(object):
    def __init__(self, content=None, text=None, json_text=None, url=None):
        self.content = content
        self.text = text
        self.json_text = json_text
        self.url = url

    def json(self):
        return self.json_text


CBR_ANSWER = '''\
<?xml version="1.0" encoding="windows-1251"?>
<ValCurs Date="23.06.2018" name="Foreign Currency Market">
    <Valute ID="R01010">
        <NumCode>036</NumCode>
        <CharCode>USD</CharCode>
        <Nominal>2</Nominal>
        <Name>valute_name</Name>
        <Value>1,234</Value>
    </Valute>
</ValCurs>
'''


class CbrTest(CurrencyRateSourceTest, CBR):
    rate_dt = TOMORROW
    rate_type = RATE_TO
    base_currency_units = Decimal('2')
    is_with_mid_rate = True
    url = 'http://www.cbr.ru/scripts/XML_daily.asp?date_req={timestamp}'.format(timestamp=rate_dt.strftime("%d.%m.%Y"))

    @classmethod
    def urlopen_and_log(cls, url, encoding=None, *args, **kwargs):
        assert url == cls.url
        return StringIO.StringIO(CBR_ANSWER)


class EcbTest(CurrencyRateSourceTest, ECB):
    rate_dt = TODAY
    rate_type = RATE_FROM
    is_with_mid_rate = True
    url = 'https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml'

    @classmethod
    def urlopen_and_log(cls, url, encoding=None, *args, **kwargs):
        assert url == cls.url
        buf = '''<?xml version="1.0" encoding="UTF-8"?>
            <Cube>
                <Cube time=\'2018-06-25\'>
                    <Cube currency=\'USD\' rate=\'1.234\'/>
                </Cube>
            </Cube>'''
        return StringIO.StringIO(buf)


class TcmbTest(CurrencyRateSourceTest, TCMB):
    rate_dt = TODAY
    rate_type = RATE_TO
    is_with_sell_rate = True
    is_with_buy_rate = True
    url = 'http://tcmb.gov.tr/kurlar/today.xml'

    @classmethod
    def urlopen_and_log(cls, url, encoding=None, *args, **kwargs):
        assert url == cls.url
        buf = '''<?xml version="1.0" encoding="UTF-8"?><?xml-stylesheet type="text/xsl" href="isokur.xsl"?>
        <Tarih_Date Tarih="25.06.2018" Date="06/25/2018"  Bulten_No="2018/121" >
            <Currency CrossOrder="9" Kod="EUR" CurrencyCode="USD">
                <Unit>1</Unit>
                <Isim>EURO</Isim>
                <CurrencyName>EURO</CurrencyName>
                <ForexBuying>3.45678</ForexBuying>
                <ForexSelling>2.34</ForexSelling>
                <BanknoteBuying>5.4033</BanknoteBuying>
                <BanknoteSelling>5.4250</BanknoteSelling>
                <CrossRateUSD/>
                <CrossRateOther>1.1654</CrossRateOther>
            </Currency>
        </Tarih_Date>'''
        return StringIO.StringIO(buf)


class NbuTest(CurrencyRateSourceTest, NBU):
    rate_dt = TODAY
    rate_type = RATE_TO
    is_with_mid_rate = True
    url = 'http://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?valcode=USD&date={timestamp}'.format(
        timestamp=rate_dt.strftime("%Y%m%d"))

    @classmethod
    def urlopen_and_log(cls, url, encoding=None, *args, **kwargs):
        assert url == cls.url
        buf = '''<?xml version="1.0" encoding="UTF-8"?>
                <exchange>
                    <currency>
                        <r030>682</r030>
                        <txt>Саудівський рiял</txt>
                        <rate>1.234</rate>
                        <cc>USD</cc>
                        <exchangedate>25.06.2018</exchangedate>
                    </currency>
                </exchange>'''
        return StringIO.StringIO(buf)


class BocTest(CurrencyRateSourceTest, BOC):
    rate_dt = TODAY.replace(hour=10)
    rate_type = RATE_TO
    is_with_mid_rate = True
    url = 'https://srh.bankofchina.com/search/whpj/searchen.jsp'

    @property
    def currency_value_map(self):
        return {'USD': '1336'}

    @property
    def available_currencies(self):
        return {'USD'}

    @classmethod
    def request_and_log(cls, method, url, **kwargs):
        assert url == cls.url
        text = '''<html>
        <head></head>
            <body><table></table>
            <table>
            <tr></tr>
            <tr>
            <td>USD</td>
            <td>652.57</td>
            <td>647.26</td>
            <td>655.34</td>
            <td>655.34</td>
            <td>123.4</td>
            <td>{timestamp}</td>
            </tr>
            </table>
            </body>
        </html>'''.format(timestamp=cls.rate_dt.strftime("%Y.%m.%d %H:%M:%S"))
        return Response(text=text)


class CbaTest(CBA):
    rate_type = RATE_FROM
    pass


class NbrbTest(CurrencyRateSourceTest, NBRB):
    rate_dt = TODAY
    rate_type = RATE_TO
    is_with_mid_rate = True
    url = 'https://www.nbrb.by/API/ExRates/Rates?onDate={timestamp}&Periodicity=0'.format(
        timestamp=rate_dt.strftime("%Y-%m-%d"))

    @classmethod
    def urlopen_and_log(cls, url, encoding=None, *args, **kwargs):
        assert url == cls.url
        buf = '[{"Cur_ID":170,"Date":"2018-06-25T00:00:00","Cur_Abbreviation":"USD","Cur_Scale":1,"Cur_Name":"Австралийский доллар","Cur_OfficialRate":1.234}]'
        return StringIO.StringIO(buf)


class NbkzTest(CurrencyRateSourceTest, NBKZ):
    rate_dt = TODAY
    rate_type = RATE_TO
    is_with_mid_rate = True
    url = 'http://www.nationalbank.kz/rss/rates_all.xml'

    @classmethod
    def urlopen_and_log(cls, url, encoding=None, *args, **kwargs):
        assert url == cls.url
        buf = '''<?xml version="1.0" encoding="utf-8"?>
        <rates>
            <item>
                <title>USD</title>
                <pubDate>{timestamp}</pubDate>
                <description>1.234</description>
                <quant>1</quant>
                <index>DOWN</index>
                <change>-0.78</change>
                <link />
            </item>
        </rates>'''.format(timestamp=cls.rate_dt.strftime("%d.%m.%y"))
        return StringIO.StringIO(buf)


class NbgeTest(CurrencyRateSourceTest, NBGE):
    rate_dt = TOMORROW
    rate_type = RATE_TO
    is_with_mid_rate = True
    url = 'https://old.nbg.gov.ge/index.php?m=582&lng=eng'

    @classmethod
    def urlopen_and_log(cls, url, encoding=None, *args, **kwargs):
        assert url == cls.url
        buf = '''
        <html><head></head>
            <body>
                <div id="currency_id">
                <table>
                <tr>
                <td>USD</td>
                <td >1 Euro</td>
                <td>1.234</td>
                <td></td>
                <td>0.0036</td>
                </tr>
                </table>
                </div>
            </body>
        </html>'''
        return StringIO.StringIO(buf)


class BnmTest(CurrencyRateSourceTest, BNM):
    rate_dt = TOMORROW
    rate_type = RATE_TO
    base_currency_units = Decimal('2')
    is_with_mid_rate = True
    url = 'http://www.bnm.md/ru/official_exchange_rates?get_xml=1&date={timestamp}'.format(
        timestamp=rate_dt.strftime("%d.%m.%Y"))

    @classmethod
    def urlopen_and_log(cls, url, encoding=None, *args, **kwargs):
        assert url == cls.url
        return StringIO.StringIO(CBR_ANSWER)


class NbkrTest(CurrencyRateSourceTest, NBKR):
    rate_dt = TODAY
    rate_type = RATE_TO
    is_with_mid_rate = True
    url = 'http://www.nbkr.kg/XML/daily.xml'

    @classmethod
    def urlopen_and_log(cls, url, encoding=None, *args, **kwargs):
        assert url == cls.url
        buf = '''<?xml version="1.0" encoding="windows-1251" ?>
        <CurrencyRates Name="Daily Exchange Rates" Date="{timestamp}">
            <Currency ISOCode="USD">
                <Nominal>1</Nominal>
                <Value>1,234</Value>
            </Currency>
        </CurrencyRates>'''.format(timestamp=cls.rate_dt.strftime("%d.%m.%Y"))
        return StringIO.StringIO(buf)


class CbuTest(CurrencyRateSourceTest, CBU):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_mid_rate = True
    url = 'http://cbu.uz/ru/arkhiv-kursov-valyut/xml/'

    @classmethod
    def urlopen_and_log(cls, url, encoding=None, *args, **kwargs):
        assert url == cls.url
        buf = '''<?xml version="1.0" encoding="UTF-8"?><CBU_Curr name="CBU Currency XML by ISO 4217">
        <CcyNtry ID="784">
        <Ccy>USD</Ccy>
            <CcyNm_RU>Дирхам ОАЭ</CcyNm_RU>
            <CcyNm_UZ>BAA dirhami</CcyNm_UZ>
            <CcyNm_UZC>БАА дирҳами</CcyNm_UZC>
            <CcyNm_EN>UAE Dirham</CcyNm_EN>
            <CcyMnrUnts>2</CcyMnrUnts>
            <Nominal>1</Nominal>
            <Rate>1.234</Rate>
            <date>19.06.2018</date>
        </CcyNtry>
            </CBU_Curr>
        '''
        return StringIO.StringIO(buf)


class NbsTest(CurrencyRateSourceTest, NBS):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_mid_rate = True
    url = 'https://www.nbs.rs/kursnaListaModul/srednjiKurs.faces'

    @classmethod
    def urlopen_and_log(cls, url, encoding=None, *args, **kwargs):
        assert url == cls.url
        buf = '''
        <html:html xmlns:html="http://www.w3.org/1999/xhtml">
<html:body>
<html:form action="/kursnaListaModul/srednjiKurs.faces;jsessionid=CBF61E672F25FC9B120AE9ABEE4E7136" enctype="application/x-www-form-urlencoded" id="index" method="post" name="index">
<html:input name="index" type="hidden" value="index" />
<html:table class="tableHeader" id="index:grid">
<html:tbody>
</html:tbody>
</html:table>
<html:br />
				<html:div><html:table cellspacing="0" class="table table-bordered table-striped table-hover  indexsrednjiKursListaTable" id="index:srednjiKursLista" style="width: 100%;">
                    <html:caption />
                                <html:tbody>
                                <html:tr><html:td>USD</html:td><html:td>978</html:td><html:td>EMU</html:td><html:td>1</html:td><html:td>1.234</html:td></html:tr>
</html:tbody></html:table>
					</html:div>
				<html:br /><html:div class="row" id="index:j_idt36"><html:div class="col-md-12" id="index:j_idt37">

						<html:table width="100%">
                            <html:tr>
								<html:td class="form_napomena"><html:table class="tableCell" id="index:spisakNapome" width="100%">
<html:tbody>
<html:tr><html:td /></html:tr></html:tbody>
</html:table>
</html:td>
							</html:tr>
						</html:table></html:div></html:div>
				<html:br />
				<html:br />
                <html:input autocomplete="off" id="j_id1:javax.faces.ViewState:0" name="javax.faces.ViewState" type="hidden" value="-906900337824124865:4420109065245606526" />
</html:form>
</html:body>
</html:html>'''.format(timestamp=cls.rate_dt.strftime('%d/%m/%Y'))
        return StringIO.StringIO(buf)


class NbtTest(CurrencyRateSourceTest, NBT):
    rate_type = RATE_TO
    rate_dt = TOMORROW
    is_with_mid_rate = True
    url = 'https://www.nbt.tj/en/kurs/export_csv.php'.format(
        timestamp=rate_dt.strftime('%Y-%m-%d'))

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding == cls.url
        text = '''DATE; {timestamp}
840; USD; 1; 1.234'''.format(timestamp=cls.rate_dt.strftime('%Y-%m-%d'))
        return Response(text=text)


class BomTest(CurrencyRateSourceTest, BOM):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_mid_rate = True
    url = 'https://www.mongolbank.mn/eng/dblistofficialdailyrate.aspx'

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding == cls.url
        text = '''<!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <body><ul class="uk-comment-list">
                    <li>
                       <table>
                            <tr>
                               <td></td>
                               <td>USD</td>
                               <td>
                                <span>1.234</span>
                                </td>
                            </tr>
                        </table>
                        </li>
                    </body>
                </html>'''
        return Response(text=text)


class CbarTest(CurrencyRateSourceTest, CBAR):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_mid_rate = True
    url = 'https://cbar.az/currencies/{timestamp}.xml'.format(timestamp=rate_dt.strftime("%d.%m.%Y"))

    @classmethod
    def urlopen_and_log(cls, url, encoding=None, *args, **kwargs):
        assert url == cls.url
        buf = '''<?xml version="1.0" encoding="UTF-8"?>
                <ValCurs Date="25.06.2018" Name="AZN məzənnələri">
                    <ValType Type="Xarici valyutalar">
                        <Valute Code="USD">
                            <Nominal>1</Nominal>
                            <Name>Çexiya kronu</Name>
                            <Value>1.234</Value>
                        </Valute>
                </ValType></ValCurs>'''
        return StringIO.StringIO(buf)


class CnbTest(CurrencyRateSourceTest, CNB):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_mid_rate = True
    url = 'http://www.cnb.cz/en/financial_markets/foreign_exchange_market/exchange_rate_fixing/daily.txt'

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding == cls.url
        text = '''
        Country|Currency|Amount|Code|Rate
        Australia|dollar|1|USD|1.234'''
        return Response(text=text)


class NbpTest(CurrencyRateSourceTest, NBP):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_mid_rate = True
    base_currency_units = Decimal('2')
    url1 = 'http://www.nbp.pl/kursy/xml/LastA.xml'
    url2 = 'http://www.nbp.pl/kursy/xml/LastB.xml'

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding in (cls.url1, cls.url2)
        content = '''<tabela_kursow typ="B" uid="18b025">
                        <numer_tabeli>025/B/NBP/2018</numer_tabeli>
                   <data_publikacji>2018-06-20</data_publikacji>
                   <pozycja>
                      <nazwa_waluty>afgani (Afganistan)</nazwa_waluty>
                      <przelicznik>2</przelicznik>
                      <kod_waluty>USD</kod_waluty>
                      <kurs_sredni>1,234</kurs_sredni>
                   </pozycja>
        </tabela_kursow>'''
        return Response(content=content)


class BoiTest(CurrencyRateSourceTest, BOI):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_mid_rate = True
    url = 'https://www.boi.org.il/currency.xml'

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding == cls.url
        content = '''<CURRENCIES>
          <LAST_UPDATE>2018-06-25</LAST_UPDATE>
          <CURRENCY>
            <NAME>Dollar</NAME>
            <UNIT>1</UNIT>
            <CURRENCYCODE>USD</CURRENCYCODE>
            <COUNTRY>USA</COUNTRY>
            <RATE>1.234</RATE>
            <CHANGE>-0.028</CHANGE>
          </CURRENCY>
  </CURRENCIES>'''
        return Response(content=content)


class NbeTest(CurrencyRateSourceTest, NBE):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_sell_rate = True
    is_with_buy_rate = True
    url = 'https://market.nbebank.com/market/dollarbirr/index.php'

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding == cls.url
        content = '''<html>
        <head></head>
        <body>
        <table>
        <tr><table>
        </table>
        </tr>
        <table>
        <tr>
        <td></td>
        <td>US DOLLAR</td>
        <td>3.45678</td>
        <td>2.34</td>
        <td></td>
        <td></td></tr></table></body></html>'''
        return Response(content=content)


class CbnTest(CurrencyRateSourceTest, CBN):
    rate_type = RATE_TO
    rate_dt = datetime.datetime(2009, 12, 6)
    is_with_sell_rate = True
    is_with_buy_rate = True
    is_with_mid_rate = True
    url = 'http://www.cbn.gov.ng/functions/export.asp?tablename=exchange'

    def __init__(self, *a):
        super(CbnTest, self).__init__(*a)
        self.id = 1020

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding == cls.url
        text = '''
        12/6/2009,US DOLLAR, 2009, "December", 3.45678, 1.234, 2.34,
        12/5/2009,US DOLLAR, 2009, "December", 1.1, 1.2, 1.3,
        12/05/2009,EURO, 2009, "December", 2.1, 2.2, 2.3,
        12/5/2009,SWISS FRANC, 2009, "December", 3.1, 3.2, 3.3,
        11/30/2009,SWISS FRANC, 2009, "November", 4.1, 4.2, 4.3,
        11/30/2009,EURO, 2009, "November", 6.66666, 3.456, 5.123,
        11/30/2009,EURO, 2009, "November", 6.66666, 3.456, 5.123,
        11/3/2009,EURO, 2009, "November", 6.66665, 3.454, 5.122,
        11/3/2009,EURO, 2009, "November", 6.66666, 3.456, 5.123,'''
        return Response(text=text)


class BceaoTest(CurrencyRateSourceTest, BCEAO):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_sell_rate = True
    is_with_buy_rate = True
    url = 'https://www.bceao.int/fr/cours/get_all_devise_by_date'

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding == cls.url
        content = '''
        <h2>Cours des devises du mercredi 1 août 2018</h2>
            <table>
                <tbody>
                <tr>
                    <th>Devise</th>
                    <th>Vente</th>
                    <th>Achat</th>
                </tr>
                <tr>
                    <td>Dollar us</td>
                    <td>2.34,000</td>
                    <td>3,45678</td>
                </tr>
                </tbody>
            </table>
        '''

        return Response(content=content, url=cls.url)


class SarbTest(CurrencyRateSourceTest, SARB):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_mid_rate = True
    url = 'https://wwwrs.resbank.co.za/WebIndicators/CurrentMarketRates.aspx'

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding == cls.url
        content = '''<html xmlns="http://www.w3.org/1999/xhtml">
                    <head></head>
                    <body>
                      <table class="DataTable">
                      </table>
                      <table class="DataTable">
                        <tr>
                          <td Class="LeftAlignData" width="360">
                            <span class="DataLink">
                              <a target="_self" href="&#xA;              IndicatorDetail.aspx?DataItem=MMSD719A">Rand per US Dollar</a>
                            </span>
                          </td>
                          <td Class="LeftAlignData">(5) </td>
                          <td Class="RightAlignData">1.234</td>
                          <td Class="RightAlignData">2018-06-25</td>
                          <td Class="RightAlignData"><img style="position:relative; left: 0px; width: 16px; hight: 16px" src="images/UpArrow.jpg" /></td>
                        </tr>
                        </table>
                    </body>
                    </html>'''
        return Response(content=content)


class CbkTest(CurrencyRateSourceTest, CBK):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_mid_rate = True
    is_with_sell_rate = True
    is_with_buy_rate = True
    url = 'https://www.centralbank.go.ke/wp-admin/admin-ajax.php'

    @property
    def requested_currencies(self):
        return ['USD', 'KES']

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding == cls.url
        json_text = {"draw": 0, "recordsTotal": "21", "recordsFiltered": "21", "data": [
            ["26\/06\/2018", 'US DOLLAR', "1.234", "3.45678", "2.34"]]}
        return Response(json_text=json_text)


class BnrTest(CurrencyRateSourceTest, BNR):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_mid_rate = True
    url = 'http://www.bnr.ro/nbrfxrates.xml'

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding == cls.url
        content = '''<?xml version="1.0" encoding="utf-8"?>
                    <DataSet xmlns="http://www.bnr.ro/xsd">
                        <Cube date="2018-06-26">
                          <Rate currency="USD">1.234</Rate>
                        </Cube>
                      </DataSet>'''
        return Response(content=content)


class MnbTest(CurrencyRateSourceTest, MNB):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_mid_rate = True
    base_currency_units = Decimal('2')
    url = 'https://www.mnb.hu/arfolyam-tablazat?query=daily,{}'.format(rate_dt.strftime("%d/%m/%Y"))

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding == cls.url
        content = '''
<!doctype html>
<html>
  <body>
    <main id="main" class="main main-table">
      <div class="stk stk-m">

        <a class="item" href="javascript:history.back()">
          <span class="in">
            <span class="iwrp">
              <svg class="icn icn-s"><use xlink:href="#i-l-arrow-circle-left" /></svg>
            </span>
            <span class="lbl bd bd-m fw-m">Vissza a lekérdezések oldalra</span>
          </span>
        </a>

        <h1 class="ttl ttl-xl">Árfolyamok</h1>

        <div class="twrp">
          <table class="pricetable">
            <thead>
              <tr>
                <th></th>
                  <th class="rotate">EUR</th>
                  <th class="rotate">USD</th>
              </tr>
              <tr>
                <th></th>
                  <th class="rotate">osztrák schilling</th>
                  <th class="rotate">ausztrál dollár</th>
              </tr>
              <tr>
                <th></th>
                  <th class="rotate">100</th>
                  <th class="rotate">2</th>
              </tr>
            </thead>
            <tbody>
                <tr>
                  <td class="rotate">2021. június 7.</td>
                    <td class="rotate">-</td>
                    <td class="rotate">1.234</td>
                </tr>
            </tbody>
          </table>
        </div>
      </div>
    </main>
  </body>
</html>
        '''
        return Response(content=content)


class BnbTest(CurrencyRateSourceTest, BNB):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_mid_rate = True
    url = 'http://www.bnb.bg/Statistics/StExternalSector/StExchangeRates/StERForeignCurrencies/index.htm'

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding == cls.url
        text = '''Foreign Exchange Rates of the Bulgarian lev and Price of Gold valid for 27.06.2018
Date,Currency,Code,Per unit of currency/gold,Levs (BGN),Reverse rate for 1 BGN
27.06.2018,Australian Dollar,USD,1,1.234,0.804007
        '''
        return Response(text=text)


class BoeTest(CurrencyRateSourceTest, BOE):
    rate_type = RATE_FROM
    rate_dt = YESTERDAY
    is_with_mid_rate = True
    url = 'https://www.bankofengland.co.uk/boeapps/database/_iadb-fromshowcolumns.asp?CodeVer=new&xml.x=yes'

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding == cls.url
        content = '''<?xml version="1.0" encoding="windows-1252"?>
<Envelope xmlns="http://www.gesmes.org/xml/2002-08-01">
    <Cube xmlns="https://www.bankofengland.co.uk/website/agg_series" SCODE="XUDLUSS" DESC="Spot exchange rate, US $ into Sterling" COUNTRY="" CONCAT="Not seasonally adjusted # Exchange rates # US dollar # Exchange rate (spot) - US dollar into sterling # US dollar ">
        <Cube FREQ_NAME="Daily" FREQ_CODE="D" FREQ_ORD="1"/><Cube SERIES_DEF="notes and definitions for Spot exchange rates" DEF_LOC="http://www.bankofengland.co.uk/statistics/pages/iadb/notesiadb/Spot_rates.aspx"/>
        <Cube CAT_NAME="SEASONAL ADJUSTMENT" CAT_VAL="U" VAL_DESC="Not seasonally adjusted" CAT_ORD="1"/>
        <Cube CAT_NAME="TYPE" CAT_VAL="E" VAL_DESC="Exchange rates" CAT_ORD="2"/>
        <Cube CAT_NAME="OUTPUT IN" CAT_VAL="D" VAL_DESC="US dollar" CAT_ORD="3"/>
        <Cube CAT_NAME="INSTRUMENT CURRENCY" CAT_VAL="$" VAL_DESC="US dollar" CAT_ORD="6"/>
        <Cube CAT_NAME="INSTRUMENTS" CAT_VAL="RUSD" VAL_DESC="Exchange rate (spot) - US dollar into sterling" CAT_ORD="7"/>
        <Cube FIRST_OBS="1975-01-02" LAST_OBS="{rate_dt}"/>
        <Cube TIME="{rate_dt}" OBS_VALUE="1.234" OBS_CONF="N" LAST_UPDATED="2021-06-25 09:30:00">
    </Cube></Cube>
</Envelope>'''.format(rate_dt=cls.rate_dt.strftime('%Y-%m-%d'))
        return Response(content=content)


class OandaTest(CurrencyRateSourceTest, OANDA):
    rate_type = RATE_TO
    rate_dt = TODAY
    is_with_sell_rate = True
    is_with_buy_rate = True
    domestic_currency = 'UZS'
    url = 'https://www.oanda.com/currency/converter/update'

    @classmethod
    def request_and_log(cls, url, encoding=None, *args, **kwargs):
        assert encoding == cls.url
        json_text = {"id": "1",
                     "data": {"bid_ask_data": {"bid": "2.34", "ask": "3.45678"}}}
        return Response(json_text=json_text)


@pytest.mark.parametrize('currency_source',
                         [CbrTest,
                          EcbTest,
                          TcmbTest,
                          NbuTest,
                          BocTest,
                          NbrbTest,
                          NbkzTest,
                          NbgeTest,
                          BnmTest,
                          NbkrTest,
                          CbuTest,
                          NbsTest,
                          NbtTest,
                          BomTest,
                          CbarTest,
                          CnbTest,
                          NbpTest,
                          BoiTest,
                          NbeTest,
                          CbnTest,
                          BceaoTest,
                          SarbTest,
                          CbkTest,
                          BnrTest,
                          MnbTest,
                          BnbTest,
                          BoeTest,
                          OandaTest
                          ])
def test_currency_rate_load(session, currency_source):
    cs = currency_source(session)
    loaded_rate = list(cs.load_rates(session))[0]
    expected_rate = {
        'counter_currency': currency_source.domestic_currency if currency_source.rate_type == RATE_TO else 'USD',
        'buy_rate': Decimal('3.45678') if getattr(currency_source, 'is_with_buy_rate', False) else None,
        'sell_rate': Decimal('2.34') if getattr(currency_source, 'is_with_sell_rate', False) else None,
        'base_currency_units': currency_source.base_currency_units,
        'base_currency': currency_source.domestic_currency if currency_source.rate_type == RATE_FROM else 'USD',
        'mid_rate': Decimal('1.234') if getattr(currency_source, 'is_with_mid_rate', False) else None,
        'dt': currency_source.rate_dt}
    hm.assert_that(
        loaded_rate,
        hm.has_entries(expected_rate),
    )


@pytest.mark.parametrize(
    'params, res',
    [
        pytest.param(
            {},
            [('USD', Decimal('3.45678'), Decimal('1.234'), Decimal('2.34'), datetime.datetime(2009, 12, 6))],
            id='wo_params',
        ),
        pytest.param(
            {'dt': [datetime.datetime(2009, 12, 5)], 'currency': ['USD', 'EUR', 'CHF', 'JPY']},
            [
                ('USD', Decimal('1.1'), Decimal('1.2'), Decimal('1.3'), datetime.datetime(2009, 12, 5)),
                ('EUR', Decimal('2.1'), Decimal('2.2'), Decimal('2.3'), datetime.datetime(2009, 12, 5)),
                ('CHF', Decimal('3.1'), Decimal('3.2'), Decimal('3.3'), datetime.datetime(2009, 12, 5)),
            ],
            id='choose date',
        ),
        pytest.param(
            {'dt': [datetime.datetime(2009, 12, 5)], 'currency': ['USD']},
            [('USD', Decimal('1.1'), Decimal('1.2'), Decimal('1.3'), datetime.datetime(2009, 12, 5))],
            id='choose currency',
        ),
        pytest.param(
            {'dt': [datetime.datetime(2009, 12, 6), datetime.datetime(2009, 12, 5)], 'currency': ['CHF', 'USD']},
            [
                ('USD', Decimal('3.45678'), Decimal('1.234'), Decimal('2.34'), datetime.datetime(2009, 12, 6)),
                ('USD', Decimal('1.1'), Decimal('1.2'), Decimal('1.3'), datetime.datetime(2009, 12, 5)),
                ('CHF', Decimal('3.1'), Decimal('3.2'), Decimal('3.3'), datetime.datetime(2009, 12, 5)),
            ],
            id='choose dates + currencies',
        ),
        pytest.param(
            {'dt': [datetime.datetime(2009, 11, 30)], 'currency': ['USD', 'EUR', 'CHF']},
            [
                ('CHF', Decimal('4.1'), Decimal('4.2'), Decimal('4.3'), datetime.datetime(2009, 11, 30)),
                ('EUR', Decimal('6.66666'), Decimal('3.456'), Decimal('5.123'), datetime.datetime(2009, 11, 30)),
            ],
            id='double',  # полностью идентичные дубли склеиваются
        ),
    ],
)
def test_filter_params(session, params, res):
    cs = CbnTest(session)
    loaded_rates = list(cs.load_rates(session, req_dts=params.get('dt'), requested_currencies=params.get('currency')))
    hm.assert_that(
        loaded_rates,
        hm.contains(*[
            hm.has_entries({
                'counter_currency': 'NGN',
                'buy_rate': buy_rate,
                'sell_rate': sell_rate,
                'base_currency': cur,
                'mid_rate': mid_rate,
                'dt': dt,
            })
            for (cur, buy_rate, mid_rate, sell_rate, dt) in res
        ]),
    )


@pytest.mark.parametrize(
    'response, updatable, res',
    [
        pytest.param(
            '''<?xml version="1.0" encoding="windows-1252"?>
            <Envelope xmlns="http://www.gesmes.org/xml/2002-08-01">
                <Cube xmlns="http://www.bankofengland.co.uk:10066/boeapps/database/agg_series" SCODE="XUDLUSS">
                    <Cube TIME="1970-01-01" OBS_VALUE="1.234" OBS_CONF="N" LAST_UPDATED="2021-06-25 09:30:00"></Cube>
                    <Cube TIME="1970-01-02" OBS_VALUE="5.678" OBS_CONF="N" LAST_UPDATED="2021-06-25 09:30:00"></Cube>
                </Cube>
            </Envelope>''',
            [True, True],
            [
                (datetime.datetime(1970, 1, 1), 'USD', Decimal('1.234')),
                (datetime.datetime(1970, 1, 2), 'USD', Decimal('5.678'))
            ],
            id='single currency rates',
        ),
        pytest.param(
            '''<?xml version="1.0" encoding="windows-1252"?>
            <Envelope xmlns="http://www.gesmes.org/xml/2002-08-01">
                <Cube xmlns="http://www.bankofengland.co.uk:10066/boeapps/database/agg_series" SCODE="XUDLUSS">
                    <Cube TIME="1970-01-01" OBS_VALUE="1.234" OBS_CONF="N" LAST_UPDATED="2021-06-25 09:30:00"></Cube>
                    <Cube TIME="1970-01-02" OBS_VALUE="5.678" OBS_CONF="N" LAST_UPDATED="2021-06-25 09:30:00"></Cube>
                </Cube>
            </Envelope>''',
            [False, True],
            [
                (datetime.datetime(1970, 1, 2), 'USD', Decimal('5.678'))
            ],
            id='single currency mixed rates',
        ),
        pytest.param(
            '''<?xml version="1.0" encoding="windows-1252"?>
            <Envelope xmlns="http://www.gesmes.org/xml/2002-08-01">
                <Cube xmlns="http://www.bankofengland.co.uk:10066/boeapps/database/agg_series" SCODE="XUDLERS">
                    <Cube TIME="1970-01-03" OBS_VALUE="12.34" OBS_CONF="N" LAST_UPDATED="2021-06-25 09:30:00"></Cube>
                    <Cube TIME="1970-01-05" OBS_VALUE="56.78" OBS_CONF="N" LAST_UPDATED="2021-06-25 09:30:00"></Cube>
                </Cube>
                <Cube xmlns="http://www.bankofengland.co.uk:10066/boeapps/database/agg_series" SCODE="XUDLUSS">
                    <Cube TIME="1970-01-01" OBS_VALUE="1.234" OBS_CONF="N" LAST_UPDATED="2021-06-25 09:30:00"></Cube>
                    <Cube TIME="1970-01-02" OBS_VALUE="5.678" OBS_CONF="N" LAST_UPDATED="2021-06-25 09:30:00"></Cube>
                </Cube>
            </Envelope>''',
            [True, True, True, True],
            [
                (datetime.datetime(1970, 1, 3), 'EUR', Decimal('12.34')),
                (datetime.datetime(1970, 1, 5), 'EUR', Decimal('56.78')),
                (datetime.datetime(1970, 1, 1), 'USD', Decimal('1.234')),
                (datetime.datetime(1970, 1, 2), 'USD', Decimal('5.678')),
            ],
            id='mixed currencies rates',
        ),
        pytest.param(
            '''<?xml version="1.0" encoding="windows-1252"?>
            <Envelope xmlns="http://www.gesmes.org/xml/2002-08-01">
                <Cube xmlns="http://www.bankofengland.co.uk:10066/boeapps/database/agg_series" SCODE="XUDLERS">
                    <Cube TIME="1970-01-03" OBS_VALUE="12.34" OBS_CONF="N" LAST_UPDATED="2021-06-25 09:30:00"></Cube>
                    <Cube TIME="1970-01-05" OBS_VALUE="56.78" OBS_CONF="N" LAST_UPDATED="2021-06-25 09:30:00"></Cube>
                </Cube>
                <Cube xmlns="http://www.bankofengland.co.uk:10066/boeapps/database/agg_series" SCODE="XUDLUSS">
                    <Cube TIME="1970-01-01" OBS_VALUE="1.234" OBS_CONF="N" LAST_UPDATED="2021-06-25 09:30:00"></Cube>
                    <Cube TIME="1970-01-02" OBS_VALUE="5.678" OBS_CONF="N" LAST_UPDATED="2021-06-25 09:30:00"></Cube>
                </Cube>
            </Envelope>''',
            [False, True, True, False],
            [
                (datetime.datetime(1970, 1, 5), 'EUR', Decimal('56.78')),
                (datetime.datetime(1970, 1, 1), 'USD', Decimal('1.234')),
            ],
            id='mixed currencies mixed rates',
        ),
    ],
)
def test_boe_custom(session, response, updatable, res):
    with mock.patch.object(BoeTest, 'request_and_log', return_value=Response(content=response)),\
        mock.patch.object(BoeTest, '_is_updatable', side_effect=updatable):
        cs = BoeTest(session)
        rates = cs.load_rates(session)
        for rate in rates:
            hm.assert_that(
                rate,
                hm.any_of(*[
                    hm.has_entries({
                        'counter_currency': currency,
                        'buy_rate': None,
                        'sell_rate': None,
                        'base_currency': 'GBP',
                        'mid_rate': mid_rate,
                        'dt': dt,
                    })
                    for (dt, currency, mid_rate) in res])
            )


def test_invalid_data(session):
    """Реальный случай: одна дата, одна валюта, разные цифры.
    В базу мы не должны записывать 2 строки на одну дату с одинаковыми валютами.
    """
    cs = CbnTest(session)
    rates = cs.load_rates(session, req_dts=[datetime.datetime(2009, 11, 3)], requested_currencies=['EUR'])

    for item in rates:
        cs.store_rate(session, item)

    hm.raises(sa.exc.IntegrityError, hm.calling(session.flush))
