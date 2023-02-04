import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import configMock from 'auto-core/react/dataDomain/config/mock';
import creditProduct from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mock';
import type { TStateListing } from 'auto-core/react/dataDomain/listing/TStateListing';
import type { StateCreditProducts, TStateCredit } from 'auto-core/react/dataDomain/credit/TStateCredit';

import type { TOfferListing } from 'auto-core/types/TOfferListing';

import {
    creditFilterChange,
    creditInitialFee,
    maxInitialFee,
} from './creditFilterChange';
import type { State } from './creditFilterChange';

const { metrika } = contextMock;

const onChange = jest.fn();

describe('creditFilterChange', () => {
    it('ВЗНОС не может быть изменён на значение более 10кк', () => {
        const value = maxInitialFee + 1;
        const field = { name: creditInitialFee };
        const store = mockStore<State>({
            listing: {
                data: {
                    search_parameters: {
                        on_credit: true,
                        credit_initial_fee: 0,
                    },
                } as TOfferListing,
            } as TStateListing,
            credit: {
                products: {
                    data: {
                        credit_products: [ creditProduct ],
                    },
                } as StateCreditProducts,
                productCalculator: {
                    data: creditProduct,
                },
            } as TStateCredit,
            config: configMock.value(),
        });

        creditFilterChange({ value, field, metrika, onChange, state: store.getState() });

        expect(onChange).toHaveBeenCalledWith(maxInitialFee, field);
    });
});
