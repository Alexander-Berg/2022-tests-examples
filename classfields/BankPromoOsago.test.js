const React = require('react');
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend//mocks/contextMock';
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const BankPromoOsago = require('./BankPromoOsago');

describe('Manage promo osago iframe in bunker', () => {
    it('- enabled', () => {
        const store = mockStore({
            bunker: {
                'promo/osago': {
                    hasBankPromoOsagoIframe: true,
                },
            },
        });

        const tree = shallow(<BankPromoOsago/>, { context: { ...contextMock, store } });
        expect(tree.dive().find('.BankPromoOsago__frame')).toExist();
    });

    it('- disabled', () => {
        const store = mockStore({
            bunker: {
                'promo/osago': {
                    hasBankPromoOsagoIframe: false,
                },
            },
        });

        const tree = shallow(<BankPromoOsago/>, { context: { ...contextMock, store } });
        expect(tree.dive().find('.BankPromoOsago__frame_disabled')).toExist();
    });
});
