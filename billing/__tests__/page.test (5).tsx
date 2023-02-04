import React from 'react';
import Enzyme, { mount, ReactWrapper } from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';
import { combineReducers, Reducer } from 'redux';
import { Provider } from 'react-redux';
import { all } from 'redux-saga/effects';
import SagaTester from 'redux-saga-tester';
import { fetchGet } from 'common/utils/old-fetch';

import { /*loggerMiddleware,*/ HOST } from 'common/utils/test-utils/common';
import withIntlProvider from 'common/utils/test-utils/with-intl-provider';
import commonReducers from 'common/reducers/common';

const { product, productFull } = require('./data');
import { ProductAction } from '../types';
import { reducers } from '../reducers';
import { ProductStateRecord } from '../reducers/product';
import { watchRequestProductData } from '../sagas/product';
import { ProductContainer } from '../containers/ProductContainer';
import { NamesContainer } from '../containers/NamesContainer';
import { PricesContainer } from '../containers/PricesContainer';
import { TaxesContainer } from '../containers/TaxesContainer';
import { MarkupsContainer } from '../containers/MarkupsContainer';
import { SeasonCoefficientContainer } from '../containers/SeasonCoefficientContainer';
import { initializeDesktopRegistry } from 'common/__tests__/registry';

jest.mock('common/utils/old-fetch');

Enzyme.configure({ adapter: new Adapter() });

describe('admin - product - container – product', () => {
    beforeAll(initializeDesktopRegistry);

    function* rootSaga() {
        yield all([watchRequestProductData()]);
    }

    function getInitialState(productId: number) {
        return {
            perms: [],
            product$details: ProductStateRecord({
                productId,
                data: null,
                isFetching: true
            })
        };
    }

    let rootReducer: Reducer = combineReducers({
        ...commonReducers,
        ...reducers
    });
    let sagaTester: SagaTester<{}>;
    let Container: Function;
    let wrapper: ReactWrapper;
    let productId = 0;

    // @ts-ignore
    fetchGet.mockResolvedValueOnce(product);
    // @ts-ignore
    fetchGet.mockResolvedValueOnce(productFull);

    beforeEach(() => {
        ++productId;

        sagaTester = new SagaTester({
            initialState: getInitialState(productId),
            reducers: rootReducer
        });

        sagaTester.start(rootSaga);

        // @ts-ignore
        const { store } = sagaTester;

        Container = withIntlProvider(() => (
            <Provider store={store}>
                <ProductContainer />
                <NamesContainer />
                <PricesContainer />
                <TaxesContainer />
                <MarkupsContainer />
                <SeasonCoefficientContainer />
            </Provider>
        ));

        wrapper = mount(<Container />);
    });

    test('open page - should request product data, should render 4 content blocks without markups & season_coefficient', async () => {
        expect(sagaTester.wasCalled(ProductAction.REQUEST)).toBe(true);

        await sagaTester.waitFor(ProductAction.RECEIVE);
        wrapper.update();

        expect(wrapper.find('Product').find('PageSection').length).toBe(1);
        expect(wrapper.find('Product').find('TableDetails').length).toBe(3);

        expect(wrapper.find('Names').find('PageSection').length).toBe(1);

        expect(wrapper.find('Prices').find('PageSection').length).toBe(1);

        expect(wrapper.find('Taxes').find('PageSection').length).toBe(1);

        expect(wrapper.find('SeasonCoefficient').find('PageSection').length).toBe(0);

        expect(wrapper.find('Markups').find('PageSection').length).toBe(0);

        // Fetch tests
        expect(fetchGet).nthCalledWith(
            1,
            `${HOST}/product`,
            {
                product_id: 1
            },
            false,
            false
        );
    });

    test('open page - should request product data, should render 6 content blocks', async () => {
        expect(sagaTester.wasCalled(ProductAction.REQUEST)).toBe(true);

        await sagaTester.waitFor(ProductAction.RECEIVE);
        wrapper.update();

        expect(wrapper.find('Product').find('PageSection').length).toBe(1);
        expect(wrapper.find('Product').find('TableDetails').length).toBe(3);

        expect(wrapper.find('Names').find('PageSection').length).toBe(1);

        expect(wrapper.find('Prices').find('PageSection').length).toBe(1);

        expect(wrapper.find('Taxes').find('PageSection').length).toBe(1);

        expect(wrapper.find('SeasonCoefficient').find('PageSection').length).toBe(1);

        expect(wrapper.find('Markups').find('PageSection').length).toBe(1);

        // Fetch tests
        expect(fetchGet).nthCalledWith(
            2,
            `${HOST}/product`,
            {
                product_id: 2
            },
            false,
            false
        );
    });
});
