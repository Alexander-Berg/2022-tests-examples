const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const CatalogRelatedVideo = require('./CatalogRelatedVideo');

const store = mockStore({
    breadcrumbs: {
        data: {
            FOO: {},
            BMW: {
                id: 'BMW',
                name: 'BMW',
                models: {
                    BAR: {},
                    X5: {
                        id: 'X5',
                        name: 'X5',
                        generations: {
                            '123': {},
                            '10382710': {
                                id: '10382710',
                                name: 'III (F15)',
                                configurations: {},
                            },
                        },
                    },
                },
            },
        },
    },
});

const params = {
    category: 'cars',
    complectation_id: '10382711_21041586_20679662',
    configuration_id: '10382711',
    mark: 'bmw',
    model: 'x5',
    super_gen: '10382710',
};

it('Должен добавлять бордер с пропом withBorder', () => {
    const wrapper = shallow(
        <CatalogRelatedVideo
            params={ params }
        />, { context: { store } }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
