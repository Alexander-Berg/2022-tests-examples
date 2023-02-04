require('jest-enzyme');
const React = require('react');
const { shallow } = require('enzyme');

const AmpTabs = require('./AmpTabs');

const tabs = [
    { name: 'Таб1', content: '1' },
    { name: 'Таб2', content: '2' },
    { name: 'Таб3', content: '3' },
];

it('должен отрендерить табы со строкой фильтров', () => {
    const wrapper = shallow(
        <AmpTabs
            id="tabs"
            tabs={ tabs }
        />,
    );

    expect(wrapper.find('.AmpTabs__filter .tab')).toExist();
});

it('должен отрендерить табы без строки фильров, если всего 1 таб', () => {
    const wrapper = shallow(
        <AmpTabs
            id="tabs"
            tabs={ [ tabs[0] ] }
        />,
    );

    expect(wrapper.find('.AmpTabs__filter .tab')).not.toExist();
});

it('должен скрыть табы, если передан пустой массив табов', () => {
    const wrapper = shallow(
        <AmpTabs
            id="tabs"
            tabs={ [] }
        />,
    );

    expect(wrapper.find('.AmpTabs')).not.toExist();
});
