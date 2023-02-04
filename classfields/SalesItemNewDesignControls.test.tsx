import React from 'react';
import { shallow } from 'enzyme';
import MockDate from 'mockdate';
import type { ShallowWrapper } from 'enzyme';

import type { Actions } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';
import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import SalesItemNewDesignControls from './SalesItemNewDesignControls';
import type { Props } from './SalesItemNewDesignControls';

const defaultOffer = cloneOfferWithHelpers(offerMock)
    .withCreationDate(String(new Date('2021-03-15T12:00:00Z').getTime()))
    .withExpireDate(String(new Date('2021-04-03T12:00:00Z').getTime()))
    .withServices(offerMock.services.map(item => ({
        ...item, expire_date: String(new Date('2021-04-03T12:00:00Z').getTime()),
    })))
    .value();

const defaultProps = {
    offer: defaultOffer,
    handleOfferActivate: jest.fn(),
    handleOfferDelete: jest.fn(),
    handleDraftDelete: jest.fn(),
    handleOfferHide: jest.fn(),
    onMoreMenuVisibleChange: jest.fn(),
    onCallsHistoryPopupVisibleChange: jest.fn(),
    isUserBanned: false,
    isCallsHistoryModalVisible: false,
    isHovered: true,
    isMoreMenuVisible: false,
};

beforeEach(() => {
    MockDate.set('2021-01-01');
});

describe('отображаемые контролы', () => {
    it('отображает все, если все условия выполнены', () => {
        const offer = cloneOfferWithHelpers(defaultOffer)
            .withAction({ archive: true } as Actions)
            .value();
        const wrapper = shallowRenderComponent({ props: { ...defaultProps, offer } });

        const elements = wrapper.find('HoveredTooltip').map(item => (item.props() as any).tooltipContent);

        expect(elements).toEqual([ 'Редактировать', 'Снять с публикации', 'Удалить', 'Поделиться' ]);
    });

    it('отображает Опубликовать вместо Снять с публикации и не отображает Поделиться, когда есть нужный экшн и оффер не активен', () => {
        const offer = cloneOfferWithHelpers(defaultOffer)
            .withAction({ archive: true, activate: true } as Actions)
            .withStatus(OfferStatus.INACTIVE)
            .value();
        const wrapper = shallowRenderComponent({ props: { ...defaultProps, offer } });

        const elements = wrapper.find('HoveredTooltip').map(item => (item.props() as any).tooltipContent);

        expect(elements).toEqual([ 'Редактировать', 'Опубликовать', 'Удалить' ]);
    });

    it('нет удалить, если нет экшена', () => {
        const offer = cloneOfferWithHelpers(defaultOffer)
            .withAction({ archive: false } as Actions)
            .value();
        const wrapper = shallowRenderComponent({ props: { ...defaultProps, offer } });

        const elements = wrapper.find('HoveredTooltip').map(item => (item.props() as any).tooltipContent);

        expect(elements).toEqual([ 'Редактировать', 'Снять с публикации', 'Поделиться' ]);
    });

    it('нет удалить, если юзер забанен', () => {
        const wrapper = shallowRenderComponent({ props: { ...defaultProps, isUserBanned: true } });

        const elements = wrapper.find('HoveredTooltip').map(item => (item.props() as any).tooltipContent);

        expect(elements).toEqual([ 'Редактировать', 'Снять с публикации', 'Поделиться' ]);
    });

    it('нет редактировать, если нет нужного экшена', () => {
        const offer = cloneOfferWithHelpers(defaultOffer)
            .withAction({ edit: false } as Actions)
            .value();
        const wrapper = shallowRenderComponent({ props: { ...defaultProps, offer } });

        const elements = wrapper.find('HoveredTooltip').map(item => (item.props() as any).tooltipContent);

        expect(elements).toEqual([ 'Снять с публикации', 'Поделиться' ]);
    });

    it('отображает кнопки для оффера в драфте', () => {
        const offer = cloneOfferWithHelpers(defaultOffer)
            .withAction({ archive: false } as Actions)
            .withStatus(OfferStatus.DRAFT)
            .value();
        const wrapper = shallowRenderComponent({ props: { ...defaultProps, offer } });

        const elements = wrapper.find('HoveredTooltip').map(item => (item.props() as any).tooltipContent);

        expect(elements).toEqual([ 'Продолжить заполнение', 'Удалить' ]);
    });
});

describe('содержимое выпадашки', () => {
    it('все элементы, если условия выполняются', () => {
        const wrapper = shallowRenderComponent({ props: defaultProps });

        const list = getDropdownChildrenKeys(wrapper);

        expect(list).toEqual([ 'contract', 'print-offer', 'calls-history' ]);
    });

    it('не рендерит лист продажи и историю звонков, если категория не cars', () => {
        const wrapper = shallowRenderComponent({ props: {
            ...defaultProps,
            offer: cloneOfferWithHelpers(defaultOffer).withCategory('moto').value(),
        } });

        const list = getDropdownChildrenKeys(wrapper);

        expect(list).toEqual([ 'contract' ]);
    });

    it('не рендерит историю звонков, если нет redirect_phones', () => {
        const wrapper = shallowRenderComponent({ props: {
            ...defaultProps,
            offer: cloneOfferWithHelpers(defaultOffer).withSeller({ ...defaultOffer.seller, redirect_phones: false }).value(),
        } });

        const list = getDropdownChildrenKeys(wrapper);

        expect(list).toEqual([ 'contract', 'print-offer' ]);
    });

    it('не рендерит историю звонков, если нет counters.calls_all', () => {
        const wrapper = shallowRenderComponent({ props: {
            ...defaultProps,
            offer: cloneOfferWithHelpers(defaultOffer).withCounters({}).value(),
        } });

        const list = getDropdownChildrenKeys(wrapper);

        expect(list).toEqual([ 'contract', 'print-offer' ]);
    });

    it('не рендерит историю звонков, если counters.calls_all === 0', () => {
        const wrapper = shallowRenderComponent({ props: {
            ...defaultProps,
            offer: cloneOfferWithHelpers(defaultOffer).withCounters({ calls_all: 0 }).value(),
        } });

        const list = getDropdownChildrenKeys(wrapper);

        expect(list).toEqual([ 'contract', 'print-offer' ]);
    });
});

// сравниваем массив key элементов внутри модала, чтобы не писать большой снэпшот
function getDropdownChildrenKeys(wrapper: ShallowWrapper) {
    wrapper.find('.SalesItemNewDesignControls').find('Dropdown').dive().find('.Dropdown__switcher').simulate('click');
    const menu = wrapper.find('.SalesItemNewDesignControls__moreMenu');

    return menu.children().map(item => item.key());
}

function shallowRenderComponent({ props }: { props: Props }) {
    const ContextProvider = createContextProvider(contextMock);

    const wrapper = shallow(
        <ContextProvider>
            <SalesItemNewDesignControls { ...props }/>
        </ContextProvider>,
    );

    return wrapper.dive();
}
