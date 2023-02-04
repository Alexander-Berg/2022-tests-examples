const { shallow } = require('enzyme');
const React = require('react');
const _ = require('lodash');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const ListingSeoTagCarouselItem = require('./ListingSeoTagCarouselItem');

const ContextProvider = createContextProvider(contextMock);
const onClick = _.noop;

it('Должен строить ссылку на поиск по кузову', () => {
    const searchParameters = {
        catalog_filter: [ {} ],
    };
    const params = { catalog_filter: [], body_type_group: [ 'SEDAN' ] };
    const wrapper = shallow(
        <ContextProvider>
            <ListingSeoTagCarouselItem
                text="Седан"
                searchParamName="body_type_group"
                searchParamValue="SEDAN"
                params={ params }
                searchParameters={ searchParameters }
                onClick={ onClick }
                url="link/listing/?body_type_group=SEDAN"
            />
        </ContextProvider>,
    ).dive();
    expect(wrapper.find('Link').props().url).toBe('link/listing/?body_type_group=SEDAN');
});

it('Должен строить ссылку на поиск по кузову и марке', () => {
    const searchParameters = {
        catalog_filter: [ {
            mark: 'AUDI',
        } ],
    };
    const params = { body_type_group: [ 'SEDAN' ] };
    const wrapper = shallow(
        <ContextProvider>
            <ListingSeoTagCarouselItem
                text="Седан"
                searchParamName="body_type_group"
                searchParamValue="SEDAN"
                params={ params }
                searchParameters={ searchParameters }
                onClick={ onClick }
                url="link/listing/?catalog_filter=mark%3DAUDI&body_type_group=SEDAN"
            />
        </ContextProvider>,
    ).dive();
    expect(wrapper.find('Link').props().url).toBe('link/listing/?catalog_filter=mark%3DAUDI&body_type_group=SEDAN');
});

it('Должен строить ссылку на ценовых пресетах без mmm с параметром "price_to"', () => {
    const searchParameters = {
        catalog_filter: [ {
            mark: 'AUDI',
        } ],
    };
    const itemParams = {
        price_to: 100000,
        currency: null,
        sort: null,
    };
    const wrapper = shallow(
        <ContextProvider>
            <ListingSeoTagCarouselItem
                text="100 000 ₽"
                params={ itemParams }
                searchParamName="price_to"
                searchParamValue={ 100000 }
                searchParameters={ searchParameters }
                onClick={ onClick }
                url="link/listing/?catalog_filter=mark%3DAUDI&price_to=100000&currency=&sort="
            />
        </ContextProvider>,
    ).dive();
    expect(wrapper.find('Link').props().url).toBe('link/listing/?catalog_filter=mark%3DAUDI&price_to=100000&currency=&sort=');
});
