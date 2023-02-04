import { DeepPartial } from 'utility-types';

import { IMicrodataCoreStore } from 'realty-core/view/react/common/components/Microdata/types';

/* eslint-disable max-len */
const mockSiteCard = {
    name: 'ЖК «Октябрьское поле»',
    fullName: 'ЖК «Октябрьское поле»',
    locativeFullName: 'ЖК «Октябрьское поле»',
    salesDepartment: {
        id: 123,
        name: 'РГ-Девелопмент',
    },
    resaleTotalOffers: 0,
    timestamp: 0,
    isFromPik: false,
    location: {
        geoId: 213,
        rgid: 193295,
        settlementRgid: 587795,
        settlementGeoId: 213,
        populatedRgid: 741964,
        address: 'Москва, Кронштадтский бул., 6, к. 1-6',
        subjectFederationId: 1,
        subjectFederationRgid: 741964,
        subjectFederationName: 'Москва и МО',
        point: {
            latitude: 55.841267,
            longitude: 37.49443,
            precision: 'EXACT',
        },
    },
    regionInfo: {
        rgid: 587795,
        name: 'Москва',
        locative: 'в Москве',
        populatedRgid: 587795,
        isInLO: false,
        isInMO: true,
        parents: [],
    },
    description: 'Проект, общей площадью 188 тыс. кв. м.',
    id: 189856,
    siteSpecialProposals: [
        {
            shortDescription: 'Ипотека 2.9%',
            fullDescription:
                'Ипотеку предоставляет банк ВТБ (ПАО). Ставка 2.9% на весь срок кредита. Первоначальный взнос от 20%, ' +
                'срок кредита от 1 до 30 лет. Программа действует при покупке апартаментов у партнёра банка ВТБ (ПАО) в Москве и Московской обл. ' +
                'Подробная информация на сайтах samolet.ru. и наш.дом.рф. Генеральная лицензия банка №1000. Предложение застройщика действует до 31.12.2021',
        },
        {
            shortDescription: 'Скидка 110 000 руб.',
            fullDescription:
                'При покупке онлайн предоставляется скидка 110000 руб. Не суммируется со скидками и акциями.',
        },
        {
            shortDescription: 'Трейд-ин',
            fullDescription:
                'Простой способ обменять вашу старую квартиру на новую от Группы «Самолет». Обмен происходит за 60 дней. Действует в 150 городах России.',
        },
    ],
};

export const mockState: DeepPartial<IMicrodataCoreStore> = {
    cards: {
        sites: mockSiteCard,
    },
    page: {
        name: 'newbuilding-mortgage',
    },
};
