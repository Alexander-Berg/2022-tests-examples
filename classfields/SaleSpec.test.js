const React = require('react');
const SaleSpec = require('./SaleSpec');
const { shallow } = require('enzyme');

it('должен вернуть корректный компонент', () => {
    const SaleSpecInstance = shallow(
        <SaleSpec
            isShowingAvtokod={ true }
            category={{ alias: 'cars' }}
            tableItems={ [
                { item: 500000 },
                { item: '1.6 MT (105 л.с.)' },
                { item: 'Передний' },
                { item: 'Универсал 5 дв.' },
                { item: 'Универсал 5 дв.' },
                { item: 'Бензин' },
                { item: 'Z6F6XXEEC6FU06862' },
            ] }
        />,
    ).instance();
    SaleSpecInstance.renderTable = () => 'table';

    expect(SaleSpecInstance.render()).toMatchSnapshot();
});

it('renderTable должен вернуть корректный компонент', () => {
    const SaleSpecInstance = shallow(
        <SaleSpec
            tableItems={ [
                { item: 500000 },
                { item: '1.6 MT (105 л.с.)' },
                { item: 'Передний' },
                { item: 'Универсал 5 дв.' },
                { item: 'Бензин' },
                { item: 'Z6F6XXEEC6FU06862' },
            ] }
        />,
    ).instance();

    expect(SaleSpecInstance.renderTable()).toMatchSnapshot();
});

describe('renderCell тесты:', () => {
    it('должен вернуть null, если !cell', () => {
        const SaleSpecInstance = shallow(
            <SaleSpec
                single={ true }
            />,
        ).instance();

        expect(SaleSpecInstance.renderCell(undefined)).toBeNull();
    });

    it('должен вернуть корректный компонент', () => {
        const SaleSpecInstance = shallow(
            <SaleSpec
                single={ true }
            />,
        ).instance();

        expect(SaleSpecInstance.renderCell({ item: '123456789' })).toMatchSnapshot();
    });

    it('должен вернуть корректный компонент, если hasTooltip', () => {
        const SaleSpecInstance = shallow(
            <SaleSpec
                single={ true }
            />,
        ).instance();

        expect(SaleSpecInstance.renderCell({ item: '123456789', hasTooltip: true })).toMatchSnapshot();
    });
});
