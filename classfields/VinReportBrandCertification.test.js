const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const VinReportBrandCertification = require('./VinReportBrandCertification');

it('VinReportBrandCertification рендерит VinReportLoading, если данные еще не пришли', async() => {
    const wrapper = shallow(
        <Provider store={ mockStore({ bunker: {} }) }>
            <VinReportBrandCertification certification={{
                header: {
                    title: 'Было дело',
                    timestamp_update: '1571028005586',
                    is_updating: true,
                },
            }}/>
        </Provider>,
    ).dive().dive();

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});
