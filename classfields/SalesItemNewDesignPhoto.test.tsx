import _ from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';

import { OfferStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import cardStateMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';

import panoramaExteriorMock from 'auto-core/models/panoramaExterior/mocks';

import type { Props } from './SalesItemNewDesignPhoto';
import SalesItemNewDesignPhoto from './SalesItemNewDesignPhoto';

let props: Props;
let context;

beforeEach(() => {
    props = {
        offer: cardStateMock,
        isInactive: false,
    };
    context = _.cloneDeep(contextMock);
    context.metrika.sendParams.mockClear();
});

it('правильно формирует ссылку если у оффера есть фото', () => {
    const page = shallowRenderComponent(props);
    const link = page.find('a');

    expect(link.prop('href')).toEqual('link/card/?category=cars&section=used&mark=FORD&model=ECOSPORT&sale_id=1085562758&sale_hash=1970f439');
});

it('правильно формирует ссылку если у оффера нет фото', () => {
    props.offer = cloneOfferWithHelpers(cardStateMock)
        .withImages([])
        .value();

    const page = shallowRenderComponent(props);
    const link = page.find('a');

    expect(link.prop('href')).toEqual('link/form/?category=cars&section=used&form_type=edit&sale_id=1085562758&sale_hash=1970f439#photo');
});

it('правильно формирует ссылку если оффер в статусе драфт', () => {
    props.offer = cloneOfferWithHelpers(cardStateMock)
        .withStatus(OfferStatus.DRAFT)
        .value();

    const page = shallowRenderComponent(props);
    const link = page.find('a');

    expect(link.prop('href')).toEqual('link/form/?category=cars&section=used&form_type=add&from_lk=true');
});

it('правильно формирует ссылку если произошла ошибка обработки панорамы', () => {
    props.offer = cloneOfferWithHelpers(cardStateMock)
        .withPanoramaExterior('next', panoramaExteriorMock.withFailed().value())
        .value();

    const page = shallowRenderComponent(props);
    const panoramaError = page.find('PanoramaProcessingError');

    expect(panoramaError.prop('url')).toEqual('link/form/?category=cars&section=used&form_type=edit&sale_id=1085562758&sale_hash=1970f439#panorama');
});

function shallowRenderComponent(props: Props) {
    const ContextProvider = createContextProvider(contextMock);

    return shallow(
        <ContextProvider>
            <SalesItemNewDesignPhoto { ...props }/>
        </ContextProvider>,
    ).dive();
}
