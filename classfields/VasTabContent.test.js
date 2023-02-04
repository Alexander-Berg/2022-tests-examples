const _ = require('lodash');
const React = require('react');
const VasTabContent = require('./VasTabContent');

const { shallow } = require('enzyme');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const { InView } = require('react-intersection-observer');

const initialState = {};
let props;

let context;
let vasLogParams;
contextMock.logVasEvent = jest.fn((params) => {
    vasLogParams = params;
});

beforeEach(() => {
    props = {
        offer: {
            category: 'cars',
            saleId: '1093704898-916ea010',
        },
        name: 'package_vip',
        data: [ {
            serviceID: 'package_vip',
            serviceInfo: {
                name: 'VIP',
                description: 'бла бла бла',
                service: 'package_vip',
                days: 60,
                prolongation_allowed: false,
                price: 777,
                original_price: 999,
                package_services: [
                    { service: 'all_sale_color', name: 'выделением цветом' },
                    { service: 'all_sale_fresh', name: 'поднятие в поиске' },
                    { service: 'all_sale_toplist', name: 'поднятие в топ' },
                    { service: 'all_sale_special', name: 'спецпредложение' },
                ],
            },
        } ],
        parentCategory: 'cars',
        onSubmit: jest.fn(),
    };

    context = _.cloneDeep(contextMock);

    context.logVasEvent.mockClear();
});

describe('лог васов', () => {
    it('правильно логирует событие показа при попадании кнопки в поле видимости', () => {
        const page = shallowRenderComponent({ props });
        const buttonObserver = page.find('.VasFooter').find(InView);
        buttonObserver.simulate('change', true);

        expect(context.logVasEvent).toHaveBeenCalledTimes(1);
        expect(vasLogParams).toMatchSnapshot();
    });

    it('не логирует событие показа если кнопка вне поля видимости', () => {
        const page = shallowRenderComponent({ props });
        const buttonObserver = page.find('.VasFooter').find(InView);
        buttonObserver.simulate('change', false);

        expect(context.logVasEvent).toHaveBeenCalledTimes(0);
    });

    it('не будет логировать уже активный сервис', () => {
        props.data[0].service = { service: 'package_turbo', is_active: true };
        const page = shallowRenderComponent({ props });
        const vasFooter = page.find('.VasFooter');

        expect(vasFooter.isEmptyRender()).toBe(true);
        expect(context.logVasEvent).toHaveBeenCalledTimes(0);
    });

    it('залогирует показ сервисов на последней вкладке при попадании их в поле видимости', () => {
        props.name = 'regular';
        props.data[0].serviceID = 'all_sale_toplist';
        props.data[0].serviceInfo.service = 'all_sale_toplist';
        const page = shallowRenderComponent({ props });
        const checkboxObserver = page.find('.VasItem__wrapper').find(InView).at(0);
        checkboxObserver.simulate('change', true);

        expect(context.logVasEvent).toHaveBeenCalledTimes(1);
        expect(vasLogParams).toMatchSnapshot();
    });

    it('не залогирует показ сервисов на последней вкладке если они вне поля видимости', () => {
        props.name = 'regular';
        props.data[0].serviceID = 'all_sale_toplist';
        props.data[0].serviceInfo.service = 'all_sale_toplist';
        const page = shallowRenderComponent({ props });
        const checkboxObserver = page.find('.VasItem__wrapper').find(InView).at(0);
        checkboxObserver.simulate('change', false);

        expect(context.logVasEvent).toHaveBeenCalledTimes(0);
    });
});

function shallowRenderComponent({ props }) {
    const store = mockStore(initialState);
    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <VasTabContent { ...props } store={ store }/>
        </ContextProvider>,
    );

    return wrapper.dive().dive();
}
