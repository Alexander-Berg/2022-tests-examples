const _ = require('lodash');
const React = require('react');

const BillingVinReportSelector = require('./BillingVinReportSelector').default;
const { shallow } = require('enzyme');
const billingStateMock = require('auto-core/react/dataDomain/billing/mocks/billing');

let defaultProps;
beforeEach(() => {
    defaultProps = {
        disabled: false,
        onTabChange: jest.fn(),
        value: '1',
        bundles: _.cloneDeep(billingStateMock.reportsBundles),
    };
});

it('сменит вкладку при клике на неё', () => {
    const selectingBundleCount = defaultProps.bundles[1].counter;
    const page = shallow(<BillingVinReportSelector { ...defaultProps }/>);

    const radioGroup = page.find('RadioGroup');
    radioGroup.simulate('change', selectingBundleCount);

    expect(defaultProps.onTabChange).toHaveBeenCalledTimes(1);
    expect(defaultProps.onTabChange).toHaveBeenCalledWith(selectingBundleCount);
});

describe('не покажет лейбл со скидкой', () => {
    it('для вкладки из одного отчёта', () => {
        const page = shallow(<BillingVinReportSelector { ...defaultProps }/>);

        const firstTab = page.find('.BillingVinReportSelector__radio').at(0);
        expect(firstTab.find('.BillingVinReportSelector__discountLabel')).toHaveLength(0);
    });

    it('если скидка меньше 20%', () => {
        const props = _.cloneDeep(defaultProps);
        props.bundles[1].price.effective_price = '99900';
        const page = shallow(<BillingVinReportSelector { ...props }/>);

        const secondTab = page.find('.BillingVinReportSelector__radio').at(1);
        expect(secondTab.find('.BillingVinReportSelector__discountLabel')).toHaveLength(0);
    });
});

it('покажет лейбл со скидкой если она больше 20%', () => {
    const page = shallow(<BillingVinReportSelector { ...defaultProps }/>);

    const secondTab = page.find('.BillingVinReportSelector__radio').at(1);
    expect(secondTab.find('.BillingVinReportSelector__discountLabel')).toHaveLength(1);
});
