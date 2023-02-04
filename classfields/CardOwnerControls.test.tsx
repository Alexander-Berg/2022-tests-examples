/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */
jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');

import type { Action } from 'redux';
import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import hideMock from 'autoru-frontend/mockData/state/helpers/hideMock';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import Button from 'auto-core/react/components/islands/Button/Button';
import paymentModalOpen from 'auto-core/react/dataDomain/state/actions/paymentModalOpen';
import getServiceInfoMerged from 'auto-core/react/lib/offer/getServiceInfoMerged';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';
import { TBillingFrom } from 'auto-core/types/TBilling';

import type { MobileAppState } from 'www-mobile/react/MobileAppState';

import CardOwnerControls from './CardOwnerControls';

const paymentModalOpenMock = paymentModalOpen as jest.MockedFunction<typeof paymentModalOpen>;
paymentModalOpenMock.mockReturnValue((() => () => { }) as unknown as Action);

let defaultProps: any;
let store: MobileAppState;
beforeEach(() => {
    defaultProps = {
        isPlacementDiscountActive: false,
        pageType: 'card',
        params: {
            from: TBillingFrom.MOBILE_CARD_PLACEMENT_PROLONG,
        },
        placementInfo: getServiceInfoMerged(offerMock, TOfferVas.PLACEMENT),
    };
    store = mockStore({
        bunker: {
            'common/activate_in_app': {
                marks: [],
            },
        },
        user: { data: {} },
    }) as Partial<MobileAppState> as MobileAppState;
});

describe('блок статистики', () => {
    it('должен отрисовать, если объявление частника активно', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSellerTypePrivate()
            .withStatus(OfferStatus.ACTIVE);

        const wrapper = shallow(
            <CardOwnerControls
                { ...defaultProps }
                offer={ offer.value() }
            />,
            { context: { store, ...contextMock } },
        ).dive().dive();

        expect(wrapper.find('.CardOwnerControlsMobile__stat')).toHaveLength(1);
    });

    it('не должен отрисовать, если объявление частника неактивно', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSellerTypePrivate()
            .withStatus(OfferStatus.INACTIVE);

        const wrapper = shallow(
            <CardOwnerControls
                { ...defaultProps }
                offer={ offer.value() }
            />,
            { context: { store, ...contextMock } },
        ).dive().dive();

        expect(wrapper.find('.CardOwnerControlsMobile__stat')).toHaveLength(0);
    });

    it('не должен отрисовать, если объявление дилера', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSellerTypeCommercial()
            .withStatus(OfferStatus.ACTIVE);

        const wrapper = shallow(
            <CardOwnerControls
                { ...defaultProps }
                offer={ offer.value() }
            />,
            { context: { store, ...contextMock } },
        ).dive().dive();

        expect(wrapper.find('.CardOwnerControlsMobile__stat')).toHaveLength(0);
    });

    it('должен отрисовать блок статистики без позиции в поиске, если ее нет', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSearchPosition(-1)
            .value();
        const wrapper = shallow(
            <CardOwnerControls
                { ...defaultProps }
                offer={ offer }
            />,
            { context: { store, ...contextMock } },
        ).dive().dive();
        const snapshot = shallowToJson(wrapper.find('.CardOwnerControlsMobile__stat'), { map: hideMock(offer, 'offer', '[Offer mock]') });
        expect(snapshot).toMatchSnapshot();
    });
});

describe('кнопки действий', () => {
    it('должен отрисовать правильный набор кнопок для редактирования и активации для частника', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSellerTypePrivate()
            .withAction({ edit: true, activate: true, hide: false, archive: true });

        const wrapper = shallow(
            <CardOwnerControls
                { ...defaultProps }
                offer={ offer.value() }
            />,
            { context: { store, ...contextMock } },
        ).dive().dive();

        expect(shallowToJson(wrapper.find('.CardOwnerControlsMobile__buttonGroup'))).toMatchSnapshot();
    });

    it('должен отрисовать только активацию для дилера', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSellerTypeCommercial()
            .withAction({ edit: true, activate: true, hide: false, archive: true });

        const wrapper = shallow(
            <CardOwnerControls
                { ...defaultProps }
                offer={ offer.value() }
            />,
            { context: { store, ...contextMock } },
        ).dive().dive();

        expect(shallowToJson(wrapper.find('.CardOwnerControlsMobile__buttonGroup'))).toMatchSnapshot();
    });

    it('должен отрисовать правильный набор кнопок для редактирования и снятия с продажи', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSellerTypePrivate()
            .withAction({ edit: true, activate: false, hide: true, archive: false });

        const wrapper = shallow(
            <CardOwnerControls
                { ...defaultProps }
                offer={ offer.value() }
            />,
            { context: { store, ...contextMock } },
        ).dive().dive();

        expect(shallowToJson(wrapper.find('.CardOwnerControlsMobile__buttonGroup'))).toMatchSnapshot();
    });

    it('должен отрисовать кнопку "написать в поддержку", если оффер забанен', () => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withSellerTypePrivate()
            .withAction({ edit: false, activate: false, hide: false, archive: false })
            .withStatus(OfferStatus.BANNED);

        const wrapper = shallow(
            <CardOwnerControls
                { ...defaultProps }
                offer={ offer.value() }
            />,
            { context: { store, ...contextMock } },
        ).dive().dive();

        expect(shallowToJson(wrapper.find('.CardOwnerControlsMobile__buttonGroup'))).toMatchSnapshot();
    });
});

describe('если для оффера действует скидка на продление размещения', () => {
    let wrapper: ShallowWrapper;
    beforeEach(() => {
        const offer = cloneOfferWithHelpers(offerMock)
            .withAction({ edit: true, activate: true, hide: false, archive: true })
            .withStatus(OfferStatus.INACTIVE)
            .value();
        wrapper = shallow(
            <CardOwnerControls
                { ...defaultProps }
                offer={ offer }
                isPlacementDiscountActive
            />,
            { context: { store, ...contextMock } },
        ).dive().dive();
    });

    it('залогирует показ васа при маунте', () => {
        expect(contextMock.logVasEvent).toHaveBeenCalledTimes(1);
        expect(contextMock.logVasEvent.mock.calls[0]).toMatchSnapshot();
    });

    it('при клике на кнопку активировать вызовет модал оплаты', () => {
        wrapper.find('Button').find({ color: Button.COLOR.GREEN }).simulate('click');
        expect(paymentModalOpenMock).toHaveBeenCalledTimes(1);
        expect(paymentModalOpenMock.mock.calls[0]).toMatchSnapshot();
    });

    it('при клике на кнопку активировать залогирует клик', () => {
        wrapper.find('Button').find({ color: Button.COLOR.GREEN }).simulate('click');
        expect(contextMock.logVasEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.logVasEvent.mock.calls[1]).toMatchSnapshot();
    });
});
