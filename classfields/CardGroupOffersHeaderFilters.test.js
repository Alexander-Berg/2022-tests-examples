const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const CardGroupOffersHeaderFilters = require('./CardGroupOffersHeaderFilters');

const stateMock = require('auto-core/react/dataDomain/cardGroup/mocks/state.mock');
const getCardGroupFilterItems = require('auto-core/react/dataDomain/cardGroup/selectors/getCardGroupFilterItems').default;
const getCardGroupFilterValues = require('auto-core/react/dataDomain/cardGroup/selectors/getCardGroupFilterValues');

const baseProps = {
    filterItems: getCardGroupFilterItems(stateMock),
    filterValues: getCardGroupFilterValues(stateMock),
    onChange: jest.fn,
    onSubmit: jest.fn,
};

it('должен отобразить кнопку получения списка офферов в активном состоянии, если есть офферы', () => {
    const tree = shallow(
        <CardGroupOffersHeaderFilters
            { ...baseProps }
            offersCount={ 100 }
        />,
        { context: contextMock },
    );
    expect(tree.find('ButtonWithLoader').props().disabled).toBe(false);
    expect(tree.find('ButtonWithLoader').childAt(0).text()).toContain('Показать 100');
});

it('не должен отобразить кнопку получения списка офферов в выключенном состоянии, если нет офферов', () => {
    const tree = shallow(
        <CardGroupOffersHeaderFilters
            { ...baseProps }
            offersCount={ 0 }
        />,
        { context: contextMock },
    );
    expect(tree.find('ButtonWithLoader').props().disabled).toBe(true);
    expect(tree.find('ButtonWithLoader').childAt(0).text()).toBe('Нет предложений');
});
