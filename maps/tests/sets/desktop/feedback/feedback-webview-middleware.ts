import cssSelectors from '../../../common/css-selectors';

const FEDBACK_KNOWN_CONTEXT = 'map.context';
const FEDBACK_UNKNOWN_CONTEXT = 'new.context';

const ADD_TOPONYM_URL =
    '/feedback/?type=object/add&ll=37.597381,55.745915&z=15&client_id=ru.yandex.yandexmaps&' +
    'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&webview=true';
const EDIT_TOPONYM_URL =
    '/feedback/?type=object/edit&uri=ymapsbm1%3A%2F%2Fgeo%3Fll%3D37.589%252C55.734%26spn' +
    '%3D0.001%252C0.001%26text%3D%25D0%25A0%25D0%25BE%25D1%2581%25D1%2581%25D0%25B8%25D1%258F%252C%2520%25D0%' +
    '259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%252C%2520%25D1%2583%25D0%25BB%25D0%25B8%25D1%2586' +
    '%25D0%25B0%2520%25D0%25A2%25D0%25B8%25D0%25BC%25D1%2583%25D1%2580%25D0%25B0%2520%25D0%25A4%25D1%2580%25D1' +
    '%2583%25D0%25BD%25D0%25B7%25D0%25B5%252C%252011%25D0%25BA8&client_id=ru.yandex.yandexmaps&' +
    'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&webview=true';
const EDIT_ORGANIZATION_URL =
    '/feedback/?type=organization/edit&uri=ymapsbm1%3A%2F%2Forg%3Foid%3D4709482929&' +
    'client_id=ru.yandex.yandexmaps&uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&' +
    'context=organization.footer&webview=true';
const ADD_ADDRESS_URL =
    '/feedback/?type=address/add&ll=37.597381,55.745915&z=15&client_id=ru.yandex.yandexmaps&' +
    'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.add_object_to_map&webview=true';
const ADD_OTHER_URL =
    '/feedback/?type=other/add&ll=37.597381,55.745915&z=15&client_id=ru.yandex.yandexmaps&' +
    'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&webview=true';

const views = [
    [
        'Форма выбора объекта для редактирования',
        '/feedback/?type=object/select&ll=37.597381,55.745915&z=15&client_id=desktop-maps',
        cssSelectors.selectObjectFeedback.view
    ],
    ['Добавление объекта (разводящая форма)', ADD_TOPONYM_URL, cssSelectors.addObjectFeedback.view],
    ['Добавление объекта: адрес', ADD_ADDRESS_URL, cssSelectors.addToponymFeedback.view],
    [
        'Добавление объекта: подъезд',
        '/feedback/?type=entrance/add&ll=37.597381,55.745915&z=15&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.add_object_to_map&webview=true',
        cssSelectors.addToponymFeedback.view
    ],
    [
        'Добавление объекта: дорога',
        '/feedback/?type=road/add&ll=37.597381,55.745915&z=15&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.add_object_to_map&webview=true',
        cssSelectors.addToponymFeedback.view
    ],
    [
        'Добавление объекта: шлагбаум',
        '/feedback/?type=barrier/add&ll=37.597381,55.745915&z=15&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.add_object_to_map&webview=true',
        cssSelectors.addToponymFeedback.view
    ],
    [
        'Добавление объекта: парковка',
        '/feedback/?type=parking/add&ll=37.597381,55.745915&z=15&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.add_object_to_map&webview=true',
        cssSelectors.addToponymFeedback.view
    ],
    [
        'Добавление объекта: забор',
        '/feedback/?type=fence/add&ll=37.597381,55.745915&z=15&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.add_object_to_map&webview=true',
        cssSelectors.addToponymFeedback.view
    ],
    [
        'Добавление объекта: калитка',
        '/feedback/?type=gate/add&ll=37.597381,55.745915&z=15&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.add_object_to_map&webview=true',
        cssSelectors.addToponymFeedback.view
    ],
    [
        'Добавление объекта: пешеходный переход',
        '/feedback/?type=crosswalk/add&ll=37.597381,55.745915&z=15&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.add_object_to_map&webview=true',
        cssSelectors.addToponymFeedback.view
    ],
    [
        'Добавление объекта: провайдер',
        '/feedback/?type=provider/add&ll=37.597381,55.745915&z=15&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.add_object_to_map&webview=true',
        cssSelectors.addToponymFeedback.view
    ],
    [
        'Добавление объекта: остановка',
        '/feedback/?type=transit/stop/add&ll=37.597381,55.745915&z=15&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.add_object_to_map&webview=true',
        cssSelectors.addToponymFeedback.view
    ],
    [
        'Добавление объекта: организация',
        '/feedback/?type=organization/add&ll=37.597381,55.745915&z=15&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.add_object_to_map&webview=true',
        cssSelectors.organizationFeedback.card
    ],
    [
        'Добавление объекта: другое',
        '/feedback/?type=other/add&ll=37.597381,55.745915&z=15&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.add_object_to_map&webview=true',
        cssSelectors.addToponymFeedback.view
    ],
    [
        'Редактирование объекта: организация (разводящая форма)',
        EDIT_ORGANIZATION_URL,
        cssSelectors.organizationFeedback.menu
    ],
    [
        'Редактирование объекта: изменение информации об организации',
        '/feedback/?type=organization/edit-info&uri=ymapsbm1%3A%2F%2Forg%3Foid%3D4709482929&' +
            'client_id=ru.yandex.yandexmaps&uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&' +
            'context=organization.footer&webview=true',
        cssSelectors.organizationFeedback.card
    ],
    ['Редактирование объекта: топоним (разводящая форма)', EDIT_TOPONYM_URL, cssSelectors.editObjectFeedback.view],
    [
        'Редактирование объекта через whatshere: топоним (разводящая форма)',
        '/feedback/?type=object/edit&ll=37.588806%2C55.734243&z=20&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.building&webview=true',
        cssSelectors.editObjectFeedback.view
    ],
    [
        'Редактирование объекта: другое',
        '/feedback/?type=object/other&uri=ymapsbm1%3A%2F%2Fgeo%3Fll%3D37.589%252C55.734%26spn%3D0.001%252C0.001' +
            '%26text%3D%25D0%25A0%25D0%25BE%25D1%2581%25D1%2581%25D0%25B8%25D1%258F%252C%2520%25D0%259C%25D0%25BE%' +
            '25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%252C%2520%25D1%2583%25D0%25BB%25D0%25B8%25D1%2586%25D0%25B0%' +
            '2520%25D0%25A2%25D0%25B8%25D0%25BC%25D1%2583%25D1%2580%25D0%25B0%2520%25D0%25A4%25D1%2580%25D1%2583%' +
            '25D0%25BD%25D0%25B7%25D0%25B5%252C%252011%25D0%25BA8&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.building&webview=true',
        cssSelectors.editObjectFeedback.views.base
    ],
    [
        'Редактирование объекта: удалить объект с карты',
        '/feedback/?type=object/not-found&uri=ymapsbm1%3A%2F%2Fgeo%3Fll%3D37.589%252C55.734%26spn%3D0.001%' +
            '252C0.001%26text%3D%25D0%25A0%25D0%25BE%25D1%2581%25D1%2581%25D0%25B8%25D1%258F%252C%2520%25D0%' +
            '259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%252C%2520%25D1%2583%25D0%25BB%25D0%25B8%' +
            '25D1%2586%25D0%25B0%2520%25D0%25A2%25D0%25B8%25D0%25BC%25D1%2583%25D1%2580%25D0%25B0%2520%25D0%' +
            '25A4%25D1%2580%25D1%2583%25D0%25BD%25D0%25B7%25D0%25B5%252C%252011%25D0%25BA8&' +
            'client_id=ru.yandex.yandexmaps&uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&' +
            'context=toponym.building&webview=true',
        cssSelectors.editObjectFeedback.views.base
    ],
    [
        'Редактирование объекта: изменение местоположения',
        '/feedback/?type=object/location/edit&uri=ymapsbm1%3A%2F%2Fgeo%3Fll%3D37.588%252C55.734%26spn%3D0.009' +
            '%252C0.005%26text%3D%25D0%25A0%25D0%25BE%25D1%2581%25D1%2581%25D0%25B8%25D1%258F%252C%2520%25D0%' +
            '259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%252C%2520%25D0%25A6%25D0%25B5%25D0%25BD%' +
            '25D1%2582%25D1%2580%25D0%25B0%25D0%25BB%25D1%258C%25D0%25BD%25D1%258B%25D0%25B9%2520%25D0%25B0%' +
            '25D0%25B4%25D0%25BC%25D0%25B8%25D0%25BD%25D0%25B8%25D1%2581%25D1%2582%25D1%2580%25D0%25B0%25D1%' +
            '2582%25D0%25B8%25D0%25B2%25D0%25BD%25D1%258B%25D0%25B9%2520%25D0%25BE%25D0%25BA%25D1%2580%25D1%' +
            '2583%25D0%25B3%252C%2520%25D1%2580%25D0%25B0%25D0%25B9%25D0%25BE%25D0%25BD%2520%25D0%25A5%25D0%' +
            '25B0%25D0%25BC%25D0%25BE%25D0%25B2%25D0%25BD%25D0%25B8%25D0%25BA%25D0%25B8%252C%2520%25D0%25BA%' +
            '25D0%25B2%25D0%25B0%25D1%2580%25D1%2582%25D0%25B0%25D0%25BB%2520%25D0%259A%25D1%2580%25D0%25B0%' +
            '25D1%2581%25D0%25BD%25D0%25B0%25D1%258F%2520%25D0%25A0%25D0%25BE%25D0%25B7%25D0%25B0&' +
            'client_id=ru.yandex.yandexmaps&uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345' +
            '&context=toponym.default&webview=true',
        cssSelectors.editObjectFeedback.views.base
    ],
    [
        'Редактирование объекта: изменение наименования',
        '/feedback/?type=object/name/edit&uri=ymapsbm1%3A%2F%2Fgeo%3Fll%3D37.588%252C55.734%26spn%3D0.009%' +
            '252C0.005%26text%3D%25D0%25A0%25D0%25BE%25D1%2581%25D1%2581%25D0%25B8%25D1%258F%252C%2520%25D0%' +
            '259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%252C%2520%25D0%25A6%25D0%25B5%25D0%25BD%' +
            '25D1%2582%25D1%2580%25D0%25B0%25D0%25BB%25D1%258C%25D0%25BD%25D1%258B%25D0%25B9%2520%25D0%25B0%' +
            '25D0%25B4%25D0%25BC%25D0%25B8%25D0%25BD%25D0%25B8%25D1%2581%25D1%2582%25D1%2580%25D0%25B0%25D1%' +
            '2582%25D0%25B8%25D0%25B2%25D0%25BD%25D1%258B%25D0%25B9%2520%25D0%25BE%25D0%25BA%25D1%2580%25D1%' +
            '2583%25D0%25B3%252C%2520%25D1%2580%25D0%25B0%25D0%25B9%25D0%25BE%25D0%25BD%2520%25D0%25A5%25D0%' +
            '25B0%25D0%25BC%25D0%25BE%25D0%25B2%25D0%25BD%25D0%25B8%25D0%25BA%25D0%25B8%252C%2520%25D0%25BA%' +
            '25D0%25B2%25D0%25B0%25D1%2580%25D1%2582%25D0%25B0%25D0%25BB%2520%25D0%259A%25D1%2580%25D0%25B0%' +
            '25D1%2581%25D0%25BD%25D0%25B0%25D1%258F%2520%25D0%25A0%25D0%25BE%25D0%25B7%25D0%25B0&' +
            'client_id=ru.yandex.yandexmaps&uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&' +
            'context=toponym.default&webview=true',
        cssSelectors.editObjectFeedback.views.base
    ],
    [
        'Редактирование входа в здание: выбор входа (разводящая форма)',
        '/feedback/?type=entrance/select&uri=ymapsbm1%3A%2F%2Fgeo%3Fll%3D37.589%252C55.734%26spn%3D0.001%' +
            '252C0.001%26text%3D%25D0%25A0%25D0%25BE%25D1%2581%25D1%2581%25D0%25B8%25D1%258F%252C%2520%25D0%' +
            '259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%252C%2520%25D1%2583%25D0%25BB%25D0%25B8%' +
            '25D1%2586%25D0%25B0%2520%25D0%25A2%25D0%25B8%25D0%25BC%25D1%2583%25D1%2580%25D0%25B0%2520%25D0%' +
            '25A4%25D1%2580%25D1%2583%25D0%25BD%25D0%25B7%25D0%25B5%252C%252011%25D0%25BA8&' +
            'client_id=ru.yandex.yandexmaps&uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&' +
            'context=toponym.building&webview=true',
        cssSelectors.editObjectFeedback.views.base
    ],
    [
        'Редактирование входа в здание (разводящая форма)',
        '/feedback/?type=entrance/edit&uri=ymapsbm1%3A%2F%2Fgeo%3Fll%3D37.589%252C55.734%26spn%3D0.001%' +
            '252C0.001%26text%3D%25D0%25A0%25D0%25BE%25D1%2581%25D1%2581%25D0%25B8%25D1%258F%252C%2520%25D0%' +
            '259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%252C%2520%25D1%2583%25D0%25BB%25D0%25B8%' +
            '25D1%2586%25D0%25B0%2520%25D0%25A2%25D0%25B8%25D0%25BC%25D1%2583%25D1%2580%25D0%25B0%2520%25D0%' +
            '25A4%25D1%2580%25D1%2583%25D0%25BD%25D0%25B7%25D0%25B5%252C%252011%25D0%25BA8%252C%2520%25D0%' +
            '25BF%25D0%25BE%25D0%25B4%25D1%258A%25D0%25B5%25D0%25B7%25D0%25B4%25201%2520%257B3940017220%' +
            '257D&client_id=ru.yandex.yandexmaps&uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&' +
            'context=toponym.building&webview=true',
        cssSelectors.editObjectFeedback.views.base
    ],
    [
        'Редактирование здания: исправление имени входа в здание',
        '/feedback/?type=entrance/name/edit&uri=ymapsbm1%3A%2F%2Fgeo%3Fll%3D37.589%252C55.734%26spn%3D0.001%' +
            '252C0.001%26text%3D%25D0%25A0%25D0%25BE%25D1%2581%25D1%2581%25D0%25B8%25D1%258F%252C%2520%25D0%' +
            '259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%252C%2520%25D1%2583%25D0%25BB%25D0%25B8%' +
            '25D1%2586%25D0%25B0%2520%25D0%25A2%25D0%25B8%25D0%25BC%25D1%2583%25D1%2580%25D0%25B0%2520%25D0%' +
            '25A4%25D1%2580%25D1%2583%25D0%25BD%25D0%25B7%25D0%25B5%252C%252011%25D0%25BA8%252C%2520%25D0%' +
            '25BF%25D0%25BE%25D0%25B4%25D1%258A%25D0%25B5%25D0%25B7%25D0%25B4%25201%2520%257B3940017220%257D&' +
            'client_id=ru.yandex.yandexmaps&uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&' +
            'context=toponym.building&webview=true',
        cssSelectors.editObjectFeedback.views.base
    ],
    [
        'Редактирование здания: неверное расположение входа в здание',
        '/feedback/?type=entrance/location/edit&uri=ymapsbm1%3A%2F%2Fgeo%3Fll%3D37.589%252C55.734%26spn%' +
            '3D0.001%252C0.001%26text%3D%25D0%25A0%25D0%25BE%25D1%2581%25D1%2581%25D0%25B8%25D1%258F%252C%' +
            '2520%25D0%259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%252C%2520%25D1%2583%25D0%' +
            '25BB%25D0%25B8%25D1%2586%25D0%25B0%2520%25D0%25A2%25D0%25B8%25D0%25BC%25D1%2583%25D1%2580%' +
            '25D0%25B0%2520%25D0%25A4%25D1%2580%25D1%2583%25D0%25BD%25D0%25B7%25D0%25B5%252C%252011%25D0%' +
            '25BA8%252C%2520%25D0%25BF%25D0%25BE%25D0%25B4%25D1%258A%25D0%25B5%25D0%25B7%25D0%25B4%25201%' +
            '2520%257B3940017220%257D&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&context=toponym.building&webview=true',
        cssSelectors.editObjectFeedback.views.base
    ],
    [
        'Редактирование здания: входа в здание здесь нет',
        '/feedback/?type=entrance/not-found&uri=ymapsbm1%3A%2F%2Fgeo%3Fll%3D37.589%252C55.734%26spn%3D0.001%' +
            '252C0.001%26text%3D%25D0%25A0%25D0%25BE%25D1%2581%25D1%2581%25D0%25B8%25D1%258F%252C%2520%25D0%' +
            '259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%252C%2520%25D1%2583%25D0%25BB%25D0%25B8%' +
            '25D1%2586%25D0%25B0%2520%25D0%25A2%25D0%25B8%25D0%25BC%25D1%2583%25D1%2580%25D0%25B0%2520%25D0%' +
            '25A4%25D1%2580%25D1%2583%25D0%25BD%25D0%25B7%25D0%25B5%252C%252011%25D0%25BA8%252C%2520%25D0%' +
            '25BF%25D0%25BE%25D0%25B4%25D1%258A%25D0%25B5%25D0%25B7%25D0%25B4%25201%2520%257B3940017220%257D&' +
            'client_id=ru.yandex.yandexmaps&uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&' +
            'context=toponym.building&webview=true',
        cssSelectors.editObjectFeedback.views.base
    ],
    [
        'Редактирование здания: неверный адрес',
        '/feedback/?type=address/edit&uri=ymapsbm1%3A%2F%2Fgeo%3Fll%3D37.589%252C55.734%26spn%3D0.001%' +
            '252C0.001%26text%3D%25D0%25A0%25D0%25BE%25D1%2581%25D1%2581%25D0%25B8%25D1%258F%252C%2520%' +
            '25D0%259C%25D0%25BE%25D1%2581%25D0%25BA%25D0%25B2%25D0%25B0%252C%2520%25D1%2583%25D0%25BB%' +
            '25D0%25B8%25D1%2586%25D0%25B0%2520%25D0%25A2%25D0%25B8%25D0%25BC%25D1%2583%25D1%2580%25D0%' +
            '25B0%2520%25D0%25A4%25D1%2580%25D1%2583%25D0%25BD%25D0%25B7%25D0%25B5%252C%252011%25D0%' +
            '25BA8&client_id=ru.yandex.yandexmaps&uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&' +
            'deviceid=12345&context=toponym.building&webview=true',
        cssSelectors.editObjectFeedback.views.base
    ],
    [
        'Редактирование маршрута (разводящая форма)',
        '/feedback/?type=route/edit&uri=ymapsbm1%3A%2F%2Froute%2Fdriving%2Fv1%2FCswCCmCAiu0jD78dnw6PHe8' +
            'dzzigKtAY-Asg2BLIF_gW4BfIDJAEpwW_BtcP11rPEbA6gDvgXrBpwEuoG-Ak0A5I3xDvCIcR-ASvLqcUhwzHAcc_lzvwKH' +
            'fXDLdi5zeXEecbQF8SYeDTkzW_CscvnwPADbgS0Cl33w-HEBDvA4cPzwOQJ-ga0AGnC48KiAGAIoAFtxiPFO8d7yHPFt8Dt' +
            'wG3A88DzwHoAj__GN8Uj0OvJhj4C-gLuCnQAfcGj0LHGq8E_wvgCWAaVYYBxgHkAbACtwLBAjCJAYoBMzOJAY4BExAVecwB' +
            'gQKuArICwwF7d3Z4bWNYnAHYAaMCkALQAY8CuwHAAZsCmwKhAqACEKMC2wHcAbcC7wGvAscCxwIiCggmEgYIARAJGBciCgg' +
            'AEgYQxQUYmQMiCggxEgYQmzYYuBgiCggKEgYQqAIYzwESABoAIIRL&travel_mode=auto&' +
            'way_points=55.735327%2C37.593373~55.734127%2C37.588731~55.718604%2C37.586964&' +
            'via_points=55.735403999999996%2C37.593929&traffic_jams=true&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&webview=true',
        cssSelectors.routeFeedback.view
    ],
    [
        'Редактирование маршрута: нельзя проехать по маршруту (разводящая форма)',
        '/feedback/?type=route/incorrect&uri=ymapsbm1%3A%2F%2Froute%2Fdriving%2Fv1%2FCswCCmCAiu0jD78dnw6P' +
            'He8dzzigKtAY-Asg2BLIF_gW4BfIDJAEpwW_BtcP11rPEbA6gDvgXrBpwEuoG-Ak0A5I3xDvCIcR-ASvLqcUhwzHAcc_lzvw' +
            'KHfXDLdi5zeXEecbQF8SYeDTkzW_CscvnwPADbgS0Cl33w-HEBDvA4cPzwOQJ-ga0AGnC48KiAGAIoAFtxiPFO8d7yHPFt8D' +
            'twG3A88DzwHoAj__GN8Uj0OvJhj4C-gLuCnQAfcGj0LHGq8E_wvgCWAaVYYBxgHkAbACtwLBAjCJAYoBMzOJAY4BExAVecwB' +
            'gQKuArICwwF7d3Z4bWNYnAHYAaMCkALQAY8CuwHAAZsCmwKhAqACEKMC2wHcAbcC7wGvAscCxwIiCggmEgYIARAJGBciCggA' +
            'EgYQxQUYmQMiCggxEgYQmzYYuBgiCggKEgYQqAIYzwESABoAIIRL&travel_mode=auto&' +
            'way_points=55.735327%2C37.593373~55.734127%2C37.588731~55.718604%2C37.586964&' +
            'via_points=55.735403999999996%2C37.593929&traffic_jams=true&client_id=ru.yandex.yandexmaps&' +
            'uuid=38c285c4-777e-a5d6-7caf-a169df3bce6a&deviceid=12345&webview=true',
        cssSelectors.routeFeedback.view
    ]
];

describe('Фидбэк webview', () => {
    describe('Новый API', () => {
        describe('Переход по URL', () => {
            views.forEach(([step, url, selector]) => {
                it(step, async function () {
                    await this.browser.openPage(url);
                    await this.browser.waitForVisible(selector);
                });
            });
        });

        describe('Завершение фидбека', () => {
            [
                ['Организация', EDIT_ORGANIZATION_URL],
                ['Добавление топонима', ADD_TOPONYM_URL],
                ['Редактирвоание топонима', EDIT_TOPONYM_URL]
            ].forEach(([name, url]) => {
                it(name, async function () {
                    await this.browser.openPage(url, {isMobile: true});
                    await this.browser.waitAndClick(cssSelectors.feedback.header.close);
                    await this.browser.verifyUrl('/feedback/completed', {
                        custom: {
                            validate: (actual: string, expected: string) => actual.endsWith(expected)
                        }
                    });
                });
            });
        });

        describe('Передача параметров', () => {
            hermione.also.in(['chrome-dark', 'iphone-dark']);
            it('Отступы', async function () {
                await this.browser.openPage(ADD_ADDRESS_URL + '&safearea_inset_top=75&safearea_inset_bottom=20', {
                    isMobile: true
                });
                await this.browser.waitAndVerifyScreenshot(cssSelectors.mapReady, 'insets');
                await this.browser.waitForUrlContains(
                    {
                        query: {
                            safearea_inset_top: 75,
                            safearea_inset_bottom: 20
                        }
                    },
                    {partial: true}
                );
            });

            describe('Контексты', () => {
                it('Известный контекст', async function () {
                    await this.browser.openPage(`${ADD_OTHER_URL}&context=${FEDBACK_KNOWN_CONTEXT}`, {
                        isMobile: true
                    });
                    await this.browser.waitForUrlContains(
                        {
                            query: {
                                'feedback-client-context': FEDBACK_KNOWN_CONTEXT,
                                'feedback-context': FEDBACK_KNOWN_CONTEXT
                            }
                        },
                        {partial: true}
                    );
                });

                it('Новый контекст', async function () {
                    await this.browser.openPage(`${ADD_OTHER_URL}&context=${FEDBACK_UNKNOWN_CONTEXT}`, {
                        isMobile: true
                    });
                    await this.browser.waitForUrlContains(
                        {
                            query: {
                                'feedback-client-context': FEDBACK_UNKNOWN_CONTEXT,
                                'feedback-context': (context: string | undefined) => context === undefined
                            }
                        },
                        {partial: true}
                    );
                });
            });
        });
    });

    describe('Старый API', () => {
        describe('Завершение фидбека', () => {
            afterEach(async function () {
                await this.browser.verifyUrl('/mobile-feedback?type=completed', {
                    custom: {
                        validate: (actual: string, expected: string) => actual.endsWith(expected)
                    }
                });
            });

            it('Добавление организации', async function () {
                await this.browser.openPage('/mobile-feedback?type=business_add', {isMobile: true});
                await this.browser.waitForVisible(cssSelectors.organizationFeedback.card);
                await this.browser.waitAndClick(cssSelectors.feedback.header.close);
            });

            it('Редактирование организации', async function () {
                await this.browser.openPage('/mobile-feedback?type=business_complaint&business_oid=4709482929', {
                    isMobile: true
                });
                await this.browser.waitForVisible(cssSelectors.organizationFeedback.menu);
                await this.browser.waitAndClick(cssSelectors.feedback.header.close);
            });
        });
    });
});
