import CartPresenter from '../CartPresenter';
import Dispatcher from '../../../Dispatcher';

describe('Basic tests of cart\'s presenter', () => {
    const mockData = {
        items: [{
            id: 1,
            service_order_id: 1,
            qty: '20',
            service_id: 1,
            act_id: 1,
            payload: {}
        }, {
            id: 2,
            service_order_id: 2,
            qty: '30',
            service_id: 2,
            act_id: 2,
            payload: {}
        }],
        item_count: 2
    };

    const mockCredentials = { service_id: 2, service_order_id: 2 };

    const dispatcher = new Dispatcher();

    it('Returns DOM element of view', () => {
        const presenter = new CartPresenter(dispatcher);

        expect(presenter.element).toBeInstanceOf(Element);
    });


    it('Sets cart\'s data', () => {
        const presenter = new CartPresenter(dispatcher);
        presenter.setCredentials(mockCredentials.service_id, mockCredentials.service_order_id);
        presenter.data = mockData;

        expect(presenter.data).toEqual(mockData);
        expect(presenter.count).toEqual(2);
        expect(presenter.quantity).toEqual('30');
        expect(presenter.itemIds).toEqual([1, 2]);

    });

    it('Enables and disables cart\'s view', () => {
        const presenter = new CartPresenter(dispatcher);
        presenter.enable();
        expect(presenter.enabled).toBe(true);
        presenter.enable(false);
        expect(presenter.enabled).toBe(false);
        presenter.enable();
        expect(presenter.enabled).toBe(true);
        presenter.disable();
        expect(presenter.enabled).toBe(false);
    });


    it('Sets cart\'s credentials', () => {
        const presenter = new CartPresenter(dispatcher);
        expect(presenter.isCredentialsSet()).toBe(false);
        presenter.setCredentials(mockCredentials.service_id, mockCredentials.service_order_id);
        expect(presenter.isCredentialsSet()).toBe(true);
    });

    it('Invokes CREATE request on cart click', () => {
        const fn = jest.fn();
        dispatcher.subscribe(Dispatcher.EVENT.CART.REDIRECT, fn);


        const presenter = new CartPresenter(dispatcher);
        const event = new MouseEvent('click');
        presenter.element.dispatchEvent(event);

        expect(fn).toBeCalled();
    });
});
