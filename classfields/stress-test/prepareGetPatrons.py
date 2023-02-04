import sys

sys.path.append("../../../common")
import commonutils3


RECOMMENDER_REST_API = '/api/v1/get_recommendations/uid/'
# HEADER_HOST = '[Host: vgorovoy-01-sas.dev.vertis.yandex.net]'
# we can't fire in balancer, so we need the next header
# curl -v -H "Host: realty-price-estimator.vrts-slb.test.vertis.yandex.net" 'lb-int-01-sas.test.vertis.yandex.net'
# HEADER_HOST_TESTING = 'Host: realty-recommender.vrts-slb.test.vertis.yandex.net'
HEADER_HOST_TESTING = 'Host: docker-01-myt.test.vertis.yandex.net'
HEADER_CLOSE = 'Connection: close'
USER_AGENT =  'User-Agent: yandex.tank'
# examples = [
#     # '{"offers": [{"offer_id": "853152132890852865","offerType":"RENT","offerCategory":"APARTMENT","roomsTotal": 3,"area": 76.0, "kitchenSpace": 10.0, "price": 50000, "buildingId": "5954259884239214622", "description": "Собственник. Показы возможны сразу. Сдается комфортная трехкомнатная просторная квартира с евроремонтом и изолированными комнатами, просторной кухней с мебелью из натурального дерева, раздельным санузлом, всей необходимой мебелью (на фото) и техникой известных фирм (телевизор, большой холодильник No Frost, стиральная машина автомат, стеклокерамическая варочная панель, вытяжка, духовой шкаф, микроволновая печь, чайник, кондиционер, радиотелефон), подключен Интернет и телефон, есть балкон. Подъезд чистый, есть грузовой лифт, домофон и мусоропровод. Квартира расположена на 3-ем этаже 10-ти этажного дома в районе Южное Бутово. Рядом хорошая инфраструктура, в доме большой супермаркет Пятерочка, аптека, цветочный магазин и тп, рядом торговые центры, кафе, возле дома детский сад и школа, недалеко находится парк и лесопарк для прогулок. Метро ул. Горчакова в пешей доступности. Пожелания к нанимателям: русские или славяне, не курящие, без животных, преимущество семьям. Сдается на срок от 6 месяцев, можно с детьми от 3 лет, договор найма, предоплата и залог (не разбивается). Рады Вашим обращениям, собственники Надежда и Людмила", "subjectFederationId": 1, "renovation": "UNKNOWN", "geocoderAddress": "Россия, Москва, Южнобутовская улица, 29"}]}',
#     '{"offers": [{"offer_id": "168295225243098625","offerType":"RENT","offerCategory":"APARTMENT","roomsTotal": 1,"area": 39.3,"kitchenSpace": 10.0,"price": 26000,"buildingId": "4656466920004334452","description": "ЖК Некрасовка. метро \"Выхино\" 15 минут транспортом . Новый дом рядом с открывающейся в 2018 году ст. метро Некрасовка-2 мин. пешком. Окна во двор. Кухня 10 кв.м., комната 19 кв.м. Балкон. Железная дверь. Интернет. Квартира после ремонта с мебелью и техникой. Сдается впервые только на длительный срок. Дополнительно оплачивается вода, водоотведение, свет по счетчикам","renovation": "UNKNOWN","subjectFederationId": 1,"geocoderAddress": "Россия, Москва, Рождественская улица, 33"}]}',
#     '{"offers": [{"offer_id": "3459844219290333697","offerType":"SELL","offerCategory":"APARTMENT","roomsTotal": 1,"area": 34.0,"kitchenSpace": 8.0,"price": 2590000,"buildingId": "1291633642366727281","description": "temp description","renovation": "UNKNOWN","subjectFederationId": 11119,"geocoderAddress": "Россия, Республика Татарстан, Казань, улица Юлиуса Фучика, 117"}]}'
# ]

examples = ['401497022',
            '655264198'
]



def generate_patrons_for_tank(json_lines, header_host = HEADER_HOST_TESTING, output_file = 'patrons.txt'):
    '''
        generates ouput file in lunapark format: https://yandextank.readthedocs.io/en/latest/tutorial.html#uri-post-style
    '''
    output_lines = []
#     GET / HTTP/1.0
# Host: xxx.tanks.example.com
# User-Agent: xxx (shell 1)
#     output_lines.append(header_host)
#     output_lines.append(HEADER_CLOSE)

    for line in json_lines:
        # output_lines.append(str(len(line)) + ' ' + PRICE_ESTIMATOR_REST_API)
        get_query = "GET /ping HTTP/1.1"
        # final_line = '\n'.join((get_query, header_host, HEADER_CLOSE)) + '\n'
        final_line = '\n'.join((get_query, header_host, HEADER_CLOSE, USER_AGENT)) + '\r\n'
        output_lines.append(str(len(bytes(final_line, 'utf-8'))))
        output_lines.append(final_line)
    commonutils3.write_lines_2_file(output_file, output_lines, encode_utf = False)

# generate_patrons_for_tank(examples)
generate_patrons_for_tank(examples, header_host=HEADER_HOST_TESTING, output_file='patrons_recommender_phantom_ping.txt')

