const React = require('react');
const { shallow } = require('enzyme');

const SortOffersFilter = require('./SortOffersFilter');

const items = [
    {
        'default': true,
        text: 'По умолчанию (актуальные)',
        val: 'fresh_relevance_1-desc',
    },
    {
        text: 'По дате',
        val: 'cr_date-desc',
    },
];

it('должен вызвать onChange с дефолтной сортировкой, если отжали выбранное значение', () => {
    const onChange = jest.fn();
    const tree = shallow(
        <SortOffersFilter
            items={ items }
            onChange={ onChange }
            value="cr_date-desc"
        />,
    );
    tree.find('Select').simulate('change', [], { foo: 'bar' });
    expect(onChange).toHaveBeenCalledWith([ 'fresh_relevance_1-desc' ], { foo: 'bar' });
});

it('должен вызвать onChange с выбранной сортировкой', () => {
    const onChange = jest.fn();
    const tree = shallow(
        <SortOffersFilter
            items={ items }
            onChange={ onChange }
            value="fresh_relevance_1-desc"
        />,
    );
    tree.find('Select').simulate('change', [ 'cr_date-desc' ], { foo: 'bar' });
    expect(onChange).toHaveBeenCalledWith([ 'cr_date-desc' ], { foo: 'bar' });
});
