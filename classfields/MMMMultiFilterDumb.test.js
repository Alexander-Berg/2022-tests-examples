const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const MMMMultiFilterDumb = require('./MMMMultiFilterDumb');

const breadcrumbsMock = {
    data: [
        {
            meta_level: 'MARK_LEVEL',
            entities: [
                { id: 'AUDI', name: 'Audi' },
                { id: 'BMW', name: 'Бе-ем-ве' },
            ],
        },
    ],
};

// выпилить экспы, сделать нормально, вернуть тест. сейчас там пиздец какой-то в коде с этой кнопкой

// it.only('должен правильно обрабатывать добавление строки мультивыбора', () => {
//     const onChange = jest.fn();
//     const ContextProvider = createContextProvider(contextMock);

//     const wrapper = shallow(
//         <ContextProvider>
//             <MMMMultiFilterDumb
//                 breadcrumbsPublicApi={ breadcrumbsMock }
//                 catalogFilter={ [
//                     { mark: 'AUDI', model: 'A1' },
//                 ] }
//                 excludeCatalogFilter={ [
//                     { mark: 'UAZ', model: 'BUKHANKA' },
//                 ]}
//                 section="all"
//                 visible={ true }
//                 onChange={ onChange }
//             />
//         </ContextProvider>
//     ).dive();
//     console.log(wrapper.debug())
// wrapper.find('MMMMultiFilterButtonAdd').at(1).simulate('click', { index: 1 });

// expect(onChange).toHaveBeenCalledWith({
//     catalog_filter: [
//         {"mark": "AUDI", "model": "A1"},
//         {},

//     ],
//     exclude_catalog_filter: [
//         {"mark": "UAZ", "model": "BUKHANKA"}
//     ]
// }, { name: 'mmm-filter' });
// });

it('должен правильно обрабатывать удаление строки мультивыбора', () => {
    const onChange = jest.fn();
    const ContextProvider = createContextProvider(contextMock);

    const wrapper = shallow(
        <ContextProvider>
            <MMMMultiFilterDumb
                breadcrumbsPublicApi={ breadcrumbsMock }
                catalogFilter={ [
                    { mark: 'AUDI', model: 'A1' },
                    { mark: 'AUDI', model: 'A2' },
                    { mark: 'BMW', model: 'X1' },
                    { mark: 'BMW', model: 'X2' },
                ] }
                excludeCatalogFilter={ [
                    { mark: 'UAZ', model: 'BUKHANKA' },
                ] }
                onMMMControlChange={ _.noop }
                section="all"
                visible={ true }
                onChange={ onChange }
            />
        </ContextProvider>,
    ).dive();

    wrapper.find('MMMMultiFilterButtonRemove').at(1).simulate('click', { index: 1 });

    expect(onChange).toHaveBeenCalledWith({
        catalog_filter: [
            { mark: 'AUDI', model: 'A1' },
            { mark: 'AUDI', model: 'A2' },

        ],
        exclude_catalog_filter: [
            { mark: 'UAZ', model: 'BUKHANKA' },
        ],
    }, { name: 'mmm-filter' });
});

it('не должен передавать exclude в фильтр, если он не исключающий', () => {
    const ContextProvider = createContextProvider(contextMock);

    const wrapper = shallow(
        <ContextProvider>
            <MMMMultiFilterDumb
                breadcrumbsPublicApi={ breadcrumbsMock }
                catalogFilter={ [
                    { mark: 'UAZ', model: 'BUKHANKA' },
                ] }
                onMMMControlChange={ _.noop }
                section="all"
                visible={ true }
                onChange={ jest.fn() }
            />
        </ContextProvider>,
    ).dive();

    expect(wrapper.find('MMMFilter').props().exclude).toBeUndefined();
});

it('должен передавать exclude в фильтр, если он исключающий', () => {
    const ContextProvider = createContextProvider(contextMock);

    const wrapper = shallow(
        <ContextProvider>
            <MMMMultiFilterDumb
                breadcrumbsPublicApi={ breadcrumbsMock }
                catalogFilter={ [] }
                excludeCatalogFilter={ [
                    { mark: 'UAZ', model: 'BUKHANKA' },
                ] }
                onMMMControlChange={ _.noop }
                section="all"
                visible={ true }
                onChange={ jest.fn() }
            />
        </ContextProvider>,
    ).dive();

    expect(wrapper.find('MMMFilter').props().exclude).toBe(true);
});
