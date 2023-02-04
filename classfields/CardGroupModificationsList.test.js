const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const CardGroupModificationsList = require('./CardGroupModificationsList').default;

const ITEMS = [
    {
        id: '1111',
        title: '1.6 л (110 л.с) дизель / Автомат / Передний',
    },
    {
        id: '2222',
        title: '1.8 л (140 л.с) бензин / Автомат / Полный',
    },
    {
        id: '3333',
        title: '2.0 л (184 л.с) бензин / Автомат / Передний',
    },
    {
        id: '4444',
        title: '2.0 л (196 л.с) дизель / Автомат / Передний',
    },
    {
        id: '5555',
        title: '1.6 л (110 л.с) дизель / Автомат / Полный',
    },
];

it('должен ничего не отрендерить, если передан пустой массив', () => {
    const tree = shallow(
        <CardGroupModificationsList
            items={ [] }
            value="1111"
            onChange={ jest.fn }
        />,
        { context: contextMock },
    );
    expect(tree).toBeEmptyRender();
});

it('должен добавить класс к корневому элементу, когда вне экрана', () => {
    const tree = shallow(
        <CardGroupModificationsList
            items={ ITEMS }
            value="1111"
            onChange={ jest.fn }
        />,
        { context: contextMock },
    );

    tree.find('InView').simulate('change', false);
    tree.update();

    expect(tree.hasClass('CardGroupModificationsList_stuck')).toBe(true);
});
