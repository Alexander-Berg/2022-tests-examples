const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const Share = require('./Share');

const Context = createContextProvider(contextMock);

it('прокидывает в YaShare seo-title карточки когда есть props.offer', () => {
    const props = {
        link: '',
        offer: cardMock,
        title: 'title',
    };

    const wrapper = shallow(
        <Context>
            <Share { ... props }>
                <span>Поделиться</span>
            </Share>
        </Context>,
    ).dive();

    wrapper.childAt(0).simulate('click');
    const yaShare = wrapper.find('Popup').find('YaShare');

    expect(yaShare.props().title)
        .toEqual('Продаю Ford EcoSport I 2017 года за 855\u00a0000 рублей на Авто.ру!');
});

it('прокидывает в YaShare title из пропсов когда нет props.offer', () => {
    const props = {
        link: '',
        title: 'title',
    };

    const wrapper = shallow(
        <Context>
            <Share { ... props }>
                <span>Поделиться</span>
            </Share>
        </Context>,
    ).dive();

    wrapper.childAt(0).simulate('click');
    const yaShare = wrapper.find('Popup').find('YaShare');

    expect(yaShare.props().title)
        .toEqual('title');
});
