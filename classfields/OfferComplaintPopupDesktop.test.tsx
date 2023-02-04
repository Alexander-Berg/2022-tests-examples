import React from 'react';
import { shallow } from 'enzyme';

import type { Seller } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import OfferComplaintPopupDesktop from './OfferComplaintPopupDesktop';

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

describe('кнопка отправить', () => {
    it('должна стать активной, если выбрать любую причину кроме другое', () => {
        const tree = render();
        expect(tree.find('ButtonWithLoader').prop('disabled')).toBe(true);
        tree.find('CheckboxGroup').simulate('change', [ 'SOLD' ]);
        expect(tree.find('ButtonWithLoader').prop('disabled')).toBe(false);
    });
    it('при выборе причины другое станет активной только после ввода причины', () => {
        const tree = render();
        tree.find('CheckboxGroup').simulate('change', [ 'ANOTHER' ]);
        expect(tree.find('ButtonWithLoader').prop('disabled')).toBe(true);
        tree.find('TextInput').simulate('change', 'bruh', { name: 'ANOTHER' });
        expect(tree.find('ButtonWithLoader').prop('disabled')).toBe(false);
    });
});

function render() {
    return shallow(
        <OfferComplaintPopupDesktop
            offer={ offer }
            isOpen
            onRequestHide={ jest.fn }
            metrikaOrigin=""
        />,
        { context: { ...contextMock, store } },
    ).dive();
}
