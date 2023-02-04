const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const ListingFiltersSearchTags = require('./ListingFiltersSearchTags');

const tagsDictionary = [
    {
        code: 'fast',
        name: 'Быстрый',
        tag: { mapping: true },
    },
    {
        code: 'big',
        name: 'Большой',
        tag: { mapping: false },
    },
    {
        code: 'pig',
        name: 'Свиновоз',
        tag: { mapping: false },
        show: true,
    },
];

it('должен правильно фильтровать теги для отображения', () => {
    const onChangeMock = jest.fn();
    const tree = shallow(
        <ListingFiltersSearchTags
            tagsDictionary={ tagsDictionary }
            onChange={ onChangeMock }
        />,
    );
    expect(shallowToJson(tree.find('Item'))).toMatchSnapshot();
});

it('должен передать в onChange список тегов и параметры контрола', () => {
    const onChangeMock = jest.fn();
    const tree = shallow(
        <ListingFiltersSearchTags
            tagsDictionary={ tagsDictionary }
            onChange={ onChangeMock }
        />,
    );

    tree.find('Tags').simulate('change', [ 'big' ]);
    expect(onChangeMock).toHaveBeenCalledWith(
        [ 'big' ], { name: 'search_tag_dict' },
    );
});

it('должен передать в value только теги из словаря', () => {
    const onChangeMock = jest.fn();
    const tree = shallow(
        <ListingFiltersSearchTags
            tags={ [ 'fast', 'other_tag' ] }
            tagsDictionary={ tagsDictionary }
            onChange={ onChangeMock }
        />,
    );

    expect(tree.find('Tags').props().value).toEqual([ 'fast' ]);
});
