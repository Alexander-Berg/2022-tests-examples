import React from 'react';
import { render } from 'jest-puppeteer-react';
import merge from 'lodash/merge';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { IAppProviderProps } from 'realty-core/view/react/libs/test-helpers';
import { ExtendedUserTypes } from 'realty-core/types/common';

import { AppProvider } from 'view/lib/test-helpers';
import 'view/deskpad/common.css';

import { ARENDA_HIDE_COOKIE } from '../components/ArendaBanner';
import PromoBanners from '../index';

const SCREENS = [
    [1000, 250],
    [1400, 250],
];

const getState = (stateOverrides = {}) => {
    return merge(
        {
            vosUserData: {
                userType: 'OWNER',
            },
            user: {
                isJuridical: false,
                userType: 'OWNER',
                isVosUser: true,
            },
            geo: {
                rgid: 741964,
                parents: [{ rgid: 741964 }],
            },
        },
        stateOverrides
    );
};

const Component: React.FunctionComponent<Partial<IAppProviderProps>> = ({ initialState }) => (
    <AppProvider initialState={initialState}>
        <div style={{ padding: '12px' }}>
            <PromoBanners />
        </div>
    </AppProvider>
);

const renderOnScreens = async (component: React.ReactElement) => {
    for (const [WIDTH, HEIGHT] of SCREENS) {
        await render(component, { viewport: { width: WIDTH, height: HEIGHT } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    }
};

advanceTo('2020-02-11 16:22:00');

describe('PromoBanners', () => {
    describe('Реферральная скидка', () => {
        it('Базовая отрисовка собственник', async () => {
            const store = getState({
                vosUserData: {
                    extendedUserType: ExtendedUserTypes.OWNER,
                    status: 'active',
                },
                cookies: {
                    'chats-hide-banner': '1',
                },
                geo: {
                    isSpb: true,
                },
            });
            const component = <Component initialState={store} />;

            await renderOnScreens(component);
        });

        it('Новосибирская область', async () => {
            const store = getState({
                vosUserData: {
                    extendedUserType: ExtendedUserTypes.OWNER,
                    status: 'active',
                },
                cookies: {
                    'chats-hide-banner': '1',
                },
                geo: {
                    isInNovosibirskObl: true,
                },
            });
            const component = <Component initialState={store} />;

            await renderOnScreens(component);
        });

        it('Свердловская область', async () => {
            const store = getState({
                vosUserData: {
                    extendedUserType: ExtendedUserTypes.OWNER,
                    status: 'active',
                },
                cookies: {
                    'chats-hide-banner': '1',
                },
                geo: {
                    isInSverdObl: true,
                },
            });
            const component = <Component initialState={store} />;

            await renderOnScreens(component);
        });
    });

    describe('Скидка на VAS', () => {
        it('Базовая отрисовка', async () => {
            const store = getState({
                discountInfo: {
                    vas: {
                        endDate: '2020-02-13 12:22:00',
                        percent: 70,
                    },
                },
                cookies: {
                    'chats-hide-banner': '1',
                },
            });
            const component = <Component initialState={store} />;

            await renderOnScreens(component);
        });
    });

    describe('Чаты', () => {
        it('Базовая отрисовка', async () => {
            const store = getState({
                cookies: {
                    [ARENDA_HIDE_COOKIE]: 1,
                },
            });

            const component = <Component initialState={store} />;

            await renderOnScreens(component);
        });
    });

    describe('Аренда', () => {
        it('Базовая отрисовка', async () => {
            const store = getState({
                offersNew: {
                    offers: [
                        {
                            category: 'APARTMENT',
                            offerType: 'RENT',
                            period: 'PER_MONTH',
                            location: {
                                subjectFederationId: 1,
                            },
                        },
                    ],
                },
                vosUserData: {
                    extendedUserType: 'OWNER',
                },
            });
            const component = <Component initialState={store} />;

            await renderOnScreens(component);
        });
    });

    describe('Скидка на размещение - акция 1 рубль', () => {
        it('Базовая отрисовка. Спб', async () => {
            const store = getState({
                discountInfo: {
                    placement: {
                        endDate: '2022-04-01 16:22:00',
                        amount: 1,
                    },
                },
                cookies: {
                    'chats-hide-banner': '1',
                },
                geo: {
                    isSpb: true,
                },
            });
            const component = <Component initialState={store} />;

            await renderOnScreens(component);
        });

        it('Базовая отрисовка. Москва', async () => {
            const store = getState({
                discountInfo: {
                    placement: {
                        endDate: '2022-04-01 16:22:00',
                        amount: 1,
                    },
                },
                cookies: {
                    'chats-hide-banner': '1',
                },
                geo: {
                    isMsk: true,
                },
            });

            const component = <Component initialState={store} />;

            await renderOnScreens(component);
        });

        it('Базовая отрисовка. Регионы', async () => {
            const store = getState({
                discountInfo: {
                    placement: {
                        endDate: '2021-04-01 16:22:00',
                        amount: 1,
                    },
                },
                cookies: {
                    'chats-hide-banner': '1',
                },
                geo: {
                    isInVoronezhObl: true,
                },
            });

            const component = <Component initialState={store} />;

            await renderOnScreens(component);
        });
    });

    describe('Баннер после снятия оффера', () => {
        it('Базовая отрисовка', async () => {
            const store = getState({
                offersNew: {
                    offers: [
                        {
                            status: 'inactive',
                            offerType: 'RENT',
                            period: 'PER_MONTH',
                            category: 'APARTMENT',
                            rentPolygon: {
                                isPointInsidePolygon: true,
                            },
                        },
                    ],
                },
            });
            const component = <Component initialState={store} />;

            await renderOnScreens(component);
        });
    });
});
