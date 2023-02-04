import {getSchemaPropSelector, getSchemaScopeSelector} from '../utils';
import {SeoFile} from '../types';

const contents: SeoFile = {
    name: 'Лендинг дискавери',
    specs: [
        {
            name: 'Обычный',
            url: '/discovery/luchshie-mesta-dlya-uzhina/',
            canonical: 'https://yandex.ru/maps/discovery/luchshie-mesta-dlya-uzhina/',
            h1: 'Лучшие рестораны для праздничного ужина',
            title: 'Лучшие рестораны для романтического ужина в Москве — Яндекс Карты',
            description:
                'Подборка лучших ресторанов для праздничного и романтического ужина в Москве. Знаковые московские места с хорошей кухней и приятной атмосферой. Адреса и описания на карте, смотрите и выбирайте!',
            schemaVerifications: [
                {
                    selector: getSchemaScopeSelector('LocalBusiness'),
                    amount: 15
                },
                ...['name', 'telephone', 'image', 'url', 'address'].map((itemProp) => ({
                    selector: getSchemaPropSelector(itemProp, 'LocalBusiness'),
                    amount: 15
                }))
            ],
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {
                    name: 'Лучшие рестораны для романтического ужина в Москве',
                    url: 'https://yandex.ru/maps/discovery/luchshie-mesta-dlya-uzhina/'
                }
            ],
            og: {
                image:
                    'https://avatars.mds.yandex.net/get-discovery-int/1674621/2a00000168ffead52b585e76ea866eb4dbba/XXXL'
            },
            alternates: [
                {
                    href: 'https://yandex.ru/maps/discovery/luchshie-mesta-dlya-uzhina/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/discovery/luchshie-mesta-dlya-uzhina/',
                    hreflang: 'en'
                }
            ]
        },
        {
            name: 'С измененным каноническом урлом (новый url)',
            url: '/discovery/biblionoch-2019-moscow/',
            canonical: 'https://yandex.ru/maps/discovery/biblionoch-2019-moscow/'
        },
        {
            name: 'С измененным каноническом урлом (устаревший url)',
            url: '/discovery/biblionoch-moscow/',
            canonical: 'https://yandex.ru/maps/discovery/biblionoch-2019-moscow/',
            redirectUrl: '/discovery/biblionoch-2019-moscow/'
        }
    ]
};

export default contents;
