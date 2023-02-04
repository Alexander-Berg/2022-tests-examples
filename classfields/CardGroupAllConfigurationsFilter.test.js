const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const _ = require('lodash');

const CardGroupAllConfigurationsFilter = require('./CardGroupAllConfigurationsFilter');

const ITEMS = [ 'Автомат', 'Роботизированная', 'Вариатор', 'Механика' ];

it('должен корректно отрендерить фильтр блока "Все конфигурации"', () => {
    const tree = shallow(
        <CardGroupAllConfigurationsFilter
            items={ ITEMS }
            index={ 0 }
            onChange={ _.noop }
            placeholder="Трансмиссия"
            values={ [] }
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});
