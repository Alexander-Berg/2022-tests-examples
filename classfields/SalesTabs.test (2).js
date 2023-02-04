const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const SalesTabs = require('./SalesTabs');

const ContextProvider = createContextProvider(contextMock);

describe('SalesTabs - компонент для отрисовки вкладок для объявлений', () => {
    it('должен отрендерить вкладки с активной "мото"', () => {
        const wrapper = shallow(
            <ContextProvider>
                <SalesTabs
                    params={{ category: 'moto' }}
                />
            </ContextProvider>).dive();

        expect(wrapper.childAt(3).hasClass('SalesTabs__Tab_active')).toBe(true);
    });

    it('должен отрендерить дополнительную активную вкладку с правильным заголовком', () => {
        const extraTabs = [
            { category: 'c2b-auction', condition: true, title: 'Тест' },
        ];

        const wrapper = shallow(
            <ContextProvider>
                <SalesTabs
                    params={{ category: 'c2b-auction' }}
                    extraTabs={ extraTabs }
                />
            </ContextProvider>).dive();

        expect(wrapper.childAt(4).hasClass('SalesTabs__Tab_active')).toBe(true);
        expect(wrapper.childAt(4).dive().text()).toBe('Тест');
    });
});
