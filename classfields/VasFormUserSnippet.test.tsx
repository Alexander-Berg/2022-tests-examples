/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import { getBunkerMock } from 'autoru-frontend/mockData/state/bunker.mock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import cardMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import getServicePrices from 'auto-core/react/lib/offer/getServicePrices';

import { TOfferVas } from 'auto-core/types/proto/auto/api/api_offer_model';
import type { PaidServicePrice } from 'auto-core/types/proto/auto/api/api_offer_model';

import { TVasSnippets } from '../../utils';

import VasFormUserSnippet from './VasFormUserSnippet';
import type { Props } from './VasFormUserSnippet';

let props: Props & { onSubmitButtonClick: jest.Mock };

beforeEach(() => {
    props = {
        isEdit: false,
        isMiddle: false,
        isPending: false,
        isVasSubmitted: false,
        servicePrices: getServicePrices(cardMock),
        type: TVasSnippets.TURBO,
        unpaidBadgesNum: 0,
        onSubmitButtonClick: jest.fn(),
        offer: cardMock,
        texts: getBunkerMock([ 'common/form_vas_text' ]) as Record<TVasSnippets, string>,
        hasMosRuActionWall: false,
        needActionBeforeSubmit: false,
        onSocialProviderConnect: jest.fn(),
        hideDiscountBadge: false,
    };

    contextMock.logVasEvent.mockClear();
    contextMock.hasExperiment.mockReset();
});

describe('логи васов:', () => {
    it('залогирует показ, когда блок будет в области видимости', () => {
        const page = shallowRenderComponent({ props });

        expect(contextMock.logVasEvent).toHaveBeenCalledTimes(0);

        page.simulate('change', true);

        expect(contextMock.logVasEvent).toHaveBeenCalledTimes(2);
        expect(contextMock.logVasEvent.mock.calls).toMatchSnapshot();
    });

    it('залогирует показ для всех услуг на сниппете "обычная продажа"', () => {
        props.type = TVasSnippets.FREE;
        props.unpaidBadgesNum = 1;
        const page = shallowRenderComponent({ props });

        page.simulate('change', true);

        expect(contextMock.logVasEvent.mock.calls).toMatchSnapshot();
    });
});

it('не покажет цену за день в экспе', () => {
    const context = {
        ...contextMock,
        hasExperiment: jest.fn((exp: string) => exp === 'AUTORUFRONT-18913_no_more_price_per_day'),
    };
    const page = shallowRenderComponent({ props, context });
    const pricePerDay = page.find('.VasFormUserSnippet__subtitlePrice');

    expect(pricePerDay.isEmptyRender()).toBe(true);
});

it('правильно рассчитывает цену за день при платном размещении', () => {
    props.unpaidBadgesNum = 1;
    const page = shallowRenderComponent({ props });
    const pricePerDay = page.find('.VasFormUserSnippet__subtitlePrice');

    // (99 + 375 + 2199) / 60 = 45
    expect(Math.round(pricePerDay.prop('price'))).toBe(45);
});

it('правильно рассчитывает цену за день при бесплатном размещении', () => {
    props.unpaidBadgesNum = 1;
    props.servicePrices = getServicePrices(
        cloneOfferWithHelpers(cardMock)
            .withCustomVas({ service: TOfferVas.PLACEMENT, price: 0 })
            .value(),
    );
    const page = shallowRenderComponent({ props });
    const pricePerDay = page.find('.VasFormUserSnippet__subtitlePrice');

    // 99 / 60 + 375 / 3 = 127
    expect(Math.round(pricePerDay.prop('price'))).toBe(127);
});

it('правильно сортирует сервисы на сниппете', () => {
    props.unpaidBadgesNum = 1;
    props.type = TVasSnippets.VIP;
    const page = shallowRenderComponent({ props });
    const services = page.find('VasFormUserSnippetItem');

    const serviceIds = services.map((service: { prop: (name: string) => PaidServicePrice }) => service.prop('serviceInfo').service);

    expect(serviceIds).toMatchSnapshot();
});

describe('цены на кнопке', () => {
    it('правильно считаются для пакета', () => {
        props.unpaidBadgesNum = 2;
        const page = shallowRenderComponent({ props });

        const [ price, originalPrice ] = page
            .find('.VasFormUserSnippet__buttonContent')
            .find('Price')
            .map((node) => node.prop('price'));

        expect(price).toBe(2772);
        expect(originalPrice).toBe(3644);
    });

    it('правильно считаются для отдельных опций', () => {
        props.type = TVasSnippets.FREE;
        props.unpaidBadgesNum = 2;
        const page = shallowRenderComponent({ props });

        const firstVas = page.find('VasFormUserSnippetItem').at(0);
        firstVas.simulate('serviceSelect', TOfferVas.TOP, true);

        const [ price, originalPrice ] = page
            .find('.VasFormUserSnippet__buttonContent')
            .find('Price')
            .map((node) => node.prop('price'));

        expect(price).toBe(2697);
        expect(originalPrice).toBe(3394);
    });
});

describe('лейбл про скидку у кнопки', () => {
    it('покажется при бесплатном размещении', () => {
        props.servicePrices = getServicePrices(
            cloneOfferWithHelpers(cardMock)
                .withCustomVas({ service: TOfferVas.PLACEMENT, price: 0 })
                .value(),
        );
        const page = shallowRenderComponent({ props });
        const discountLabel = page.find('.VasFormUserSnippet__discountLabel');

        expect(discountLabel.isEmptyRender()).toBe(false);
    });

    it('покажется при редактировании', () => {
        props.isEdit = true;
        const page = shallowRenderComponent({ props });
        const discountLabel = page.find('.VasFormUserSnippet__discountLabel');

        expect(discountLabel.isEmptyRender()).toBe(false);
    });

    it('не покажется, если передан проп на его скрытие', () => {
        props.isEdit = true;
        props.hideDiscountBadge = true;
        const page = shallowRenderComponent({ props });
        const discountLabel = page.find('.VasFormUserSnippet__discountLabel');

        expect(discountLabel.isEmptyRender()).toBe(true);
    });

    it('не покажется при платном размещении', () => {
        const page = shallowRenderComponent({ props });
        const discountLabel = page.find('.VasFormUserSnippet__discountLabel');

        expect(discountLabel.isEmptyRender()).toBe(true);
    });

    it('не покажется если нет скидки на пакет', () => {
        props.servicePrices = getServicePrices(
            cloneOfferWithHelpers(cardMock)
                .withCustomVas({ service: TOfferVas.TURBO, price: 777, original_price: 777 })
                .value(),
        );
        const page = shallowRenderComponent({ props });
        const discountLabel = page.find('.VasFormUserSnippet__discountLabel');

        expect(discountLabel.isEmptyRender()).toBe(true);
    });
});

describe('правильно формирует список сервисов для оплаты', () => {
    it('при бесплатном размещении без услуг', () => {
        props.type = TVasSnippets.FREE;
        props.servicePrices = getServicePrices(
            cloneOfferWithHelpers(cardMock)
                .withCustomVas({ service: TOfferVas.PLACEMENT, price: 0 })
                .value(),
        );
        const page = shallowRenderComponent({ props });
        const button = page.find('.VasFormUserSnippet__button');
        button.simulate('click');

        expect(props.onSubmitButtonClick).toHaveBeenCalledTimes(1);
        expect(props.onSubmitButtonClick.mock.calls[0]).toMatchSnapshot();
    });

    it('при бесплатном размещении с пакетом', () => {
        props.servicePrices = getServicePrices(
            cloneOfferWithHelpers(cardMock)
                .withCustomVas({ service: TOfferVas.PLACEMENT, price: 0 })
                .value(),
        );
        const page = shallowRenderComponent({ props });
        const button = page.find('.VasFormUserSnippet__button');
        button.simulate('click');

        expect(props.onSubmitButtonClick).toHaveBeenCalledTimes(1);
        expect(props.onSubmitButtonClick.mock.calls[0]).toMatchSnapshot();
    });

    it('при платном размещении с васами', () => {
        props.type = TVasSnippets.FREE;
        props.unpaidBadgesNum = 2;
        const page = shallowRenderComponent({ props });

        const firstVas = page.find('VasFormUserSnippetItem').at(0);
        firstVas.simulate('serviceSelect', TOfferVas.TOP, true);

        const button = page.find('.VasFormUserSnippet__button');
        button.simulate('click');

        expect(props.onSubmitButtonClick).toHaveBeenCalledTimes(1);
        expect(props.onSubmitButtonClick.mock.calls[0]).toMatchSnapshot();
    });

    it('при редактировании с васами', () => {
        props.type = TVasSnippets.FREE;
        props.unpaidBadgesNum = 1;
        props.isEdit = true;
        const page = shallowRenderComponent({ props });

        const firstVas = page.find('VasFormUserSnippetItem').at(0);
        firstVas.simulate('serviceSelect', TOfferVas.TOP, true);

        const button = page.find('.VasFormUserSnippet__button');
        button.simulate('click');

        expect(props.onSubmitButtonClick).toHaveBeenCalledTimes(1);
        expect(props.onSubmitButtonClick.mock.calls[0]).toMatchSnapshot();
    });

    it('при бесплатном размещении без услуг но с экшен-волом мос.ру', () => {
        props.type = TVasSnippets.FREE;
        props.servicePrices = getServicePrices(
            cloneOfferWithHelpers(cardMock)
                .withCustomVas({ service: TOfferVas.PLACEMENT, price: 0 })
                .value(),
        );
        props.hasMosRuActionWall = true;
        const page = shallowRenderComponent({ props });
        const button = page.find('.VasFormUserSnippet__button');
        button.simulate('click');

        expect(props.onSubmitButtonClick).toHaveBeenCalledTimes(1);
        expect(props.onSubmitButtonClick.mock.calls[0]).toMatchSnapshot();
    });

    it('при бесплатном размещении с васом и с экшен-волом мос.ру', () => {
        props.type = TVasSnippets.FREE;
        props.servicePrices = getServicePrices(
            cloneOfferWithHelpers(cardMock)
                .withCustomVas({ service: TOfferVas.PLACEMENT, price: 0 })
                .value(),
        );
        props.hasMosRuActionWall = true;

        const page = shallowRenderComponent({ props });

        const topItem = page.find('VasFormUserSnippetItem').at(1);
        topItem.simulate('optionSelect', TOfferVas.TOP);

        const button = page.find('.VasFormUserSnippet__button');
        button.simulate('click');

        expect(props.onSubmitButtonClick).toHaveBeenCalledTimes(1);
        expect(props.onSubmitButtonClick.mock.calls[0]).toMatchSnapshot();
    });

    it('при бесплатном размещении с пактом и с экшен-волом мос.ру', () => {
        props.type = TVasSnippets.TURBO;
        props.servicePrices = getServicePrices(
            cloneOfferWithHelpers(cardMock)
                .withCustomVas({ service: TOfferVas.PLACEMENT, price: 0 })
                .value(),
        );
        props.hasMosRuActionWall = true;

        const page = shallowRenderComponent({ props });

        const button = page.find('.VasFormUserSnippet__button');
        button.simulate('click');

        expect(props.onSubmitButtonClick).toHaveBeenCalledTimes(1);
        expect(props.onSubmitButtonClick.mock.calls[0]).toMatchSnapshot();
    });
});

describe('поднятие в поиске на сниппете с обычной продажей при редактировании', () => {
    beforeEach(() => {
        props.isEdit = true;
        props.type = TVasSnippets.FREE;
    });

    it('не взведет галку для всех офферов', () => {
        props.offer = cloneOfferWithHelpers(cardMock)
            .withCustomActiveServices([ { service: TOfferVas.TURBO } ])
            .value();
        const page = shallowRenderComponent({ props });
        const freshService = page.find('VasFormUserSnippetItem').find({ serviceInfo: { service: TOfferVas.FRESH } });

        expect(freshService.prop('isSelected')).toBe(false);
    });
});

function shallowRenderComponent({ context, props }: { props: Props; context?: typeof contextMock }) {
    const ContextProvider = createContextProvider(context || contextMock);

    const page = shallow(
        <ContextProvider>
            <VasFormUserSnippet { ...props }/>
        </ContextProvider>,
    );

    return page.dive().dive();
}
