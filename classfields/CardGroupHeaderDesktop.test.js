const React = require('react');
const { shallow } = require('enzyme');

const CardGroupHeaderDesktop = require('./CardGroupHeaderDesktop');

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
        <CardGroupHeaderDesktop
            mark="BMW"
            model="X3"
            superGen="III"
            bodyType="Внедорожник 5 дв"
            nameplate="20d xDrive"
            minPrice={ 1164000 }
            groupInfo={ groupInfoMock }
        />,
    );
    expect(
        tree.find('.CardGroupHeaderDesktop__priceBlock'),
    ).toHaveLength(1);
});

it('не должен отрендерить блок с ценой, если цена не передана', () => {
    const tree = shallow(
        <CardGroupHeaderDesktop
            mark="BMW"
            model="X3"
            superGen="III"
            bodyType="Внедорожник 5 дв"
            nameplate="20d xDrive"
            groupInfo={ groupInfoMock }
        />,
    );
    expect(
        tree.find('.CardGroupHeaderDesktop__priceBlock'),
    ).toHaveLength(0);
});
