# coding: utf-8
# import datetime
# import io
# import json
# import pprint
#
# import attr
# from PIL import Image, ImageDraw, ImageFont
# from deepdiff import DeepDiff
#
# import balance.balance_db as db
# import balance.balance_steps as steps
# import balance.balance_web as web
# import btestlib.pagediff as pagediff
# import btestlib.passport_steps as passport
# import btestlib.reporter as reporter
# import btestlib.utils as utils
# from btestlib import constants
# from btestlib.pagediff import ElementData, Filter
#
#
# def test_elements_lxml():
#     with open('contract_distribution.html', 'r') as f:
#         page_html = f.read()
#     expected_elements = pagediff.extract_elements(page_html)
#
#     new_page_html = get_html_page_webdriver()
#     actual_elements = pagediff.extract_elements(new_page_html)
#
#     expected_data = {element.path: element.to_check_format() for element in expected_elements}
#     actual_data = {element.path: element.to_check_format() for element in actual_elements}
#
#     ddiff = DeepDiff(actual_data, expected_data)
#
#     expected_order = [element.path for element in expected_elements]
#     actual_order = [element.path for element in actual_elements]
#
#     # todo-igogor порядок хуй нормально проверишь этим инструментом.
#     order_ddif = DeepDiff(actual_order, expected_order, ignore_order=True)
#     pprint.pprint(ddiff)
#
#     pass
#
#
# def test_2_consecutive_htmls():
#     page_html = get_html_page_webdriver()
#     expected_elements = pagediff.extract_elements(page_html)
#
#     new_page_html = get_html_page_webdriver()
#     actual_elements = pagediff.extract_elements(new_page_html)
#
#     expected_data = {element.path: element.to_check_format() for element in expected_elements}
#     actual_data = {element.path: element.to_check_format() for element in actual_elements}
#
#     ddiff = DeepDiff(actual_data, expected_data)
#
#     expected_order = [element.path for element in expected_elements]
#     actual_order = [element.path for element in actual_elements]
#
#     # todo-igogor порядок хуй нормально проверишь этим инструментом.
#     order_ddif = DeepDiff(actual_order, expected_order, ignore_order=True)
#     pprint.pprint(ddiff)
#
#     pass
#
#
# def test_path_in_webdriver():
#     page_html = get_html_page_webdriver()
#     expected_elements = pagediff.extract_elements(page_html)
#
#     with web.Driver() as driver:
#         url = 'https://balance-admin.greed-tm1f.yandex.ru/contract-edit.xml?contract_id=282273'
#         driver.get(url)
#
#         element = driver.find_element('xpath', '//' + expected_elements[123].path)
#
#         pass
#
#
# def test_outline_element():
#     # page_html = get_html_page_webdriver()
#     # expected_elements = pagediff.extract_elements(page_html)
#
#     with web.Driver() as driver:
#         url = 'https://balance-admin.greed-tm1f.yandex.ru/contract-edit.xml?contract_id=282273'
#         driver.get(url)
#
#         element = driver.find_element('xpath', "//span[@id='firm-label']")
#
#         coordinates = element.location
#         size = element.size
#
#         screen = driver.get_screenshot_as_png()
#
#         image = Image.open(io.BytesIO(screen))
#         draw = ImageDraw.Draw(image)
#         color_red = (255, 0, 0)
#         draw.rectangle((coordinates['x'], coordinates['y'],
#                         coordinates['x'] + size['width'], coordinates['y'] + size['height']), outline=color_red)
#         text = '1'
#         text_width, text_height = ImageFont.load_default().getsize(text)
#         draw.text((coordinates['x'] - text_width, coordinates['y'] - text_height), text=text, fill=color_red)
#         image.save('edited_screenshot.png')
#         png = io.BytesIO()
#         image.save(png, format='PNG')
#         reporter.attach(u'Скриншот', png.getvalue(), AttachmentType.PNG)
#         pass
#
#
# def test_different_contracts():
#     first_contract_url = 'https://balance-admin.greed-tm1f.yandex.ru/contract-edit.xml?contract_id=293231'
#     second_contract_url = 'https://balance-admin.greed-tm1f.yandex.ru/contract-edit.xml?contract_id=293232'
#
#     session = passport.auth_session()
#     # expected_page = pagediff.build_etalon(url=first_contract_url, session=session)
#     # actual_page_html = pagediff.get_html(url=second_contract_url, session=session)
#
#     # diff = pagediff.diff_html(expected_page.html, actual_page_html)
#     #
#     # pagediff.report(diff, second_contract_url, expected_page, session)
#
#     collateral_ids = ('297731', '297733')
#     contract_ids = ('293231', '293232')
#     client_ids = ('25858986', '25859028')
#     person_ids = ('4574448', '4574450')
#     # filtered_diff = diff.filter(Filter.value(ignore_values=collateral_ids + contract_ids + client_ids + person_ids))
#
#     # pagediff.report(filtered_diff, second_contract_url, expected_page, session)
#
#     pass
#
#
# def test_creating_contracts_with_stored_etalon():
#     test_unique_name = 'test_creating_contracts_with_stored_etalon_example'
#
#     client_id = steps.ClientSteps.create({'IS_AGENCY': 1})
#     person_id = steps.PersonSteps.create(client_id, constants.PersonTypes.UR.code)
#
#     to_iso = utils.Date.date_to_iso_format
#     NOW = datetime.datetime.now()
#     HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
#     HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
#     contract_id, external_id = steps.ContractSteps.create_contract_new('opt_agency', {'CLIENT_ID': client_id,
#                                                                                       'PERSON_ID': person_id,
#                                                                                       'SERVICES': [7],
#                                                                                       'PAYMENT_TYPE': 3,
#                                                                                       'DT': HALF_YEAR_BEFORE_NOW_ISO,
#                                                                                       'FINISH_DT': HALF_YEAR_AFTER_NOW_ISO,
#                                                                                       'IS_SIGNED': HALF_YEAR_BEFORE_NOW_ISO,
#                                                                                       'PERSONAL_ACCOUNT_FICTIVE': 0
#                                                                                       })
#     collateral_ids = [res['id'] for res in db.balance().execute(
#         query='SELECT id from T_CONTRACT_COLLATERAL WHERE CONTRACT2_ID = {}'.format(contract_id))]
#     client_name = db.balance().execute(query='SELECT name FROM T_CLIENT WHERE id = {}'.format(client_id),
#                                        single_row=True)['name']
#
#     contract_url = 'https://balance-admin.greed-tm1f.yandex.ru/contract-edit.xml?contract_id=' + str(contract_id)
#
#     pagediff.pagediff(unique_name=test_unique_name, url=contract_url,
#                       filters=[Filter.Value(value=str(value)) for value
#                                in [contract_id, external_id, person_id, client_id, client_name] + collateral_ids])
#     pass
#
#
# def test_ddiff():
#     l1 = [1, 2, 3, 4]
#     l2 = [2, 3, 4, 5, 6]
#
#     ddiff = DeepDiff(l1, l2, ignore_order=True)
#
#     added = sorted([(int(k[5:-1]), v) for k, v in ddiff['iterable_item_added'].iteritems()], key=lambda x: x[0])
#     removed = sorted([(int(k[5:-1]), v) for k, v in ddiff['iterable_item_removed'].iteritems()], key=lambda x: x[0])
#
#     l3 = [1, 3, 2, 4]
#     ddiff2 = DeepDiff(l1, l3)
#
#     moved = sorted([(int(k[5:-1]), v) for k, v in ddiff2['values_changed'].iteritems()], key=lambda x: x[0])
#
#     expected_indexes = {values['expected_value']: index for index, values in moved}
#     actual_indexes = {values['actual_value']: index for index, values in moved}
#
#     positions_change = sorted([(value, expected_indexes[value], actual_indexes[value])
#                                for value in expected_indexes.keys()],
#                               key=lambda x: x[1])
#
#     d1 = {1: 'a', '2': 'b', 3: 'c'}
#     d2 = {'2': 'bff', 3: 'e', 4: 'f'}
#
#     dict_diff = DeepDiff(d1, d2)
#
#     pass
#
#
# def test_synthetic_html():
#     with open('original.html', 'r') as f:
#         expected_html = f.read()
#     with open('edited.html', 'r') as f:
#         actual_html = f.read()
#
#     diff = pagediff.htmldiff(actual_html, expected_html)
#
#     # with reporter.step(u"")
#
#     pass
#
#
# def test_get_html_requests():
#     url = 'https://balance-admin.greed-tm1f.yandex.ru/contract-edit.xml?contract_id=293231'
#     # url = 'https://www.yandex.ru'
#
#     # requests_html = pagediff.get_html(url)
#
#     # with open('requests_html.html', 'a') as f:
#     #     f.write(requests_html.encode('utf-8'))
#
#     # webdriver_html = get_html_page_webdriver(url)
#
#     # import difflib
#     #
#     # ddd = difflib.ndiff(requests_html, webdriver_html)
#     # print '\n'.join(ddd)
#     #
#     # pddd = pagediff.diff_html(webdriver_html, requests_html)
#
#     pass
#
#
# def test_attr_serialisation():
#     obj = ElementData(path='//body[1]/div[2]/div[4]/span[2]/input[1]', tag='input', text=u'пффф',
#                       attributes={'style': u'display: none;', 'name': u'is-faxed-date', 'disabled': u'disabled',
#                                   'readonly': u'', 'type': u'text', 'id': u'is-faxed-date'})
#     print str(obj)
#     utils.Presenter.pretty(attr.asdict(obj))
#     ddd = attr.asdict(obj)
#
#     with open('obj_json.json', 'a') as f:
#         json.dump(ddd, f)
#
#     with open('obj_json.json') as f:
#         obj_json = json.load(f)
#         deser = ElementData(**obj_json)
#
#     pass
#
#
# def test_cookies_txt():
#     pagediff.get_page_archive(url='https://balance-admin.greed-tm1f.yandex.ru/contract-edit.xml?contract_id=316921')
#
#     pass
#
#
# def get_html_page_webdriver(url):
#     with web.Driver() as driver:
#         driver.get(url)
#         html = driver.page_source
#         return html
#         # element.get_attribute('innerHTML') # or outerHTML - с текущим элементом
#
#
# from lxml import etree
# import copy
#
#
# def difference_comment(element, type_='changed'):
#     # $x("//comment()[contains(., 'pagediff')]/preceding-sibling::*")
#     # $x("//comment()[contains(., 'pagediff')]")
#     return etree.Comment('{}\n{}'.format('pagediff-' + type_, etree.tostring(element, method='html')))
#
#
# def test_lxml_parse_print():
#     with open('contract_html_example.html', 'r') as f:
#         html_string = f.read().decode('utf-8')
#
#     tree = etree.HTML(html_string)
#     element = tree.xpath('//body[1]/table[1]/tbody[1]/tr[1]/td[2]')[0]
#
#     changed_element = copy.deepcopy(element)
#     changed_element.text = u'ololo beatch'
#     comment = difference_comment(changed_element)
#
#     parent = element.getparent()
#     parent.insert(parent.index(element) + 1, comment)
#
#     parsed_html = etree.tostring(tree, method='html')
#
#     with open('contract_html_example_parsed.html', 'w') as f:
#         f.write(parsed_html.encode('utf-8'))
#
#
# @attr.s(init=False)
# class Temp(object):
#     x = attr.ib()
#     y = attr.ib()
#
#     def __init__(self, y, x=6):
#         self.y = y
#         self.x = x
#
#
# def test_attrs_init():
#     t = Temp(y=3)
