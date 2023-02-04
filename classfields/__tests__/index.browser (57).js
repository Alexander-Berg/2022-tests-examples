import React from 'react';
import omit from 'lodash/omit';
import { advanceTo } from 'jest-date-mock';
import { render as _render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import Gate from 'realty-core/view/react/libs/gate';

import { AppProvider } from 'view/lib/test-helpers';
import { trustStatus } from 'view/lib/trusted-owner';
import 'view/deskpad/common.css';

import OwnerOfferPreview from '../index';

import { getState, getDraft, getOffer, getOffers, productsWithDiscount } from './mock';

advanceTo(new Date('2020-06-01T03:00:00.111Z'));

// eslint-disable-next-line no-undef
global.BUNDLE_LANG = 'ru';

const SIZES = [
    [ 1280, 800 ],
    [ 1000, 800 ]
];

Gate.create = () => {
    return new Promise(resolve => {
        setTimeout(() => {
            resolve();
        }, 200);
    });
};

const render = async renderComponent => {
    // eslint-disable-next-line guard-for-in,no-unused-vars
    for (const size of SIZES) {
        const [ width, height ] = size;

        // eslint-disable-next-line no-await-in-loop
        await _render(renderComponent, { viewport: { width, height } });
        // eslint-disable-next-line no-await-in-loop
        expect(await takeScreenshot()).toMatchImageSnapshot();
    }
};

const Component = ({ store, ...props }) => (
    <div style={{ padding: '10px' }}>
        <AppProvider initialState={store}>
            <OwnerOfferPreview
                togglePhotoPanel={() => {}}
                isAddPhotoPanelOpened={false}
                readOnly={false}
                fromCallCenter={false}
                {...props}
            />
        </AppProvider>
    </div>
);

describe('OwnerOfferPreview', () => {
    describe('Цена', () => {
        it('Отрисовка по умолчанию', async() => {
            const state = getState();
            const offer = getOffer();

            await render(<Component offer={offer} store={state} />);
        });

        it('Черновик с нулевой ценой', async() => {
            const state = getState();
            const offer = getDraft();

            await render(<Component offer={offer} store={state} />);
        });

        it('Цена в евро', async() => {
            const state = getState();
            const offer = getOffer({
                currency: 'EUR'
            });

            await render(<Component offer={offer} store={state} />);
        });

        it('Цена в долларах', async() => {
            const state = getState();
            const offer = getOffer({
                currency: 'USD'
            });

            await render(<Component offer={offer} store={state} />);
        });
    });

    describe('Скидки', () => {
        it('Скидки на VAS', async() => {
            const state = getState();
            const offer = getOffer({
                services: productsWithDiscount
            });

            await render(<Component offer={offer} store={state} />);
        });
    });

    describe('Сниппет', () => {
        const state = getState();

        describe('placement === free', () => {
            const offers = getOffers({
                placement: {
                    free: {}
                }
            });

            it.each(Object.keys(offers))('%s', async key => {
                const offer = offers[key];

                await render(<Component offer={offer} store={state} />);
            });
        });

        describe('placement === quota', () => {
            const offers = getOffers({
                placement: {
                    quota: {}
                }
            });

            it.each(Object.keys(offers))('%s', async key => {
                const offer = offers[key];

                await render(<Component offer={offer} store={state} />);
            });
        });

        describe('placement === paymentRequired.paid == false', () => {
            const offers = getOffers({
                placement: {
                    paymentRequired: {
                        paid: false
                    }
                }
            });

            it.each(Object.keys(offers))('%s', async key => {
                const offer = offers[key];

                await render(<Component offer={offer} store={state} />);
            });
        });

        describe('placement === paymentRequired.paid == true', () => {
            const offers = getOffers({
                placement: {
                    paymentRequired: {
                        paid: true
                    }
                },
                services: {
                    placement: {
                        priceContext: {
                            isAvailable: true,
                            effective: 47,
                            base: 47,
                            reasons: [],
                            modifiers: {}
                        },
                        description: {
                            duration: 30,
                            description: 'Платное размещение'
                        },
                        isChangingStatus: false,
                        isChangingNotCancelable: false,
                        isWaitingForDeactivation: false,
                        status: 'active',
                        renewal: {
                            status: 'ACTIVE'
                        },
                        isAppliedFromFeed: false,
                        end: 1599224442488
                    }
                }
            });

            it.each(Object.keys(offers))('%s', async key => {
                const offer = offers[key];

                await render(<Component offer={offer} store={state} />);
            });
        });
    });

    describe('бэйдж проверенного собственника', () => {
        describe('продажа', () => {
            it.each(Object.keys(omit(trustStatus, 'NOT_LINKED_MOSRU')))('отрисовка для статуса %s', async status => {
                const state = getState();
                const offer = getOffer({
                    trustedOwnerInfo: {
                        ownerTrustedStatus: status
                    }
                });

                await _render(<Component offer={offer} store={state} />, {
                    viewport:
                        { width: 1000, height: 600 }
                });

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.hover('.owner-offer-preview-info-panel__badge');

                await page.waitFor(300);

                expect(await takeScreenshot({
                    keepCursor: true
                })).toMatchImageSnapshot();
            });
        });

        describe('аренда', () => {
            it.each(Object.keys(omit(trustStatus, 'NOT_LINKED_MOSRU')))('отрисовка для статуса %s', async status => {
                const state = getState();
                const offer = getOffer({
                    trustedOwnerInfo: {
                        ownerTrustedStatus: status
                    },
                    offerType: 'RENT',
                    period: 'PER_MONTH'
                });

                await _render(<Component offer={offer} store={state} />, {
                    viewport:
                        { width: 1000, height: 600 }
                });

                expect(await takeScreenshot()).toMatchImageSnapshot();

                await page.hover('.owner-offer-preview-info-panel__badge');

                await page.waitFor(300);

                expect(await takeScreenshot({
                    keepCursor: true
                })).toMatchImageSnapshot();
            });
        });
    });
});
