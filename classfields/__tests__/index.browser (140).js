import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { generateImageAliases, generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import OffersSerpAuthor from '../index';

advanceTo(new Date('2020-06-01T03:00:00.111Z'));

const renderOptions = { viewport: { width: 300, height: 150 } };

const author = {
    id: '0',
    agentName: 'Шумов Борис Валериевич',
    category: 'AGENCY',
    organization: 'ООО "Лимонад"',
    creationDate: '2019-04-01T03:00:00.111Z'
};

const offer = {
    offerType: 'SELL',
    offerCategory: 'APARTMENT',
    offerStatus: 'active',
    offerId: '3584237142849818368',
    price: {
        currency: 'RUR',
        period: 'PER_MONTH',
        value: 6777888,
        valuePerPart: 101163,
        unitPerPart: 'SQUARE_METER',
        photo: '1',
        organization: '1'
    },
    author,
    location: {
        rgid: 176337
    },
    uid: '123'
};

const Component = ({ offer: offerProp }) => (
    <AppProvider
        initialState={{
            user: {
                avatarHost: '123'
            },
            config: {
                yaArendaUrl: 'https://arenda.realty.test.vertis.yandex.ru/'
            }
        }}
        context={{ link: () => 'link' }}
    >
        <OffersSerpAuthor offer={offerProp} />
    </AppProvider>
);

describe('OffersSerpAuthor', () => {
    it.skip('Агент, лого 120x28, длинное имя', async() => {
        await render(
            <Component
                offer={{
                    ...offer,
                    author: {
                        ...author,
                        profile: {
                            userType: 'AGENT',
                            name: 'АИЙИЛЬЦИКЛИКИРМИЦИБАЙРАКТАЗИЙАНКАГРАМАНОГЛУ Круциатус',
                            logo: generateImageAliases({ width: 120, height: 28 })
                        },
                        category: 'AGENT',
                        agentName: 'АИЙИЛЬЦИКЛИКИРМИЦИБАЙРАКТАЗИЙАНКАГРАМАНОГЛУ Круциатус'
                    }
                }}
            />,
            renderOptions
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it.skip('Агенство, лого 120x28, длинное название', async() => {
        await render(
            <Component
                offer={{
                    ...offer,
                    author: {
                        ...author,
                        profile: {
                            userType: 'AGENCY',
                            name: 'Пупкино и Ко',
                            logo: generateImageAliases({ width: 120, height: 28 })
                        },
                        category: 'AGENCY',
                        agentName: 'МГСН - Московская Городская Служба Недвижимости, агентство'
                    },
                    salesDepartment: {
                        logo: generateImageUrl({ width: 120, height: 28 })
                    }
                }}
            />,
            renderOptions
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    describe('сервис яндекса', () => {
        it('яндекс аренда', async() => {
            await render(
                <Component
                    offer={{
                        ...offer,
                        yandexRent: true,
                        author: {
                            ...author,
                            category: 'AGENCY',
                            photo: generateImageUrl(),
                            organization: 'Яндекс Аренда',
                            agentName: 'Яндекс Аренда'
                        }
                    }}
                />,
                renderOptions
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
