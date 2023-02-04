import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';
import no from 'nommon';
import _ from 'lodash';

import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import mockStore from 'autoru-frontend/mocks/mockStore';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import getMultiposting from 'auto-core/react/lib/offer/getMultiposting';
import getIdHash from 'auto-core/react/lib/offer/getIdHash';

import type { CabinetOffer } from '../types';

import AvitoButtons from './AvitoButtons';

const offer = cloneOfferWithHelpers(offerMock).withMultiposting().value();

const defaultProps = {
    confirm: _.noop,
    // FIXME jpath
    // eslint-disable-next-line no-restricted-properties
    avitoInfo: no.jpath('.classifieds{ .name === "AVITO" }[0]', getMultiposting(offer)),
    canWriteSaleResource: true,
    offerID: getIdHash(offer),
    shouldShowButtonsWithoutText: false,
    applySaleService: () => new Promise(() => 'OK'),
    handleClassifiedAddButtonClick: () => new Promise(() => 'OK'),
    offers: [ offer ] as Array<CabinetOffer>,
};

it('задизейблит услуги увеличения просмотров при включенной одной из них', () => {
    const tree = shallowRenderComponent();
    const viewsUpButtons = tree.children().findWhere(node => [ 'x5', 'x10' ].includes(node.key()));
    expect(viewsUpButtons.everyWhere(button => button.prop('disabled'))).toEqual(true);
});

it('не будет рендерить тултип для услуг увеличения просмотров при включенной одной из них', () => {
    const tree = shallowRenderComponent();
    const viewsUpButtons = tree.findWhere(node => [ 'x2', 'x5', 'x10' ].includes(node.key()));
    expect(viewsUpButtons.everyWhere(button => button.name() === 'SaleServiceButton')).toEqual(true);
});

function shallowRenderComponent(props = defaultProps) {
    return shallow(
        <Provider store={ mockStore({ applySaleService: () => {} }) }>
            <AvitoButtons { ...props }/>
        </Provider>,
    ).dive().dive();
}
