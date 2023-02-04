/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
}));

const React = require('react');
const { shallow } = require('enzyme');
const _ = require('lodash');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const servicesMock = require('autoru-frontend/www-desktop-lk/react/components/Sales/testData/services.mock');
const getResource = require('auto-core/react/lib/gateApi').getResource;
const predictBuyers = require('auto-core/server/resources/publicApiUserOffers/methods/predictBuyers.nock.fixtures');
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const { TOfferVas } = require('auto-core/types/proto/auto/api/api_offer_model');
const { WEEK } = require('auto-core/lib/consts');

const OfferHideDialog = require('./OfferHideDialog');

const storeMock = {
    state: {
        hideModalOpened: true,
        hideModalParams: { offerID: '123-456', category: 'testCategory' },
    },
    bunker: getBunkerMock([ 'common/donations_in_lk' ]),
};

const props = {
    params: {
        offerID: '123-456',
        category: 'test-category',
    },
};

const TEST_REASON = 'TEST_REASON';

let store;
let context;

beforeEach(() => {
    store = mockStore(storeMock);
    context = contextMock;
});

afterEach(() => {
    getResource.mockClear();
    jest.restoreAllMocks();
});

it('должен вызвать правильный action при попытке закрыть окно без снятия объявления', () => {
    const wrapper = shallow(<OfferHideDialog store={ store } { ...props }/>, { context: context }).dive();
    wrapper.instance().closeDialog();

    expect(store.getActions()).toEqual([ { type: 'CLOSE_HIDE_OFFER_MODAL' } ]);
});

it('должен закрыть модал и показать нотифайку при успешном снятии с продажи', () => {
    return new Promise((resolve, reject) => {
        const expectedActions = [
            {
                payload: { message: 'Статус объявления изменен', view: 'success' },
                type: 'NOTIFIER_SHOW_MESSAGE',
            },
            {
                payload: { offer: { foo: 'bar' }, reloadPageAfterClose: true },
                type: 'OPEN_REVIEWS_PROMO_MODAL',
            },
            {
                type: 'CLOSE_HIDE_OFFER_MODAL',
            },
        ];
        const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: context }).dive();
        const mockResponse = Promise.resolve({ status: 'SUCCESS' });
        getResource.mockImplementation(() => mockResponse);
        wrapper.instance().hideOffer({});

        setTimeout(() => {
            try {
                expect(store.getActions()).toEqual(expectedActions);
                resolve();
            } catch (e) {
                reject(e);
            }
        }, 100);
    });
});

it('должен закрыть модал и показать нотифайку на ошибку снятия с продажи', () => {
    return new Promise((resolve, reject) => {
        const expectedActions = [
            {
                type: 'NOTIFIER_SHOW_MESSAGE',
                payload: { message: 'Не удалось изменить статус объявления', view: 'error' },
            },
            {
                type: 'CLOSE_HIDE_OFFER_MODAL',
            },
        ];

        const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: context }).dive();
        const mockResponse = Promise.resolve({ status: 'ERROR' });
        getResource.mockImplementation(() => mockResponse);
        wrapper.instance().hideOffer({});

        setTimeout(() => {
            try {
                expect(store.getActions()).toEqual(expectedActions);
                resolve();
            } catch (e) {
                reject(e);
            }
        }, 100);
    });
});

it('должен вызывать getResource с правильными параметрами', () => {
    return new Promise((resolve, reject) => {
        const mockUserData = {
            applyContract: false,
            buyerPhone: '+79999999',
            manySpamCalls: false,
            reason: 'SOLD_ON_AUTORU',
            sellerComment: '',
            soldPrice: '1000000',
        };
        const expectedParams = {
            offerID: '123-456',
            category: 'testCategory',
            reason: 'SOLD_ON_AUTORU',
            sold_price: 1000000,
            many_spam_calls: false,
            buyer_phone: '+79999999',
            add_to_white_list: false,
        };
        const wrapper = shallow(<OfferHideDialog store={ store } { ...props }/>, { context: context }).dive();
        const mockResponse = Promise.resolve({ status: 'SUCSESS' });
        getResource.mockImplementation(() => mockResponse);
        expect(getResource.mock.calls).toHaveLength(0);
        wrapper.instance().hideOffer(mockUserData);

        setTimeout(() => {
            try {
                expect(getResource.mock.calls).toHaveLength(1);
                expect(getResource).toHaveBeenCalledWith('offerHide', expectedParams);
                resolve();
            } catch (e) {
                reject(e);
            }
        }, 100);
    });
});

it('должен открыть ссылку на ДКП', () => {
    let window;
    global.open = jest.fn(() => {
        return window = { location: {} };
    });
    const wrapper = shallow(<OfferHideDialog offer={{ id: '12345-67890' }} store={ store } { ...props }/>, { context }).dive();
    const mockResponse = Promise.resolve({ status: 'SUCSESS' });
    getResource.mockImplementation(() => mockResponse);
    expect(global.open).not.toHaveBeenCalled();
    wrapper.instance().hideOffer({ applyContract: true });
    expect(global.open).toHaveBeenCalled();
    expect(window.location.href).toEqual('link/docs-dkp/?sale_id=12345-67890');
});

it('должен правильно отображать активные услуги в модале', () => {
    const wrapper = shallow(<OfferHideDialog store={ store } { ...props }/>, { context: context }).dive();
    expect(wrapper.dive().find('.OfferHideDialog__servicesItem')).toHaveLength(0);
    wrapper.setProps({ offer: { services: servicesMock } });
    expect(wrapper.dive().find('.OfferHideDialog__servicesItem')).toHaveLength(4);
});

it('не должен отображать протухшие услуги в модале', () => {
    const wrapper = shallow(<OfferHideDialog store={ store } { ...props }/>, { context: context }).dive();
    expect(wrapper.find('.OfferHideDialog__servicesItem')).toHaveLength(0);
    wrapper.setProps({
        offer: {
            services: [
                {
                    service: 'all_sale_color',
                    expire_date: Date.now() - 1000,
                },
            ],
        },
    });
    expect(wrapper.find('.OfferHideDialog__servicesItem')).toHaveLength(0);
});

it('не должен активировать кнопку снятия, если не указана причина снятия', () => {
    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    const hideButton = wrapper.find('Button');
    expect(hideButton.props().disabled).toBe(true);
});

it('должен активировать кнопку снятия, если указана причина снятия, оставив её неяркой для причин требующих доп. данных', () => {
    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    wrapper.setState({ reason: 'SOLD_ON_AUTORU' });
    const hideButton = wrapper.find('Button');
    expect(hideButton.props().disabled).toBe(false);
    expect(hideButton.props().color).toBe('gray');
});

it('должен активировать кнопку снятия, если указана причина снятия, сделав её яркой для причин не требующих доп. данных', () => {
    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    wrapper.setState({ reason: 'RETHINK' });
    const hideButton = wrapper.find('Button');
    expect(hideButton.props().disabled).toBe(false);
    expect(hideButton.props().color).toBe('blue');
});

it('должен обновить RadioGroup и state', () => {
    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    expect(wrapper.find('RadioGroup').props().value).toEqual('');
    expect(wrapper.state().reason).toEqual('');

    wrapper.find('RadioGroup').simulate('change', TEST_REASON);
    expect(wrapper.find('RadioGroup').props().value).toEqual(TEST_REASON);
    expect(wrapper.state().reason).toEqual(TEST_REASON);
});

it('если причина снятия "Продал на Автору", должен запросить и показать телефоны возможных покупателей', () => {
    const predictionsPromise = Promise.resolve(predictBuyers.has_predicted_phones());
    getResource.mockImplementation(() => predictionsPromise);

    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    expect(wrapper.state().predictions).toHaveLength(0);
    wrapper.find('RadioGroup').simulate('change', 'SOLD_ON_AUTORU');

    return predictionsPromise
        .then(() => {
            expect(wrapper.state().predictions).toHaveLength(2);
            expect(wrapper.find('.OfferHideDialog__predictedPhones')).toHaveLength(1);
            expect(wrapper.find('.OfferHideDialog__predictedPhone')).toHaveLength(4);
        });
});

it('если причина снятия "Продал на Автору", должен запросить телефоны и, если их нет, сразу спросить телефон покупателя', () => {
    const predictionsPromise = Promise.resolve(predictBuyers.no_predicted_phones());
    getResource.mockImplementation(() => predictionsPromise);

    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    expect(wrapper.state().predictions).toHaveLength(0);
    wrapper.instance().inputOtherPhone.current = { focus: jest.fn() };
    wrapper.find('RadioGroup').simulate('change', 'SOLD_ON_AUTORU');

    return predictionsPromise.then(
        async() => {
            await new Promise((resolve) => setTimeout(resolve));
            expect(wrapper.state().predictions).toHaveLength(0);
            expect(wrapper.state().approvedPhone).toEqual('otherPhone');
            expect(wrapper.find('TextInput[name="phone"]')).toHaveLength(1);
            expect(wrapper.instance().inputOtherPhone.current.focus).toHaveBeenCalled();
        },
    );
});

it('если показали возможные телефоны, но выбрано "Никому", спрашиваем телефон покупателя прямо', () => {
    const predictionsPromise = Promise.resolve(predictBuyers.has_predicted_phones());
    getResource.mockImplementation(() => predictionsPromise);

    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    wrapper.find('RadioGroup').simulate('change', 'SOLD_ON_AUTORU');
    wrapper.instance().inputOtherPhone.current = { focus: jest.fn() };

    return predictionsPromise
        .then(() => {
            wrapper.find('.OfferHideDialog__predictedPhones').simulate('change', 'otherPhone');
            expect(wrapper.find('TextInput[name="phone"]')).toHaveLength(1);
            expect(wrapper.instance().inputOtherPhone.current.focus).toHaveBeenCalled();
        });
});

it('должен обновить чекбоксы и state manySpamCalls', () => {
    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    const callsCheckbox = wrapper.find('Checkbox[name="manySpamCalls"]');
    expect(wrapper.state().manySpamCalls).toBe(false);

    callsCheckbox.simulate('check', true);
    expect(wrapper.state().manySpamCalls).toBe(true);
});

it('должен обновить чекбоксы и state applyContract', () => {
    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    const contractCheckbox = wrapper.find('Checkbox[name="applyContract"]');
    expect(wrapper.state().applyContract).toBe(false);

    contractCheckbox.simulate('check', true);
    expect(wrapper.state().applyContract).toBe(true);
});

it('должен обрабатывать ввод цены продажи', () => {
    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    wrapper.setState({ reason: 'SOLD_ON_AUTORU' });
    wrapper.find('TextInputInteger[name="price"]').simulate('change', 1);
    expect(wrapper.state().soldPrice).toEqual(1);
});

it('должен вызвать правильный callback c правильными данными при снятии с продажи по второстепенной причине', () => {
    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    const hideOfferSpy = jest.spyOn(wrapper.instance(), 'hideOffer');

    wrapper.setState({ reason: TEST_REASON });
    wrapper.find('Button').simulate('click');

    expect(hideOfferSpy).toHaveBeenCalled();
    expect(hideOfferSpy).toHaveBeenCalledWith({
        addToWhiteList: false,
        applyContract: false,
        buyerPhone: '',
        manySpamCalls: false,
        reason: TEST_REASON,
        selectedCompetitor: '',
        sellerComment: '',
        soldPrice: null,
    });
});

it('должен вызвать правильный callback c правильными данными при снятии с продажи по причине "Продажа на Автору"', () => {
    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    const { predictions } = predictBuyers.has_predicted_phones();
    const hideOfferSpy = jest.spyOn(wrapper.instance(), 'hideOffer');

    wrapper.setState({ reason: 'SOLD_ON_AUTORU', predictions });
    wrapper.find('RadioGroup').simulate('change', predictions[0].user_phone);
    wrapper.find('TextInputInteger[name="price"]').simulate('change', 1000000);
    wrapper.find('Button').simulate('click');

    expect(hideOfferSpy).toHaveBeenCalled();
    expect(hideOfferSpy).toHaveBeenCalledWith({
        addToWhiteList: false,
        applyContract: false,
        buyerPhone: predictions[0].user_phone,
        manySpamCalls: false,
        reason: 'SOLD_ON_AUTORU',
        selectedCompetitor: '',
        sellerComment: '',
        soldPrice: 1000000,
    });
});

it('должен вызвать правильный callback c правильными данными при снятии с продажи по причине "Другое"', () => {
    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    const hideOfferSpy = jest.spyOn(wrapper.instance(), 'hideOffer');

    wrapper.setState({ reason: 'OTHER' });
    wrapper.find('TextArea').simulate('change', 'Жене подарил');
    wrapper.find('Button').simulate('click');

    expect(hideOfferSpy).toHaveBeenCalled();
    expect(hideOfferSpy).toHaveBeenCalledWith({
        addToWhiteList: false,
        applyContract: false,
        buyerPhone: '',
        manySpamCalls: false,
        reason: 'OTHER',
        selectedCompetitor: '',
        sellerComment: 'Жене подарил',
        soldPrice: null,
    });
});

it('должен передать флаг для ДКП при снятии с продажи, если он был отмечен', () => {
    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    const hideOfferSpy = jest.spyOn(wrapper.instance(), 'hideOffer');

    wrapper.setState({ reason: TEST_REASON });
    wrapper.find('Checkbox[name="applyContract"]').simulate('check', true);
    wrapper.find('Button').simulate('click');

    expect(hideOfferSpy).toHaveBeenCalled();
    expect(hideOfferSpy).toHaveBeenCalledWith({
        addToWhiteList: false,
        applyContract: true,
        buyerPhone: '',
        manySpamCalls: false,
        reason: TEST_REASON,
        selectedCompetitor: '',
        sellerComment: '',
        soldPrice: null,
    });
});

it('должен вызывать правильный callback при отмене снятия с продажи', () => {
    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    const closeDialogSpy = jest.spyOn(wrapper.instance(), 'closeDialog');

    wrapper.setState({ reason: TEST_REASON });
    wrapper.find('Link').simulate('click');
    expect(closeDialogSpy).toHaveBeenCalled();
});

it('должен показать список конкурентов и спросить цену, если выбрали "Продал где-то ещё"', () => {
    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    wrapper.find('RadioGroup').simulate('change', 'SOLD_SOMEWHERE');
    expect(wrapper.find('.OfferHideDialog__competitors')).toHaveLength(1);
    expect(wrapper.find('TextInputInteger[name="price"]')).toHaveLength(1);
});

it('должен показать поле для ввода текста, если выбрали "Другое"', () => {
    const wrapper = shallow(<OfferHideDialog offer={{ foo: 'bar' }} store={ store } { ...props }/>, { context: contextMock }).dive();
    wrapper.instance().inputSellerComment.current = { focus: jest.fn() };
    wrapper.find('RadioGroup').simulate('change', 'OTHER');
    expect(wrapper.find('TextArea')).toHaveLength(1);
    expect(wrapper.instance().inputSellerComment.current.focus).toHaveBeenCalled();
});

describe('эксп с донатами', () => {
    let offer;
    let context;

    beforeEach(() => {
        offer = cloneOfferWithHelpers(offerMock)
            .withActiveVas([])
            .withCustomVas({ service: TOfferVas.PLACEMENT, price: 0 });
        context = {
            ...contextMock,
            hasExperiment: (exp) => exp === 'AUTORUFRONT-18269_donate',
        };

        getResource.mockReturnValue(Promise.resolve({
            predictions: [ { user_phone: '79112223344', text: 'владимир владимирович' } ],
        }));
    });

    it('покажет блок при соблюдении всех условий', () => {
        const wrapper = shallow(<OfferHideDialog offer={ offer.value() } store={ store } { ...props }/>, { context }).dive();
        wrapper.find('RadioGroup').simulate('change', 'SOLD_ON_AUTORU');

        const donationBlock = wrapper.find('OfferHideDialogDonation');
        expect(donationBlock.isEmptyRender()).toBe(false);
    });

    describe('не покажет блок', () => {
        it('вне экспа', () => {
            context.hasExperiment = (exp) => exp !== 'AUTORUFRONT-18269_donate';
            const wrapper = shallow(<OfferHideDialog offer={ offer.value() } store={ store } { ...props }/>, { context }).dive();
            wrapper.find('RadioGroup').simulate('change', 'SOLD_ON_AUTORU');

            const donationBlock = wrapper.find('OfferHideDialogDonation');
            expect(donationBlock.isEmptyRender()).toBe(true);
        });

        it('у объявления дилера', () => {
            offer = offer.withSellerTypeCommercial();
            const wrapper = shallow(<OfferHideDialog offer={ offer.value() } store={ store } { ...props }/>, { context }).dive();
            wrapper.find('RadioGroup').simulate('change', 'SOLD_ON_AUTORU');

            const donationBlock = wrapper.find('OfferHideDialogDonation');
            expect(donationBlock.isEmptyRender()).toBe(true);
        });

        it('если размещение платное', () => {
            offer = offer.withCustomVas({ service: TOfferVas.PLACEMENT, price: 1 });
            const wrapper = shallow(<OfferHideDialog offer={ offer.value() } store={ store } { ...props }/>, { context }).dive();
            wrapper.find('RadioGroup').simulate('change', 'SOLD_ON_AUTORU');

            const donationBlock = wrapper.find('OfferHideDialogDonation');
            expect(donationBlock.isEmptyRender()).toBe(true);
        });

        it('если есть активный вас', () => {
            offer = offer.withActiveVas([ TOfferVas.TURBO ]);
            const wrapper = shallow(<OfferHideDialog offer={ offer.value() } store={ store } { ...props }/>, { context }).dive();
            wrapper.find('RadioGroup').simulate('change', 'SOLD_ON_AUTORU');

            const donationBlock = wrapper.find('OfferHideDialogDonation');
            expect(donationBlock.isEmptyRender()).toBe(true);
        });

        it('если период размещения был больше 2 недель', () => {
            offer = offer.withCreationDate(Date.now() - 3 * WEEK);
            const wrapper = shallow(<OfferHideDialog offer={ offer.value() } store={ store } { ...props }/>, { context }).dive();
            wrapper.find('RadioGroup').simulate('change', 'SOLD_ON_AUTORU');

            const donationBlock = wrapper.find('OfferHideDialogDonation');
            expect(donationBlock.isEmptyRender()).toBe(true);
        });

        it('если тачка не из легковых', () => {
            offer = offer.withCategory('MOTO');
            const wrapper = shallow(<OfferHideDialog offer={ offer.value() } store={ store } { ...props }/>, { context }).dive();
            wrapper.find('RadioGroup').simulate('change', 'SOLD_ON_AUTORU');

            const donationBlock = wrapper.find('OfferHideDialogDonation');
            expect(donationBlock.isEmptyRender()).toBe(true);
        });
    });

    describe('при закрытии модала', () => {
        it('отправит метрику если я жопа с ручкой', () => {
            const wrapper = shallow(<OfferHideDialog offer={ offer.value() } store={ store } { ...props }/>, { context }).dive();
            wrapper.find('RadioGroup').simulate('change', 'SOLD_ON_AUTORU');
            wrapper.find('.OfferHideDialog__buttons Link').simulate('click');

            const hasMatchedCall = context.metrika.sendPageEvent.mock.calls.some((params) => _.isEqual(params[0], [ 'after-sale-donation', 'no-thanks' ]));
            expect(hasMatchedCall).toBe(true);
        });

        it('не отправит метрику если я щедрая душа', () => {
            const wrapper = shallow(<OfferHideDialog offer={ offer.value() } store={ store } { ...props }/>, { context }).dive();
            wrapper.find('RadioGroup').simulate('change', 'SOLD_ON_AUTORU');

            const donationBlock = wrapper.find('OfferHideDialogDonation');
            donationBlock.simulate('submit');

            wrapper.find('.OfferHideDialog__buttons Link').simulate('click');

            const hasMatchedCall = context.metrika.sendPageEvent.mock.calls.some((params) => _.isEqual(params[0], [ 'after-sale-donation', 'no-thanks' ]));
            expect(hasMatchedCall).toBe(false);
        });
    });
});
