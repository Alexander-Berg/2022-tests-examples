const React = require('react');
const renderer = require('react-test-renderer');

const SalesItemPaidOffer = require('./SalesItemPaidOffer');

it('должен отрендерить компонент, когда оффер платный', () => {
    const offer = {
        services: [
            { service: 'all_sale_activate' },
        ],
    };
    const tree = renderer.create(<SalesItemPaidOffer offer={ offer }/>).toJSON();
    expect(tree).toMatchSnapshot();
});

it('не должен отрендерить компонент, когда оффер не платный', () => {
    const offer = {
        services: [
            { service: 'all_sale_fresh' },
        ],
    };
    const tree = renderer.create(<SalesItemPaidOffer offer={ offer }/>).toJSON();
    expect(tree).toBeNull();
});
