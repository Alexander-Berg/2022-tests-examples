import {SeoFile} from '../types';

const hexUriBus123 =
    '796d617073626d313a2f2f7472616e7369742f6c696e653f69643d3231335f3132335f6275735f6d6f73676f727472616e73266c6c3d33372e35333132313625324335352e383338353031266e616d653d31323326723d3237383926747970653d627573';
const hexUriBus282 =
    '796d617073626d313a2f2f7472616e7369742f6c696e653f69643d32303336393235373238266c6c3d33372e35353335323625324335352e383630333835266e616d653d32383226723d3537373926747970653d627573';
const hexUriBus101 =
    '796d617073626d313a2f2f7472616e7369742f6c696e653f69643d32303337323639393730266c6c3d33372e34323530363125324335342e383334383130266e616d653d31303126723d313132303026747970653d6d696e69627573';

const contents: SeoFile = {
    name: 'Лендинг маршрутов ОТ',
    specs: [
        {
            name: 'Обычный',
            url: `/213/moscow/routes/bus_123/${hexUriBus123}/`,
            canonical: `https://yandex.ru/maps/213/moscow/routes/bus_123/${hexUriBus123}/`,
            h1: 'Автобус 123',
            title: 'Автобус 123 в Москве: маршрут, остановки — Яндекс Карты',
            description:
                'Маршрут и остановки автобуса 123 на карте Москвы. Маршруты общественного транспорта на Яндекс Картах',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Общественный транспорт', url: 'https://yandex.ru/maps/213/moscow/transport/'},
                {name: 'Автобусы', url: 'https://yandex.ru/maps/213/moscow/transport/buses/'},
                {name: 'Автобус 123', url: `https://yandex.ru/maps/213/moscow/routes/bus_123/${hexUriBus123}/`}
            ],
            alternates: [
                {
                    href: `https://yandex.ru/maps/213/moscow/routes/bus_123/${hexUriBus123}/`,
                    hreflang: 'ru'
                },
                {
                    href: `https://yandex.com/maps/213/moscow/routes/bus_123/${hexUriBus123}/`,
                    hreflang: 'en'
                }
            ]
        },
        {
            name: 'С неправильным городом в ссылке',
            url: `/43/kazan/routes/bus_282/${hexUriBus282}/`,
            canonical: `https://yandex.ru/maps/213/moscow/routes/bus_282/${hexUriBus282}/`,
            h1: 'Автобус 282',
            title: 'Автобус 282 в Москве: маршрут, остановки — Яндекс Карты',
            description:
                'Маршрут и остановки автобуса 282 на карте Москвы. Маршруты общественного транспорта на Яндекс Картах',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Общественный транспорт', url: 'https://yandex.ru/maps/213/moscow/transport/'},
                {name: 'Автобусы', url: 'https://yandex.ru/maps/213/moscow/transport/buses/'},
                {name: 'Автобус 282', url: `https://yandex.ru/maps/213/moscow/routes/bus_282/${hexUriBus282}/`}
            ],
            redirectUrl: `/213/moscow/routes/bus_282/${hexUriBus282}`
        },
        {
            name: 'Проходящий через разные города',
            url: `/10754/serpuhov/routes/minibus_101/${hexUriBus101}/`,
            canonical: `https://yandex.ru/maps/10754/serpuhov/routes/minibus_101/${hexUriBus101}/`,
            h1: 'Маршрутка 101',
            title: 'Маршрутка 101, Серпухов - Заокский: маршрут, остановки — Яндекс Карты',
            description:
                'Маршрут и остановки маршрутки 101 на карте Серпухова. Маршруты общественного транспорта на Яндекс Картах',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Серпухов', url: 'https://yandex.ru/maps/10754/serpuhov/'},
                {name: 'Общественный транспорт', url: 'https://yandex.ru/maps/10754/serpuhov/transport/'},
                {
                    name: 'Маршрутка 101',
                    url: `https://yandex.ru/maps/10754/serpuhov/routes/minibus_101/${hexUriBus101}/`
                }
            ]
        },
        {
            name: 'Пустая форма маршрута',
            url: '/?rtext=',
            title: 'Навигатор онлайн: построение маршрута на карте — Яндекс Карты',
            description:
                'Онлайн-навигация с учетом пробок на Яндекс Картах. Построение маршрута из одной точки в другую на автомобиле, общественном транспорте или пешком на сайте и в мобильном приложении. Яндекс Карты помогут найти улицу, дом, организацию, посмотреть спутниковую карту и панорамы улиц городов.'
        },
        {
            name: 'Форма маршрута с первой выбранной точкой',
            url: '/?rtext=55.733696,37.587814',
            title: 'Навигатор онлайн: построение маршрута на карте — Яндекс Карты',
            description:
                'Онлайн-навигация с учетом пробок на Яндекс Картах. Построение маршрута из одной точки в другую на автомобиле, общественном транспорте или пешком на сайте и в мобильном приложении. Яндекс Карты помогут найти улицу, дом, организацию, посмотреть спутниковую карту и панорамы улиц городов.'
        },
        {
            name: 'Форма маршрута со второй выбранной точкой',
            url: '/?rtext=~55.733696,37.587814',
            title:
                'улица Льва Толстого, 16: как доехать на автомобиле, общественным транспортом или пешком – Яндекс Карты',
            description:
                'улица Льва Толстого, 16: варианты маршрутов с указанием расстояния и времени в пути. Яндекс Карты покажут, как добраться до нужного места на разных видах транспорта или пешком.',
            og: {
                image:
                    'https://static-maps.yandex.ru/1.x/?api_key=01931952-3aef-4eba-951a-8afd26933ad6&theme=light&lang=ru_RU&size=520,440&l=map&lg=0&cr=0&pt=37.587814,55.733696,pm2bm&signature=06Q-0P4fbrx1Ic_lgmSPHEKAMTrhi4k93RHH9ud6r7s='
            }
        },
        {
            name: 'Форма маршрута с двумя точками',
            url: '/?rtext=55.733370,37.587515~55.733696,37.587814',
            title:
                'улица Льва Толстого, 16: как доехать на автомобиле, общественным транспортом или пешком – Яндекс Карты',
            description:
                'улица Льва Толстого, 16: варианты маршрутов с указанием расстояния и времени в пути. Яндекс Карты покажут, как добраться до нужного места на разных видах транспорта или пешком.',
            og: {
                image:
                    'https://static-maps.yandex.ru/1.x/?api_key=01931952-3aef-4eba-951a-8afd26933ad6&theme=light&lang=ru_RU&size=520,440&l=map&lg=0&cr=0&pt=37.587553,55.733386,pm2am~37.587899,55.733546,pm2bm&pl=c:6000ffa0,w:4,bc:ffffff,bw:2,37.587553,55.733386,37.587642,55.733427,37.587703,55.733455,37.587899,55.733546&signature=nV2B4Hgb5pe5Z3a-XNhYqMvXlvk4z9gdkmkZvvYIasw='
            }
        }
    ]
};

export default contents;
