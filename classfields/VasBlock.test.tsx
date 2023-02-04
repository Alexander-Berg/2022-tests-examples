jest.mock('../../dataDomain/state/actions/openModalDialog', () => {
    return jest.fn(() => () => { });
});
jest.mock('auto-core/react/dataDomain/sales/actions/toggleServiceAutoProlongation', () => ({
    fetchAndToggleServiceAutoProlongation: jest.fn(() => () => { }),
}));
jest.mock('auto-core/react/dataDomain/card/actions/toggleAutoProlongation');
jest.mock('auto-core/react/dataDomain/card/actions/billingSchedulesPut');
jest.mock('auto-core/react/dataDomain/card/actions/billingSchedulesRemove');

import React from 'react';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import * as offerActions from 'auto-core/react/dataDomain/sales/actions/toggleServiceAutoProlongation';
import toggleAutoProlongation from 'auto-core/react/dataDomain/card/actions/toggleAutoProlongation';
import billingSchedulesPut from 'auto-core/react/dataDomain/card/actions/billingSchedulesPut';
import billingSchedulesRemove from 'auto-core/react/dataDomain/card/actions/billingSchedulesRemove';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import configStateMock from 'auto-core/react/dataDomain/config/mock';
import userWithAuth from 'auto-core/react/dataDomain/user/mocks/withAuth.mock';

import { TBillingFrom } from 'auto-core/types/TBilling';
import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import openModalDialogAction from '../../dataDomain/state/actions/openModalDialog';

import VasBlock from './VasBlock';

const openModalDialog = openModalDialogAction as jest.MockedFunction<typeof openModalDialogAction>;

const fetchAndToggleServiceAutoProlongation =
    offerActions.fetchAndToggleServiceAutoProlongation as jest.MockedFunction<typeof offerActions.fetchAndToggleServiceAutoProlongation>;

const toggleAutoProlongationMock = toggleAutoProlongation as jest.MockedFunction<typeof toggleAutoProlongation>;
toggleAutoProlongationMock.mockReturnValue(() => Promise.resolve());

let autoRenewTime: string;

const billingSchedulesPutMock = billingSchedulesPut as jest.MockedFunction<typeof billingSchedulesPut>;
billingSchedulesPutMock.mockReturnValue((time: string) => {
    autoRenewTime = time;
    return () => {};
});

const billingSchedulesRemoveMock = billingSchedulesRemove as jest.MockedFunction<typeof billingSchedulesRemove>;
billingSchedulesRemoveMock.mockReturnValue(() => () => Promise.resolve());

let props: any;
let initialState: any;

beforeEach(() => {
    props = {
        offer: offerMock,
        params: {
            from: TBillingFrom.MOBILE_CARD,
        },
    };
    initialState = {
        bunker: getBunkerMock([ 'common/vas_vip' ]),
        config: configStateMock.value(),
        user: userWithAuth,
    };

    contextMock.logVasEvent.mockClear();
    contextMock.metrika.sendParams.mockClear();
});

describe('правильно отображает список сервисов', () => {
    it('если доступен вип, покажет его, турбо и все сервисы', () => {
        props.offer = cloneOfferWithHelpers(offerMock)
            .withCustomVas({
                service: TOfferVas.VIP,
                recommendation_priority: 1,
            })
            .withPrice(initialState.bunker['common/vas_vip'].minvalue + 1)
            .value();
        const page = shallowRenderComponent({ props, initialState });
        const packages = page.find('VasItemPackage').map(mapNodeToServiceId);
        const services = page.find('VasItemService').map(mapNodeToServiceId);

        expect(packages).toEqual([ TOfferVas.VIP, TOfferVas.TURBO ]);
        expect(services).toEqual([ TOfferVas.STORIES, TOfferVas.TOP, TOfferVas.COLOR, TOfferVas.SPECIAL ]);
    });

    it('если не доступен вип, покажет турбо, экспресс и все сервисы', () => {
        props.offer = cloneOfferWithHelpers(offerMock)
            .withCustomVas({
                service: TOfferVas.VIP,
                recommendation_priority: 0,
            })
            .withPrice(initialState.bunker['common/vas_vip'].minvalue - 1)
            .value();
        const page = shallowRenderComponent({ props, initialState });
        const packages = page.find('VasItemPackage').map(mapNodeToServiceId);
        const services = page.find('VasItemService').map(mapNodeToServiceId);

        expect(packages).toEqual([ TOfferVas.TURBO, TOfferVas.EXPRESS ]);
        expect(services).toEqual([ TOfferVas.STORIES, TOfferVas.TOP, TOfferVas.COLOR, TOfferVas.SPECIAL ]);
    });

    it('если вип куплен, то покажет его и сторисы', () => {
        props.offer = cloneOfferWithHelpers(offerMock)
            .withCustomActiveServices([ { service: TOfferVas.VIP } ])
            .value();
        const page = shallowRenderComponent({ props, initialState });
        const packages = page.find('VasItemPackage').map(mapNodeToServiceId);
        const services = page.find('VasItemService').map(mapNodeToServiceId);

        expect(packages).toEqual([ TOfferVas.VIP ]);
        expect(services).toEqual([ TOfferVas.STORIES ]);
    });

    it('если не пришел вас', () => {
        const offer = cloneOfferWithHelpers(offerMock).value();
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        // @ts-ignore
        offer.service_prices = offer.service_prices.map((service) => {
            if (service.service === 'show-in-stories') {
                return {};
            }

            return service;
        });

        props.offer = offer;
        const page = shallowRenderComponent({ props, initialState });
        const packages = page.find('VasItemPackage').map(mapNodeToServiceId);
        const services = page.find('VasItemService').map(mapNodeToServiceId);

        expect(packages).toEqual([ TOfferVas.VIP, TOfferVas.TURBO ]);
        expect(services).toEqual([ TOfferVas.TOP, TOfferVas.COLOR, TOfferVas.SPECIAL ]);
    });
});

it('при клике на сервис откроет галерею с васами', () => {
    const page = shallowRenderComponent({ props, initialState });
    const vipPackage = page.find('VasItemPackage').at(0);
    vipPackage.simulate('blockClick', TOfferVas.VIP);

    expect(openModalDialog).toHaveBeenCalledTimes(1);
    expect(openModalDialog.mock.calls[0]).toMatchSnapshot();
});

it('при попадании сервиса в поле видимости залогирует показ', () => {
    const page = shallowRenderComponent({ props, initialState });
    const vipPackage = page.find('VasItemPackage').at(0);
    vipPackage.simulate('intersectionChange', TOfferVas.VIP);

    expect(contextMock.logVasEvent).toHaveBeenCalledTimes(1);
    expect(contextMock.logVasEvent.mock.calls[0]).toMatchSnapshot();
});

describe('при включении/выключении автопролонгации', () => {
    it('отправит правильный экшен на карточке', () => {
        initialState.config.data.pageType = 'card';
        const page = shallowRenderComponent({ props, initialState });
        const vipPackage = page.find('VasItemPackage').at(0);
        vipPackage.simulate('autoProlongationToggle', true, TOfferVas.VIP);

        expect(toggleAutoProlongationMock).toHaveBeenCalledTimes(1);
        expect(toggleAutoProlongationMock.mock.calls[0]).toMatchSnapshot();
    });

    it('отправит правильный экшен в лк', () => {
        initialState.config.data.pageType = 'sales';
        const page = shallowRenderComponent({ props, initialState });
        const vipPackage = page.find('VasItemPackage').at(0);
        vipPackage.simulate('autoProlongationToggle', false, TOfferVas.VIP);

        expect(fetchAndToggleServiceAutoProlongation).toHaveBeenCalledTimes(1);
        expect(fetchAndToggleServiceAutoProlongation.mock.calls[0]).toMatchSnapshot();
    });
});

describe('автоподнятие', () => {
    it('отправит правильный экшен при включении', () => {
        const page = shallowRenderComponent({ props, initialState });
        const freshService = page.find('VasItemFresh');
        freshService.simulate('autoRenewChange', true, '07:00', true);

        expect(billingSchedulesPutMock).toHaveBeenCalledTimes(1);
        expect(billingSchedulesPutMock.mock.calls[0]).toMatchSnapshot();
        expect(autoRenewTime).toBe('07:00');
    });

    it('отправит правильный экшен при выключении', () => {
        const page = shallowRenderComponent({ props, initialState });
        const freshService = page.find('VasItemFresh');
        freshService.simulate('autoRenewChange', false);

        expect(billingSchedulesRemoveMock).toHaveBeenCalledTimes(1);
        expect(billingSchedulesRemoveMock.mock.calls[0]).toMatchSnapshot();
    });
});

function mapNodeToServiceId(node: { prop: (name: string) => { service: string } }) {
    return node.prop('serviceInfo').service;
}

function shallowRenderComponent({ initialState, props }: {props: any; initialState: any}) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    const page = shallow(
        <ContextProvider>
            <VasBlock { ...props } store={ store }/>
        </ContextProvider>,
    );

    return page.dive().dive();
}
