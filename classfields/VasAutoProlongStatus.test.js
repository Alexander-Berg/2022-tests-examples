/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { Provider } = require('react-redux');
const { mount } = require('enzyme');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const VasAutoProlongStatus = require('./VasAutoProlongStatus');

const store = mockStore({});

it('должен вернуть content === null, если prolongable = false и prolongation_allowed = undefined', () => {
    const vasAutoProlongStatus = mount(
        <Provider store={ store }>
            <VasAutoProlongStatus
                service={{
                    prolongable: false,
                    service: 'unknown-but-prop-is-required',
                }}
                serviceInfo={{
                    days: 20,
                    price: 200,
                    original_price: 200,
                    currency: 'RUR',
                }}
            />
        </Provider>,
    );

    expect(vasAutoProlongStatus.find('InfoPopup').props().content).toBeNull();
});
