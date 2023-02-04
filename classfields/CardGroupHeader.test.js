const React = require('react');
const { shallow } = require('enzyme');

const CardGroupHeader = require('./CardGroupHeader');

const groupInfoMock = {
    mark: {
        name: 'BMW',
    },
    model: {
        name: 'X3',
    },
    generation: {
        name: 'III',
    },
};

it('должен отрендерить блок с ценой, если передана цена', () => {
    const tree = shallow(
        <CardGroupHeader
            mark={{ name: 'BMW', id: 'bmw' }}
            model={{ name: 'X3', id: 'x3' }}
            superGen={{ name: 'III', id: '123' }}
            minPrice={ 1164000 }
            groupInfo={ groupInfoMock }
        />,
    );
    expect(
        tree.find('.CardGroupHeaderMobile__price'),
    ).toHaveLength(1);
});

it('не должен отрендерить блок с ценой, если цена не передана', () => {
    const tree = shallow(
        <CardGroupHeader
            mark={{ name: 'BMW', id: 'bmw' }}
            model={{ name: 'X3', id: 'x3' }}
            superGen={{ name: 'III', id: '123' }}
            groupInfo={ groupInfoMock }
        />,
    );
    expect(
        tree.find('.CardGroupHeaderMobile__price'),
    ).toHaveLength(0);
});
