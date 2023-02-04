const React = require('react');
const ListingCategory = require('./ListingCategory');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const { shallow } = require('enzyme');

const ContextProvider = createContextProvider(contextMock);

const TEST_PARAMS = [
    {
        title: 'getItemLinkParams должен вернуть категорю moto_category',
        props: { category: 'moto' },
        item: { val: 'snowmobile' },
        result: { moto_category: 'snowmobile', dealer_code: undefined },
    },
    {
        title: 'getItemLinkParams должен вернуть категорю trucks_category',
        props: { category: 'trucks' },
        item: { val: 'buses' },
        result: { trucks_category: 'buses', dealer_code: undefined },
    },
    {
        title: 'getItemLinkParams должен вернуть код дилера',
        props: { category: 'trucks', dealerCode: 'germes' },
        item: { val: 'buses' },
        result: { trucks_category: 'buses', dealer_code: 'germes' },
    },
];

TEST_PARAMS.forEach((test) => {
    it(test.title, () => {
        const wrapper = shallow(
            <ContextProvider>
                <ListingCategory { ...test.props }/>
            </ContextProvider>,
        );
        const instance = wrapper.dive().instance();

        expect(instance.getItemLinkParams(test.item)).toStrictEqual(test.result);
    });
});

const TEST_ROUTRES = [
    {
        title: 'getItemLinkRoute должен вернуть commercial-listing',
        props: { category: 'trucks' },
        result: 'commercial-listing',
    },
    {
        title: 'getItemLinkRoute должен вернуть moto-listing',
        props: { category: 'moto' },
        result: 'moto-listing',
    },
    {
        title: 'getItemLinkRoute должен вернуть pageType',
        props: { category: 'moto', pageType: 'page-dealer' },
        result: 'page-dealer',
    },
];

TEST_ROUTRES.forEach((test) => {
    it(test.title, () => {
        const wrapper = shallow(
            <ContextProvider>
                <ListingCategory { ...test.props }/>
            </ContextProvider>,
        );
        const instance = wrapper.dive().instance();

        expect(instance.getItemLinkRoute()).toBe(test.result);
    });
});
