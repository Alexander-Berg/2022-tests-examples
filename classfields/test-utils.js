const _ = require('lodash');

const billingStateMock = require('auto-core/react/dataDomain/billing/mocks/billing');
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const contextMock = require('autoru-frontend/mocks/contextMockBrowser');

const { cards, serviceMock } = require('../Billing.mock');

const RETURN_URL = 'http://example.com';

function prepareInitialState({ hasExperiment, isMobile } = {}) {
    const config = configStateMock.withPageParams({
        from: 'new-lk-tab',
        returnUrl: decodeURIComponent(JSON.stringify(RETURN_URL)),
    }).value();

    const billing = _.cloneDeep(billingStateMock);
    billing.paymentInfo[0].detailed_product_infos[0].prolongation_allowed = false;

    config.data.host = 'http://docker.for.mac.host.internal:1111';

    const initialState = {
        billing: billing,
        config: config,
    };

    const props = {
        isMobile,
        params: {},
    };

    const context = _.cloneDeep(contextMock);
    context.link = () => '';
    context.hasExperiment = hasExperiment || (() => {});

    return { initialState, props, context };
}

function addAutoProlongationConditionsToState({ initialState, props }) {
    initialState.billing.paymentInfo[0].detailed_product_infos = [ _.cloneDeep(serviceMock.turboPackageService) ];
    initialState.billing.paymentInfo[0].detailed_product_infos[0].prolongation_allowed = true;
    initialState.billing.paymentInfo[0].payment_methods.push(_.cloneDeep(cards.api_v3));
    props.params.from = 'new-lk-tab';
}

function putPaymentMethodToFirstPosition({ initialState }, method) {
    const newMethods = initialState.billing.paymentInfo[0].payment_methods.filter(({ id }) => id !== method.id);
    initialState.billing.paymentInfo[0].payment_methods = [ method, ...newMethods ];
}

function addBundles({ initialState }) {
    initialState.billing.bundles = [
        {
            product: 'offers-history-reports',
            duration: '31536000s',
            price: {
                effective_price: '39900',
                base_price: '39900',
            },
            counter: '1',
            ticketId: 'user:12740420-6239d1779dbe8dfd4b2258e4c2e1391a-1595361851828',
        }, {
            product: 'offers-history-reports',
            duration: '31536000s',
            price: {
                effective_price: '99000',
                base_price: '99000',
            },
            counter: '10',
        },
    ];
    initialState.billing.paymentInfo[0].detailed_product_infos = [
        {
            prolongation_allowed: false,
            duration: '31536000s',
            base_price: 399,
            service: 'offers-history-reports',
            name: 'История размещений',
            days: 365,
            effective_price: 399,
            prolongation_forced: false,
            prolongation_forced_not_togglable: false,
        },
    ];
    initialState.billing.reportsSubscription.counter = 0;
}

module.exports = {
    addBundles,
    prepareInitialState,
    addAutoProlongationConditionsToState,
    putPaymentMethodToFirstPosition,
};
