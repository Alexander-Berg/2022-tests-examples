const React = require('react');
const { shallow } = require('enzyme');
const MockDate = require('mockdate');

let YearFilter;
let ITEMS;
beforeEach(() => {
    MockDate.set('2021-01-01');
    YearFilter = require('./YearFilter');
    ITEMS = require('auto-core/lib/selectOptions/year');
});

it('должен рендерить все года, если не передается yearLimits', () => {
    const tree = shallow(
        <YearFilter
            name="name"
            onChange={ jest.fn() }
        />,
    );

    const children = tree.children();

    // годы начинаются с 1890, поэтому сравниваем только длину массива
    expect(children).toHaveLength(ITEMS.length);
});

it('должен рендерить года только в переданных пределах, если есть yearLimits', () => {
    const tree = shallow(
        <YearFilter
            name="name"
            onChange={ jest.fn() }
            yearLimits={{ min_year: 2018, max_year: 2020 }}
        />,
    );

    const children = tree.children();
    const childrenValues = children.map(year => year.props().value);

    expect(childrenValues).toEqual([ 2020, 2019, 2018 ]);
});

it('должен рендерить года только в переданных пределах, если есть только ограничение снизу', () => {
    const tree = shallow(
        <YearFilter
            name="name"
            onChange={ jest.fn() }
            yearLimits={{ min_year: 2017 }}
        />,
    );

    const children = tree.children();
    const childrenValues = children.map(year => year.props().value);

    expect(childrenValues).toEqual([ 2021, 2020, 2019, 2018, 2017 ]);
});

it('должен рендерить года только в переданных пределах, если есть только ограничение сверху', () => {
    const tree = shallow(
        <YearFilter
            name="name"
            onChange={ jest.fn() }
            yearLimits={{ max_year: 2017 }}
        />,
    );

    const children = tree.children();

    // годы начинаются с 1890, поэтому сравниваем только длину массива
    expect(children).toHaveLength(ITEMS.length - 4);
});
