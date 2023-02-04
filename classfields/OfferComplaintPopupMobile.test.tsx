import React from 'react';
import { shallow } from 'enzyme';

import type { Seller } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import OfferComplaintPopupMobile from './OfferComplaintPopupMobile';

const store = mockStore({
    user: { data: {} },
});

const offer = {
    category: 'cars',
    seller: {
        phones: [],
    } as Partial<Seller> as Seller,
    seller_type: 'PRIVATE',
} as Partial<Offer> as Offer;

it('кнопка далее должна стать активной только после выбора причины', () => {
    const tree = render();
    expect(tree.find('.OfferComplaintPopupMobile__button').prop('disabled')).toBe(true);
    tree.find('CheckboxGroup').simulate('change', [ 'SOLD' ]);
    expect(tree.find('.OfferComplaintPopupMobile__button').prop('disabled')).toBe(false);
});

describe('кнопка отправить', () => {
    it('должна быть активной при пустых инпутах, если нет причины другое', () => {
        const tree = render();
        tree.find('CheckboxGroup').simulate('change', [ 'SOLD' ]);
        tree.find('.OfferComplaintPopupMobile__button').simulate('click');
        expect(tree.find('ButtonWithLoader').prop('disabled')).toBe(false);
    });
    it('должна стать активной после ввода причины, если есть причина другое', () => {
        const tree = render();
        tree.find('CheckboxGroup').simulate('change', [ 'SOLD', 'ANOTHER' ]);
        tree.find('.OfferComplaintPopupMobile__button').simulate('click');
        expect(tree.find('ButtonWithLoader').prop('disabled')).toBe(true);
        tree.find('TextInput').at(1).simulate('change', 'bruh', { name: 'ANOTHER' });
        expect(tree.find('ButtonWithLoader').prop('disabled')).toBe(false);
    });
});

function render() {
    return shallow(
        <OfferComplaintPopupMobile
            offer={ offer }
            isOpen
            onRequestHide={ jest.fn }
            metrikaOrigin=""
        />,
        { context: { ...contextMock, store } },
    ).dive();
}
