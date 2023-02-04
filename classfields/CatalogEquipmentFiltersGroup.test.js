const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const CatalogEquipmentFiltersGroup = require('./CatalogEquipmentFiltersGroup');

it('должен отрендерить сначала селекты, потом чекбоксы, причем ровно в том порядке, в котором они пришли', () => {
    // NOTE: чекбоксы и селекты могут идти вперемешку
    const category = {
        name: 'название',
        groups: [
            {
                options: [
                    { code: 'code1', name: 'чекбокс1' },
                    { code: 'code2', name: 'чекбокс2' },
                ],
            },
            { name: 'селект1', options: [] },
            {
                options: [
                    { code: 'code3', name: 'чекбокс3' },
                    { code: 'code4', name: 'чекбокс4' },
                ],
            },
            { name: 'селект2', options: [] },
        ],
    };

    const wrapper = shallow(<CatalogEquipmentFiltersGroup category={ category } onChange={ jest.fn() }/>);
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
