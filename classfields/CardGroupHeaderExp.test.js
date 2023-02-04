const React = require('react');
const { shallow } = require('enzyme');

const CardGroupHeaderExp = require('./CardGroupHeaderExp');

it('должен отрендерить блок с ценой, если передана цена', () => {
    const tree = shallow(
        <CardGroupHeaderExp
            mark="BMW"
            model="X3"
            superGen="III"
            bodyType="Внедорожник 5 дв"
            nameplate="20d xDrive"
            minPrice={ 1164000 }
        />,
    );
    expect(
        tree.find('.CardGroupHeader__priceBlock'),
    ).toHaveLength(1);
});

it('не должен отрендерить блок с ценой, если цена не передана', () => {
    const tree = shallow(
        <CardGroupHeaderExp
            mark="BMW"
            model="X3"
            superGen="III"
            bodyType="Внедорожник 5 дв"
            nameplate="20d xDrive"
        />,
    );
    expect(
        tree.find('.CardGroupHeader__priceBlock'),
    ).toHaveLength(0);
});
