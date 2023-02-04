import {SeoFile} from '../types';

const contents: SeoFile = {
    name: 'Сео-эксперименты.',
    specs: [
        {
            name: 'Дом с валидным конфигом',
            mockVersion: 'seoExpValidConfig',
            url: '/213/moscow/house/ulitsa_lva_tolstogo_16/Z04Ycw9nSUwEQFtvfXtycnVlbQ==/',
            canonical: 'https://yandex.ru/maps/213/moscow/house/ulitsa_lva_tolstogo_16/Z04Ycw9nSUwEQFtvfXtycnVlbQ==/',
            title:
                '[GROUP_1 title] Улица Льва Толстого, 16 на карте Москвы, ближайшее метро Парк культуры — Яндекс.Карты',
            description:
                '[GROUP_1 description] Проложить маршрут на карте Улица Льва Толстого, 16: организации в здании, почтовый индекс, описание, фотографии и отзывы на все заведения в доме',
            og: {
                title: '[GROUP_1 og.title] Россия, Москва, улица Льва Толстого, 16, 119021'
            }
        },
        {
            name: 'Конфиг группы со сломанными переводами не применяется',
            // Конфиг валидный, в этой группе сломан перевод, ожидаем что для группы не применятся переопределения.
            mockVersion: 'seoExpBrokenTranslationGroups',
            url: '/213/moscow/house/ulitsa_ostozhenka_38s2/Z04Ycw5kSUIFQFtvfXtyeH1hZw==/',
            canonical: 'https://yandex.ru/maps/213/moscow/house/ulitsa_ostozhenka_38s2/Z04Ycw5kSUIFQFtvfXtyeH1hZw==/',
            title: 'Улица Остоженка, 38с2 на карте Москвы, ближайшее метро Парк культуры — Яндекс Карты',
            description:
                'Проложить маршрут на карте Улица Остоженка, 38с2: организации в здании, почтовый индекс, описание, фотографии и отзывы на все заведения в доме',
            og: {
                title: 'Россия, Москва, улица Остоженка, 38с2, 119034'
            }
        },
        {
            name: 'Конфиг группы без сломанных переводов применяется, даже если соседний сломан',
            // Конфиг валидный, в соседней группе сломан перевод, ожидаем что для группы применятся переопределения.
            mockVersion: 'seoExpBrokenTranslationGroups',
            url: '/213/moscow/house/pugovishnikov_pereulok_2/Z04Ycw9mQEEOQFtvfXtycn5mYA==/',
            canonical: 'https://yandex.ru/maps/213/moscow/house/pugovishnikov_pereulok_2/Z04Ycw9mQEEOQFtvfXtycn5mYA==/',
            title:
                '[GROUP_2 title] Пуговишников переулок, 2 на карте Москвы, ближайшее метро Парк культуры — Яндекс.Карты',
            description:
                '[GROUP_2 description] Проложить маршрут на карте Пуговишников переулок, 2: организации в здании, почтовый индекс, описание, фотографии и отзывы на все заведения в доме',
            og: {
                title: '[GROUP_2 og.title] Россия, Москва, Пуговишников переулок, 2, 119021'
            }
        },
        {
            name: 'Опечатка/несуществующий параметр перевода в конфиге',
            // Тест нужен чтобы убедиться что мы не отдаем undefined в строке перевода.
            mockVersion: 'seoExpConfigTranslateParamTypo1',
            url: '/213/moscow/house/kropotkinskiy_pereulok_7s3/Z04Ycw5iQEUCQFtvfXtydnpkbQ==/',
            canonical:
                'https://yandex.ru/maps/213/moscow/house/kropotkinskiy_pereulok_7s3/Z04Ycw5iQEUCQFtvfXtydnpkbQ==/',
            title: 'Кропоткинский переулок, 7с3 на карте Москвы, ближайшее метро Парк культуры — Яндекс Карты',
            description:
                '[GROUP_1 description] Проложить маршрут на карте Кропоткинский переулок, 7с3: организации в здании, почтовый индекс, описание, фотографии и отзывы на все заведения в доме',
            og: {
                title: '[GROUP_1 og.title] Россия, Москва, Кропоткинский переулок, 7с3, 119034'
            }
        },
        {
            name: 'patternMatches работает в лендинге',
            mockVersion: 'seoExpWithPattern',
            url: '/213/moscow/house/bolshoy_palashyovskiy_pereulok_9s2/Z04Ycw5pS0MCQFtvfXt3dXtkYA==/',
            canonical:
                'https://yandex.ru/maps/213/moscow/house/bolshoy_palashyovskiy_pereulok_9s2/Z04Ycw5pS0MCQFtvfXt3dXtkYA==/',
            title: '[GROUP_2] Большой Палашёвский переулок, 9с2 на карте Москвы — Яндекс.Карты'
        },
        {
            name: 'patternMatches сохраняет приоритет за urls',
            mockVersion: 'seoExpWithPattern',
            url: '/213/moscow/house/ulitsa_ostozhenka_38s2/Z04Ycw5kSUIFQFtvfXtyeH1hZw==/',
            canonical: 'https://yandex.ru/maps/213/moscow/house/ulitsa_ostozhenka_38s2/Z04Ycw5kSUIFQFtvfXtyeH1hZw==/',
            title: '[GROUP_1] Улица Остоженка, 38с2 на карте Москвы — Яндекс.Карты'
        },
        {
            name: 'Пустая строка убирает контент у перевода',
            mockVersion: 'seoExpWithEmptyString1',
            url: '/213/moscow/house/ulitsa_ostozhenka_38s2/Z04Ycw5kSUIFQFtvfXtyeH1hZw==/',
            canonical: 'https://yandex.ru/maps/213/moscow/house/ulitsa_ostozhenka_38s2/Z04Ycw5kSUIFQFtvfXtyeH1hZw==/',
            description: ''
        }
    ]
};

export default contents;
