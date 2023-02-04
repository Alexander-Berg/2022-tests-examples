const React = require('react');
const { shallow } = require('enzyme');

const _ = require('lodash');

const sleep = require('auto-core/lib/sleep');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const dealerCreditConfig = require('auto-core/react/dataDomain/banks/mocks/dealerCreditConfig.mock');

const BankFormCalculator = require('./BankFormCalculator');

const offerWithDealerCredit = cloneOfferWithHelpers(offerMock)
    .withDealerCredit()
    .withDiscountOptions({ credit: 10000 })
    .value();

const defaultData = { amount: 750000, term: 5, fee: 105000, monthlyPayment: 15350 };
const defaultDealerProps = {
    creditConfig: dealerCreditConfig,
    data: defaultData,
    offer: offerWithDealerCredit,
    onDataChange: () => {},
};

it('верно обрабатывает стоимость тачки меньше минимальной по банку', async() => {
    const props = {
        offer: cloneOfferWithHelpers(offerMock)
            .withPrice(7600)
            .value(),
        creditConfig: dealerCreditConfig,
        data: {
            amount: null,
            term: null,
            fee: null,
        },
        onDataChange: () => {},
    };
    const tree = shallowRenderComponent(props);

    await sleep(500);

    expect(tree.state()).toMatchObject({
        amount: 10000,
        fee: 0,
        term: 5,
    });
});

// it('не должен сбрасывать значения на дефолтные, если размер кредита - 0', () => {
//     const props = _.cloneDeep(defaultDealerProps);
//     props.data.amount = 0;
//     const tree = shallowRenderComponent(props);

//     expect(tree.state()).toEqual({
//         amount: 0,
//         fee: 855000,
//         monthlyPayment: 0,
//         term: 5,
//         isModalVisible: false,
//     });
// });

// it('должен сбрасывать значения на дефолтные, если нет данных по размеру кредита', () => {
//     const props = _.cloneDeep(defaultDealerProps);
//     props.data.amount = undefined;
//     const tree = shallowRenderComponent(props);

//     expect(tree.state()).toEqual({ ...defaultData, isModalVisible: false });
// });

// it('должен сбрасывать значения на дефолтные, если нет данных по выбранному периоду', () => {
//     const props = _.cloneDeep(defaultDealerProps);
//     props.data.term = undefined;
//     const tree = shallowRenderComponent(props);

//     expect(tree.state()).toEqual({ ...defaultData, isModalVisible: false });
// });

it('должен показать подсказку, если проп isDealerForm = true и есть скидки за кредит', () => {
    const props = _.cloneDeep(defaultDealerProps);
    props.isDealerForm = true;
    const tree = shallowRenderComponent(props);

    expect(tree.find('.BankFormCalculator__amountNote')).toExist();
});

it('должен показать подсказку в мке, если проп isDealerForm = true и есть скидки за кредит', () => {
    const props = _.cloneDeep(defaultDealerProps);
    props.isDealerForm = true;
    props.isMobile = true;
    const tree = shallowRenderComponent(props);

    expect(tree.find('.BankFormCalculator__priceTooltip')).toExist();
});

function shallowRenderComponent(props = defaultDealerProps) {
    return shallow(
        <BankFormCalculator { ...props }/>,
        { context: { ...contextMock } },
    );
}
