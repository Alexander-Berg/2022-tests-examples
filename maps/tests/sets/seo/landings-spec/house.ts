import {getSchemaPropSelector, getSchemaScopeSelector} from '../utils';
import {SeoFile} from '../types';

const contents: SeoFile = {
    name: 'Лендинг дома',
    specs: [
        {
            name: 'Обычный',
            url: '/213/moscow/house/ulitsa_lva_tolstogo_16/Z04Ycw9nSUwEQFtvfXtycnVlbQ==/',
            canonical: 'https://yandex.ru/maps/213/moscow/house/ulitsa_lva_tolstogo_16/Z04Ycw9nSUwEQFtvfXtycnVlbQ==/',
            h1: 'улица Льва Толстого, 16',
            title: 'Улица Льва Толстого, 16 на карте Москвы, ближайшее метро Парк культуры — Яндекс Карты',
            description:
                'Проложить маршрут на карте Улица Льва Толстого, 16: организации в здании, почтовый индекс, описание, фотографии и отзывы на все заведения в доме',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/geo/moskva/53166393/'},
                {name: 'Район Хамовники', url: 'https://yandex.ru/maps/213/moscow/geo/rayon_khamovniki/53211698/'},
                {
                    name: 'Улица Льва Толстого, 16',
                    url: 'https://yandex.ru/maps/213/moscow/house/ulitsa_lva_tolstogo_16/Z04Ycw9nSUwEQFtvfXtycnVlbQ==/'
                }
            ],
            og: {
                title: 'Россия, Москва, улица Льва Толстого, 16, 119021',
                description:
                    'Проложить маршрут на карте Улица Льва Толстого, 16: организации в здании, почтовый индекс, описание, фотографии и отзывы на все заведения в доме'
            },
            alternates: [
                {
                    href:
                        'https://yandex.ru/maps/213/moscow/house/ulitsa_lva_tolstogo_16/Z04Ycw9nSUwEQFtvfXtycnVlbQ==/',
                    hreflang: 'ru'
                },
                {
                    href:
                        'https://yandex.com/maps/213/moscow/house/ulitsa_lva_tolstogo_16/Z04Ycw9nSUwEQFtvfXtycnVlbQ==/',
                    hreflang: 'en'
                }
            ]
        },
        {
            name: 'Лендинг панорамы',
            url: '/213/moscow/house/ulitsa_rossolimo_11s1/Z04Ycw9hTUMHQFtvfXtydXVlZg==/panorama/',
            canonical:
                'https://yandex.ru/maps/213/moscow/house/ulitsa_rossolimo_11s1/Z04Ycw9hTUMHQFtvfXtydXVlZg==/panorama/',
            h1: 'улица Россолимо, 11с1',
            title: 'Панорама: Улица Россолимо, 11с1 на карте Москвы — Яндекс Карты',
            description: 'Панорамы адреса Улица Россолимо, 11с1 от Яндекс Карт',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/geo/moskva/53166393/'},
                {name: 'Район Хамовники', url: 'https://yandex.ru/maps/213/moscow/geo/rayon_khamovniki/53211698/'},
                {
                    name: 'Улица Россолимо, 11с1',
                    url: 'https://yandex.ru/maps/213/moscow/house/ulitsa_rossolimo_11s1/Z04Ycw9hTUMHQFtvfXtydXVlZg==/'
                }
            ]
        },
        {
            name: 'Лендинг фото',
            url: '/213/moscow/house/ulitsa_rossolimo_11s1/Z04Ycw9hTUMHQFtvfXtydXVlZg==/gallery/',
            canonical:
                'https://yandex.ru/maps/213/moscow/house/ulitsa_rossolimo_11s1/Z04Ycw9hTUMHQFtvfXtydXVlZg==/gallery/',
            h1: 'улица Россолимо, 11с1',
            title: 'Фото: Улица Россолимо, 11с1 на карте Москвы — Яндекс Карты',
            description: 'Фотографии адреса Улица Россолимо, 11с1 от пользователей Яндекс Карт',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/geo/moskva/53166393/'},
                {name: 'Район Хамовники', url: 'https://yandex.ru/maps/213/moscow/geo/rayon_khamovniki/53211698/'},
                {
                    name: 'Улица Россолимо, 11с1',
                    url: 'https://yandex.ru/maps/213/moscow/house/ulitsa_rossolimo_11s1/Z04Ycw9hTUMHQFtvfXtydXVlZg==/'
                }
            ]
        },
        {
            name: 'Лендинг организаций внутри',
            url: '/213/moscow/house/yazykovskiy_pereulok_5k4/Z04Ycw9iTEABQFtvfXtycXVnbA==/inside/',
            canonical:
                'https://yandex.ru/maps/213/moscow/house/yazykovskiy_pereulok_5k4/Z04Ycw9iTEABQFtvfXtycXVnbA==/inside/',
            title: 'Организации внутри Языковский переулок, 5к4 — Яндекс Карты',
            description: 'Все организации внутри Языковский переулок, 5к4 — Яндекс Карты',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/geo/moskva/53166393/'},
                {name: 'Район Хамовники', url: 'https://yandex.ru/maps/213/moscow/geo/rayon_khamovniki/53211698/'},
                {
                    name: 'Языковский переулок, 5к4',
                    url:
                        'https://yandex.ru/maps/213/moscow/house/yazykovskiy_pereulok_5k4/Z04Ycw9iTEABQFtvfXtycXVnbA==/'
                }
            ]
        },
        {
            name: 'Без поля region берем регион из URL',
            url: '/213/moscow/house/ulitsa_lenina_34/Z08YcgZiSUcDQFtpfX15c3hkZA==/',
            canonical: 'https://yandex.ru/maps/213/moscow/house/ulitsa_lenina_34/Z08YcgZiSUcDQFtpfX15c3hkZA==/',
            title: 'Улица Ленина, 34 на карте Москвы — Яндекс Карты',
            description:
                'Проложить маршрут на карте Улица Ленина, 34: организации в здании, почтовый индекс, описание, фотографии и отзывы на все заведения в доме',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {
                    name: 'Улица Ленина, 34',
                    url: 'https://yandex.ru/maps/213/moscow/house/ulitsa_lenina_34/Z08YcgZiSUcDQFtpfX15c3hkZA==/'
                }
            ]
        },
        {
            name: 'Вдалеке от метро',
            url: '/157/minsk/house/undefined/Zk4YcwNlQUEGQFtpfXV0dnxmbA==/',
            title: 'Улица Выготского, 16 на карте Минска — Яндекс Карты',
            schemaVerifications: [
                {
                    selector: getSchemaScopeSelector('Place'),
                    amount: 1
                },
                {
                    selector: getSchemaPropSelector('name', 'Place'),
                    value: 'улица Выготского, 16'
                },
                {
                    selector: getSchemaPropSelector('description', 'Place'),
                    content: 'улица Выготского, 16, микрорайон Новинки, Минск, Беларусь'
                },
                {
                    selector: getSchemaPropSelector('image', 'Place'),
                    content: /data:image\/png;base64/
                }
            ],
            redirectUrl: '/157/minsk/house/Zk4YcwNlQUEGQFtpfXV0dnxmbA==/'
        }
    ]
};

export default contents;
