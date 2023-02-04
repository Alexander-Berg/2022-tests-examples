// @ts-expect-error idk
global.BUNDLE_LANG = 'ru';

import React from 'react';
import noop from 'lodash/noop';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';
import { connect } from 'react-redux';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';
import { allure } from '@realty-front/jest-utils/puppeteer/tests-helpers/allure';

import { AppProvider } from 'view/libs/test-helpers';
import reducer, { IProfilePageStore } from 'view/reducers/pages/ProfilePage';

import { IProfileCardBaseProps } from '../ProfileCard.types';
import { ProfileCard } from '../ProfileCard';
import profileCardFiltersStyles from '../ProfileCardOffersFilters/styles.module.css';

import mocks from './mocks';
import offers from './mocks/offers';

advanceTo(new Date('2020-06-01T03:00:00.111Z'));

enum SIZES {
    WITHOUT_OFFERS,
    WITH_MANY_OFFERS,
    WITH_ONE_OFFER,
    SHORT,
}

const WIDTH = {
    PHONE: 375,
    TABLET: 660,
};

const HEIGHTS = {
    [SIZES.WITHOUT_OFFERS]: {
        PHONE: 1000,
        TABLET: 700,
    },
    [SIZES.WITH_MANY_OFFERS]: {
        PHONE: 3000,
        TABLET: 1700,
    },
    [SIZES.WITH_ONE_OFFER]: {
        PHONE: 1350,
        TABLET: 1100,
    },
    [SIZES.SHORT]: {
        PHONE: 500,
        TABLET: 400,
    },
};

const selectors = {
    shorter: '.Shorter__expander',
    phoneButton: '.Button_view_yellow',
    filters: {
        category: `.${profileCardFiltersStyles.category}`,
        type: `.${profileCardFiltersStyles.type}`,
        items: {
            first: '.Select__menu .Menu__item_mode_radio:first-of-type',
            second: '.Select__menu .Menu__item_mode_radio:nth-child(2)',
            last: '.Select__menu .Menu__item_mode_radio:last-of-type',
        },
    },
};

const getResolutions = (size: SIZES) => {
    const phoneResolution = { viewport: { width: WIDTH.PHONE, height: HEIGHTS[size].PHONE } };
    const tabletResolution = { viewport: { width: WIDTH.TABLET, height: HEIGHTS[size].TABLET } };

    return [phoneResolution, tabletResolution];
};

const renderSeveralResolutions = async (
    Component: React.ReactElement,
    dimensions: ReturnType<typeof getResolutions>
) => {
    for (const dimension of dimensions) {
        await render(Component, dimension);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    }
};

const ProfileCardTestContainer = (connect((state: IProfilePageStore) => ({
    profile: state.profiles.profileCard.card!,
    locative: state.geo.locative,
}))(ProfileCard) as unknown) as React.ComponentType<IProfileCardBaseProps>;

// eslint-disable-next-line
const Component: React.FunctionComponent<{ store: any, Gate?: any }> = ({ store, Gate }) => (
    <AppProvider initialState={store} Gate={Gate} rootReducer={reducer}>
        <ProfileCardTestContainer onOfferStats={noop} onPhoneClick={noop} onShowAllOffersClick={noop} />
    </AppProvider>
);

describe('ProfileCard', () => {
    describe('Базовые состояния верстки', () => {
        it('Агентство без офферов', async () => {
            allure.descriptionHtml('В текущем гео нет офферов, в других есть');

            await renderSeveralResolutions(
                <Component store={mocks.defaultAgency} />,
                getResolutions(SIZES.WITHOUT_OFFERS)
            );
        });

        it('Агент без офферов', async () => {
            allure.descriptionHtml('В текущем гео нет офферов, в других есть');

            await renderSeveralResolutions(
                <Component store={mocks.defaultAgent} />,
                getResolutions(SIZES.WITHOUT_OFFERS)
            );
        });

        it('Профиль без офферов по предвыбранным фильтрам', async () => {
            allure.descriptionHtml('В гео есть офферы, но по текущим фильтрам ничего не нашли');

            await renderSeveralResolutions(
                <Component store={mocks.withoutOffers} />,
                getResolutions(SIZES.WITH_ONE_OFFER)
            );
        });

        it('Профиль с 6 офферами по предвыбранным фильтрам', async () => {
            allure.descriptionHtml('В текущем гео есть офферы и в других есть');

            await renderSeveralResolutions(
                <Component store={mocks.withManyOffers} />,
                getResolutions(SIZES.WITH_MANY_OFFERS)
            );
        });

        it('Профиль с 1 оффером по предвыбранным фильтрам', async () => {
            allure.descriptionHtml('В текущем гео все офферы, в других нет');

            await renderSeveralResolutions(
                <Component store={mocks.withOneOffer} />,
                getResolutions(SIZES.WITH_ONE_OFFER)
            );
        });
    });

    describe('Описание', () => {
        it('Не обрезается короткое описание', async () => {
            await render(<Component store={mocks.shortDescription} />, getResolutions(SIZES.WITHOUT_OFFERS)[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('При клике на "Прочитать больше" открывается полное описание', async () => {
            await render(<Component store={mocks.withoutOffers} />, getResolutions(SIZES.WITH_ONE_OFFER)[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.shorter);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Фильтры', () => {
        it('Успешно загружается выдача с новым оффером', async () => {
            allure.descriptionHtml('"Купить" "Коммерческую" -> "Посуточно" "Квартиру"');

            const Gate = {
                create() {
                    return Promise.resolve({ offers: { items: offers.shortRent }, totalOffers: 1 });
                },
            };

            await render(
                <Component store={mocks.sellCommercial} Gate={Gate} />,
                getResolutions(SIZES.WITH_ONE_OFFER)[0]
            );

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.filters.type);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.filters.items.last);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Успешно загружается новая, пустая выдача', async () => {
            allure.descriptionHtml('"Купить" "Квартиру" -> "Купить" "Комнату"');

            const Gate = {
                create() {
                    return Promise.resolve({ offers: { items: [] }, totalOffers: 0 });
                },
            };

            await render(<Component store={mocks.withOneOffer} Gate={Gate} />, getResolutions(SIZES.WITH_ONE_OFFER)[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.filters.category);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.filters.items.second);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Загрузка новых офферов падает с ошибкой', async () => {
            allure.descriptionHtml('Ожидаем, появится заглушка с ошибкой');

            const Gate = {
                create() {
                    return Promise.reject();
                },
            };

            await render(<Component store={mocks.withOneOffer} Gate={Gate} />, getResolutions(SIZES.WITH_ONE_OFFER)[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.filters.category);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.filters.items.second);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Загрузка офферов - состояние, во время загрузки', async () => {
            allure.descriptionHtml('видим паранджу над офферами');

            const Gate = {
                create() {
                    return new Promise(noop);
                },
            };

            await render(<Component store={mocks.withOneOffer} Gate={Gate} />, getResolutions(SIZES.WITH_ONE_OFFER)[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.filters.category);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.filters.items.second);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('Телефоны', () => {
        it('Плавающий телефон когда недоскролили до конца', async () => {
            await render(<Component store={mocks.withOneOffer} />, getResolutions(SIZES.SHORT)[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.evaluate(() => {
                window.scrollBy(0, window.innerHeight);
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.evaluate(() => {
                window.scrollBy(0, window.innerHeight);
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Успешная загрузка одного телефона', async () => {
            const Gate = {
                get() {
                    return Promise.resolve({ phones: [{ wholePhoneNumber: '+79992134916' }] });
                },
            };

            await render(<Component store={mocks.withOneOffer} Gate={Gate} />, getResolutions(SIZES.SHORT)[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.phoneButton);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        describe('Успешная загрузка двух телефонов', () => {
            const Gate = {
                get() {
                    return Promise.resolve({
                        phones: [{ wholePhoneNumber: '+79992134916' }, { wholePhoneNumber: '+74442134916' }],
                    });
                },
            };

            it('Телефон 375px', async () => {
                await render(<Component store={mocks.withOneOffer} Gate={Gate} />, getResolutions(SIZES.SHORT)[0]);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.phoneButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });

            it('Планшет 660px', async () => {
                await render(<Component store={mocks.withManyOffers} Gate={Gate} />, getResolutions(SIZES.SHORT)[1]);

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.click(selectors.phoneButton);

                expect(await takeScreenshot()).toMatchImageSnapshot();
            });
        });

        it('Не удалось загрузить телефоны', async () => {
            const Gate = {
                get() {
                    return Promise.reject();
                },
            };

            await render(<Component store={mocks.withOneOffer} Gate={Gate} />, getResolutions(SIZES.SHORT)[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.phoneButton);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('Состояние, во время загрузки телефонов', async () => {
            const Gate = {
                get() {
                    return new Promise(noop);
                },
            };

            await render(<Component store={mocks.withOneOffer} Gate={Gate} />, getResolutions(SIZES.SHORT)[0]);

            expect(await takeScreenshot()).toMatchImageSnapshot();

            await page.click(selectors.phoneButton);

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
