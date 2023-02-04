const React = require('react');
const { shallow } = require('enzyme');

const listingMock = require('www-cabinet/react/dataDomain/callsNumbers/mocks/listing');
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const CallsNumbersTable = require('./CallsNumbersTable');

describe('render', () => {
    it('должен вернуть пустой листинг', () => {
        const tree = shallow(
            <CallsNumbersTable
                listing={ [] }
            />, { context: contextMock },
        );
        tree.instance().renderEmptyRow = () => 'EmptyRow';

        expect(tree.instance().render()).toMatchSnapshot();
    });

    it('должен вернуть листинг с данными', () => {
        const tree = shallow(
            <CallsNumbersTable
                listing={ listingMock }
            />, { context: contextMock },
        );
        tree.instance().renderTableRow = () => 'TableRow';

        expect(tree.instance().render()).toMatchSnapshot();
    });
});

it('renderEmptyRow: должен вернуть элемент-плейсхолдер', () => {
    const tree = shallow(
        <CallsNumbersTable
            listing={ [] }
        />, { context: contextMock },
    );

    expect(tree.instance().renderEmptyRow()).toMatchSnapshot();
});

describe('renderTableRow', () => {
    it('должен вернуть элемент списка', () => {
        const tree = shallow(
            <CallsNumbersTable
                hasMultipostingRow={ true }
                listing={ [] }
                hasButtonRow={ true }
                renderButton={ () => 'Кнопка' }
            />, { context: contextMock },
        );
        tree.instance().getOfferDescription = () => 'offerDescription';

        expect(tree.instance().renderTableRow(listingMock[0])).toMatchSnapshot();
    });
});

describe('renderTargetCell', () => {
    it('должен вернуть название категории и ссылку на оффер', () => {
        const tree = shallow(
            <CallsNumbersTable
                listing={ [] }
                isManager={ true }
            />, { context: contextMock },
        );

        expect(tree.instance().renderTargetCell(listingMock[0])).toMatchSnapshot();
    });

    it('должен вернуть только название категории', () => {
        const tree = shallow(
            <CallsNumbersTable
                listing={ [] }
                isManager={ true }
            />, { context: contextMock },
        );

        expect(tree.instance().renderTargetCell(listingMock[2])).toMatchSnapshot();
    });

    it('должен вернуть название категории и секцию', () => {
        const tree = shallow(
            <CallsNumbersTable
                listing={ [] }
                isManager={ true }
            />, { context: contextMock },
        );

        expect(tree.instance().renderTargetCell(listingMock[1])).toMatchSnapshot();
    });

    it('должен вернуть Легковые автомобили, если передана категория CARS, но не передана секция', () => {
        const tree = shallow(
            <CallsNumbersTable
                listing={ [] }
                isManager={ true }
            />, { context: contextMock },
        );

        expect(tree.instance().renderTargetCell(listingMock[3])).toMatchSnapshot();
    });
});
