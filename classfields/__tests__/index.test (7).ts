import { h1, title, description } from '../index';

import mocks from './mocks/index.ts';

const defaultMocks = {
    ...mocks,
    locative: 'во Фрязино',
    subjectFederationLocative: 'в Московской области',
    userType: 'AGENCY',
};

const mockWithMoscowGeo = {
    ...mocks,
    userType: 'AGENCY',
    locative: 'в Москве',
    subjectFederationLocative: 'в Москве и МО',
};

const mockWithoutSubjFederation = {
    ...mocks,
    locative: 'в Московской области',
    subjectFederationRgidInfo: undefined,
    userType: 'AGENT',
};

/* eslint-disable max-len */
describe('Сео тексты для агентов', () => {
    describe('Для агенств с rgid + субъектом Федерации', () => {
        test('Генеранция заголовка', () => {
            const profileSearchTitle = title(defaultMocks);
            expect(profileSearchTitle).toBe(
                'Агентства недвижимости во Фрязино в Московской области - контакты, отзывы и все объекты недвижимости от агентств - Яндекс Недвижимость!'
            );
        });

        test('Генерация description', () => {
            const profileSearchDescription = description(defaultMocks);

            expect(profileSearchDescription).toBe(
                'Яндекс Недвижимость - полная база квартир, домов и других объектов недвижимости в аренду или на продажу от агентств недвижимости во Фрязино в Московской области'
            );
        });

        test('Генерация h1', () => {
            const profileSearchH1 = h1(defaultMocks);

            expect(profileSearchH1).toBe('Все агентства во Фрязино в Московской области');
        });
    });

    describe('Для агентсв со словом в городе, который уже есть в Субъекте Федерации (e.g. Москва и Москва и МО)', () => {
        test('Генеранция заголовка', () => {
            const profileSearchTitle = title(mockWithMoscowGeo);

            expect(profileSearchTitle).toBe(
                'Агентства недвижимости в Москве и МО - контакты, отзывы и все объекты недвижимости от агентств - Яндекс Недвижимость!'
            );
        });

        test('Генерация description', () => {
            const profileSearchDescription = description(mockWithMoscowGeo);

            expect(profileSearchDescription).toBe(
                'Яндекс Недвижимость - полная база квартир, домов и других объектов недвижимости в аренду или на продажу от агентств недвижимости в Москве и МО'
            );
        });

        test('Генерация h1', () => {
            const profileSearchH1 = h1(mockWithMoscowGeo);

            expect(profileSearchH1).toBe('Все агентства в Москве и МО');
        });
    });

    describe('Для агентов в субъекте Федерации', () => {
        test('Генеранция заголовка', () => {
            const profileSearchTitle = title(mockWithoutSubjFederation);

            expect(profileSearchTitle).toBe(
                'Агенты по недвижимости в Московской области - контакты, отзывы и все объекты недвижимости от агентов - Яндекс Недвижимость!'
            );
        });

        test('Генерация description', () => {
            const profileSearchDescription = description(mockWithoutSubjFederation);

            // eslint-disable-next-line max-len
            expect(profileSearchDescription).toBe(
                'Яндекс Недвижимость - полная база квартир, домов и других объектов недвижимости в аренду или на продажу от агентов по недвижимости в Московской области'
            );
        });

        test('Генерация h1', () => {
            const profileSearchH1 = h1(mockWithoutSubjFederation);

            expect(profileSearchH1).toBe('Все агенты в Московской области');
        });
    });
});
