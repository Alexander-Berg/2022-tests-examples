const React = require('react');
const { shallow } = require('enzyme');

const TabsItem = require('auto-core/react/components/desktop/Tabs/TabsItem');

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

        expect(wrapper.find(TabsItem)).toHaveLength(4);
        expect(wrapper.childAt(3).prop('active')).toBe(true);
    });

    it('должен отрендерить дополнительную вкладку "Аукцион", если есть заявки для c2b-аукциона', () => {
        const extraTabs = [
            {
                title: 'Аукцион',
                category: 'c2b-auction',
                url: 'my-c2b-auction',
                condition: true,
            },
        ];

        const wrapper = shallow(
            <ContextProvider>
                <SalesTabs
                    params={{ category: 'moto' }}
                    extraTabs={ extraTabs }
                />
            </ContextProvider>).dive();

        expect(wrapper.find(TabsItem)).toHaveLength(5);
        expect(wrapper.childAt(4).dive().text()).toBe('Аукцион');
    });
});
