import React from 'react';
import { render } from 'jest-puppeteer-react';
import { advanceTo } from 'jest-date-mock';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProviders } from 'view/libs/test-helpers/AppProviders';

import ClientTariffsContainer from '../container';

advanceTo(new Date('2019-11-06T03:00:00.111Z'));

const defaultStoreMock = {
    config: {
        serverTime: Date.now(),
        timeDelta: 0
    },
    user: { permissions: [ 'tuz_extended_tariff_disable' ] },
    client: {
        tariffs: {
            data: {
                realty3: {
                    uid: '1312312'
                },
                plannedAction: {},
                callStats: {},
                tariff: 'extended',
                priceLists: [],
                individualPriceLists: [],
                individualPricingOptions: {
                    from: '',
                    to: '',
                    priceListName: undefined
                },
                individualPlacementDiscounts: [],
                individualPlacementDiscountsOptions: {
                    from: '',
                    to: '',
                    discount: undefined
                }
            },
            network: {
                fetchClientTariffsStatus: 'loaded',
                disableExtendedTariffStatus: 'loaded',
                bindIndividualPriceListStatus: 'loaded',
                deleteIndividualPricingStatus: 'loaded',
                bindIndividualPlacementDiscountStatus: 'loaded',
                deleteIndividualPlacementDiscountStatus: 'loaded'
            }
        }
    }
};

const context = {
    router: {
        entries: [ { page: 'clientTariffs', params: { clientId: '1337' } } ]
    }
};

const Component = ({ store }) => (
    <AppProviders store={store} context={context}>
        <ClientTariffsContainer />
    </AppProviders>
);

describe('ClientTariffs', () => {
    it('correct draw with extended tariff', async() => {
        await render(<Component store={defaultStoreMock} />, { viewport: { width: 750, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw with minimum tariff', async() => {
        const store = {
            ...defaultStoreMock,
            client: {
                tariffs: {
                    ...defaultStoreMock.client.tariffs,
                    data: {
                        ...defaultStoreMock.client.tariffs.data,
                        tariff: 'minimum'
                    }
                }
            }
        };

        await render(<Component store={store} />, { viewport: { width: 750, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw with maximum tariff', async() => {
        const store = {
            ...defaultStoreMock,
            client: {
                tariffs: {
                    ...defaultStoreMock.client.tariffs,
                    data: {
                        ...defaultStoreMock.client.tariffs.data,
                        tariff: 'maximum'
                    }
                }
            }
        };

        await render(<Component store={store} />, { viewport: { width: 750, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw with enabled maximum tariff with planned switching to extended', async() => {
        const store = {
            ...defaultStoreMock,
            client: {
                tariffs: {
                    ...defaultStoreMock.client.tariffs,
                    data: {
                        ...defaultStoreMock.client.tariffs.data,
                        tariff: 'maximum',
                        plannedAction: {
                            action: 'SET_EXTENDED_TUZ',
                            plannedTime: '2019-11-09T02:58:51.111Z'
                        }
                    }
                }
            }
        };

        await render(<Component store={store} />, { viewport: { width: 750, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
    it('correct draw disabled button when planned switching to extended less than day left', async() => {
        const store = {
            ...defaultStoreMock,
            client: {
                tariffs: {
                    ...defaultStoreMock.client.tariffs,
                    data: {
                        ...defaultStoreMock.client.tariffs.data,
                        tariff: 'maximum',
                        plannedAction: {
                            action: 'SET_EXTENDED_TUZ',
                            plannedTime: '2019-11-07T02:58:51.111Z'
                        }
                    }
                }
            }
        };

        await render(<Component store={store} />, { viewport: { width: 750, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw disabled button when planned switching to minimum less than day left', async() => {
        const store = {
            ...defaultStoreMock,
            client: {
                tariffs: {
                    ...defaultStoreMock.client.tariffs,
                    data: {
                        ...defaultStoreMock.client.tariffs.data,
                        tariff: 'maximum',
                        plannedAction: {
                            action: 'TURN_OFF',
                            plannedTime: '2019-11-07T02:58:51.111Z'
                        }
                    }
                }
            }
        };

        await render(<Component store={store} />, { viewport: { width: 750, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw when is missing permission to disable', async() => {
        const store = {
            ...defaultStoreMock,
            user: {
                permissions: []
            }
        };

        await render(<Component store={store} />, { viewport: { width: 750, height: 300 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('correct draw call stats', async() => {
        const store = {
            ...defaultStoreMock,
            client: {
                ...defaultStoreMock.client,
                tariffs: {
                    ...defaultStoreMock.client.tariffs,
                    data: {
                        ...defaultStoreMock.client.tariffs.data,
                        callStats: {
                            total: 10,
                            success: 20,
                            target: 30,
                            nonTarget: 40,
                            missed: 50,
                            blocked: 60,
                            payedTuz: 70
                        }
                    }
                }
            }
        };

        await render(<Component store={store} />, { viewport: { width: 750, height: 500 } });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
