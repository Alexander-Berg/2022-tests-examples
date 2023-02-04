import {SeoFile} from '../types';

const contents: SeoFile = {
    name: 'Лендинг поиска',
    url: '/213/moscow/search/рестораны/',
    canonical: 'https://yandex.ru/maps/213/moscow/search/рестораны/',
    h1: 'Рестораны',
    title: 'Рестораны в Москве на карте рядом со мной: ★ адреса, время работы, отзывы — Яндекс Карты',
    description:
        'Рестораны в Москве, Яндекс Карты: телефоны, часы работы, фото, входы, отзывы, как проехать на транспорте или пройти пешком.',
    og: {
        image:
            'https://static-maps.yandex.ru/1.x/?api_key=01931952-3aef-4eba-951a-8afd26933ad6&theme=light&lang=ru_RU&size=520,440&l=map&spn=0.072704,0.054856&ll=37.623200,55.756414&lg=0&cr=0&pt=37.630010,55.766242,round~37.627366,55.756200,round~37.592319,55.779562,round~37.634981,55.743881,round~37.644584,55.733268,round~37.654081,55.754447,round~37.612977,55.763365,round~37.621516,55.765073,round~37.597562,55.765698,round~37.603144,55.775261,round~37.621607,55.754777,round~37.627263,55.750425,round~37.620157,55.754811,round~37.628645,55.750421,round~37.627566,55.756247,round~37.617114,55.757131,round~37.623473,55.758511,round~37.613862,55.756571,round~37.643695,55.760499,round~37.625089,55.758913,round~37.624310,55.757991,round~37.614125,55.763390,round~37.625863,55.758361,round~37.632278,55.756673,round~37.617988,55.758092,round&signature=d06XwDmCukNKWxtPjj94GGeTndnNBiRhnwj5lunzLoc='
    },
    breadcrumbList: [
        {name: 'Карты', url: 'https://yandex.ru/maps/'},
        {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
        {
            name: 'Рестораны',
            url: `https://yandex.ru/maps/213/moscow/search/${encodeURIComponent('рестораны')}/`
        }
    ],
    alternates: [
        {
            href: 'https://yandex.ru/maps/213/moscow/search/рестораны/',
            hreflang: 'ru'
        },
        {
            href: 'https://yandex.com/maps/213/moscow/search/рестораны/',
            hreflang: 'en'
        }
    ]
};

export default contents;
