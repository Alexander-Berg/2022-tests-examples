import React from 'react';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import cardStateMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';

import VasFormUserServiceSet from './VasFormUserServiceSet';
import type { Props } from './VasFormUserServiceSet';

let props: Props & { onSubmitButtonClick: jest.Mock; onAutoProlongationToggle: jest.Mock };

beforeEach(() => {
    props = {
        isOfferInactive: false,
        isPending: false,
        isVasSubmitted: false,
        offer: cloneOfferWithHelpers(cardStateMock).withCustomActiveServices([ { service: TOfferVas.TURBO } ]).value(),
        unpaidBadgesNum: 0,
        onSubmitButtonClick: jest.fn(),
        onAutoProlongationToggle: jest.fn(),
    };

    contextMock.logVasEvent.mockClear();
});

it('покажет только кнопку если оффер не активен', () => {
    props.isOfferInactive = true;
    const page = shallowRenderComponent({ props });
    const services = page.find('VasFormUserService');
    const button = page.find('.VasFormUserServiceSet__button');

    expect(services).toHaveLength(0);
    expect(button).toMatchSnapshot();
});

describe('логи васов:', () => {
    it('если куплен пакет, залогирует показ, когда блок будет в области видимости', () => {
        const page = shallowRenderComponent({ props });

        expect(contextMock.logVasEvent).toHaveBeenCalledTimes(0);

        page.simulate('change', true);

        expect(contextMock.logVasEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.logVasEvent.mock.calls).toMatchSnapshot();
    });
});

describe('правильно формирует список сервисов для оплаты', () => {
    it('при сохранении без услуг', () => {
        const page = shallowRenderComponent({ props });

        const button = page.find('.VasFormUserServiceSet__button');
        button.simulate('click');

        expect(props.onSubmitButtonClick).toHaveBeenCalledTimes(1);
        expect(props.onSubmitButtonClick.mock.calls[0]).toMatchSnapshot();
    });

    it('при сохранении неактивного оффера', () => {
        props.isOfferInactive = true;
        const page = shallowRenderComponent({ props });
        const button = page.find('.VasFormUserServiceSet__button');
        button.simulate('click');

        expect(props.onSubmitButtonClick).toHaveBeenCalledTimes(1);
        expect(props.onSubmitButtonClick.mock.calls[0]).toMatchSnapshot();
    });

    it('при сохранении с услугами если куплен пакет', () => {
        const page = shallowRenderComponent({ props });

        const freshVas = page.find('VasFormUserService').at(1);
        freshVas.simulate('serviceSelect', TOfferVas.FRESH, true);

        const button = page.find('.VasFormUserServiceSet__button');
        button.simulate('click');

        expect(props.onSubmitButtonClick).toHaveBeenCalledTimes(1);
        expect(props.onSubmitButtonClick.mock.calls[0]).toMatchSnapshot();
    });
});

describe('поднятие в поиске', () => {
    it('не будет взводить галку для любого оффера', () => {
        props.offer = cloneOfferWithHelpers(cardStateMock)
            .withCustomActiveServices([ { service: TOfferVas.TURBO } ])
            .value();
        const page = shallowRenderComponent({ props });
        const freshService = page.find({ info: { service: TOfferVas.FRESH } });

        expect(freshService.prop('isSelected')).toBe(false);
    });
});

function shallowRenderComponent({ props }: { props: Props }) {
    const ContextProvider = createContextProvider(contextMock);

    const page = shallow(
        <ContextProvider>
            <VasFormUserServiceSet { ...props }/>
        </ContextProvider>,
    );

    return page.dive();
}
