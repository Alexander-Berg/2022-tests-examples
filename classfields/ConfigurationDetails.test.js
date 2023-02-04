const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const ContextProvider = createContextProvider(contextMock);

const Configurationetails = require('./index.jsx');

const DETAILS = {
    bodyType: 'SEDAN',
    desc: 'Audi A8 IV (D5) – седан, полный привод. Автомат. Бензиновые и дизельные двигатели мощностью от 286 до 460 лошадиных сил.',
    gallery: [
        {
            cattouch: {
                url: '//avatars.mds.yandex.net/get-verba/216201/2a000001609035b09a3f13aee990c99a9ca5/cattouch',
            },
            cattouchr: {
                url: '//avatars.mds.yandex.net/get-verba/216201/2a000001609035b09a3f13aee990c99a9ca5/cattouchret',
            },
            main: {
                url: '//avatars.mds.yandex.net/get-verba/216201/2a000001609035b09a3f13aee990c99a9ca5/auto_main',
            },
        },
    ],
    panorama: null,
    prices: {
        'new': {
            listingParams: {
                category: 'cars',
                configuration_id: '21040152',
                mark: 'audi',
                model: 'a8',
                section: 'new',
                super_gen: '21040120',
            },
            value: '6 050 000 – 7 610 000',
        },
        used: {
            listingParams: {
                category: 'cars',
                configuration_id: '21040152',
                mark: 'audi',
                model: 'a8',
                section: 'used',
                super_gen: '21040120',
            },
            value: '6 150 000 – 8 275 014',
        },
    },
};

it('должен корректно отрендериться c onlyContent === false', () => {
    const tree = shallow(
        <ContextProvider>
            <Configurationetails
                details={ DETAILS }
                newRelatedCount={ 0 }
                onlyContent={ false }
            />
        </ContextProvider>,
    ).dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен корректно отрендериться c onlyContent === true', () => {
    const tree = shallow(
        <ContextProvider>
            <Configurationetails
                details={ DETAILS }
                newRelatedCount={ 0 }
                onlyContent={ true }
            />
        </ContextProvider>,
    ).dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен корректно отрендериться со ссылкой на новые', () => {
    const tree = shallow(
        <ContextProvider>
            <Configurationetails
                details={ DETAILS }
                newRelatedCount={ 10 }
                onlyContent={ false }
            />
        </ContextProvider>,
    ).dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('не должен содержать ссылку на новые при их отсутствии', () => {
    const tree = shallow(
        <ContextProvider>
            <Configurationetails
                details={ DETAILS }
                newRelatedCount={ 0 }
                onlyContent={ false }
            />
        </ContextProvider>,
    ).dive();
    expect(tree.find('.ConfigurationDetails__new_offers')).toHaveLength(0);
});

it('должен содержать ссылку на новые при их наличии', () => {
    const tree = shallow(
        <ContextProvider>
            <Configurationetails
                details={ DETAILS }
                newRelatedCount={ 1 }
                onlyContent={ false }
            />
        </ContextProvider>,
    ).dive();
    expect(tree.find('.ConfigurationDetails__new_offers')).toHaveLength(1);
});
