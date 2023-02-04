import CartView from '../CartView';
import CartModel from '../CartModel';

describe('Basic tests of cart\'s view', () => {
    it('Creates DOM element', () => {
        const model = new CartModel(()=>{});
        const view = new CartView(model, ()=>{});

        expect(view.element).toBeInstanceOf(Element);
    });

    it('Updates template', () => {
        const model1 = new CartModel(()=>{});
        const model2 = new CartModel(()=>{});
        model2.items = [1]

        const view = new CartView(model1, ()=>{});
        const initialContents = view.element.innerHTML;
        view.update(model2);

        expect(initialContents).not.toEqual(view.element.template);
    });

    it('Initialises in disabled state', () => {
        const model = new CartModel(()=>{});
        const view = new CartView(model, ()=>{});

        expect(view.element.classList.contains('b-cart_disabled')).toBe(true);
    });

    it('Removes disable class on enable', () => {
        const model = new CartModel(()=>{});
        const view = new CartView(model, ()=>{});
        view.enable(true);

        expect(view.element.classList.contains('b-cart_disabled')).toBe(false);
    });

    it('Adds disable class on disable', () => {
        const model = new CartModel(()=>{});
        const view = new CartView(model, ()=>{});
        view.enable(true);
        view.enable(false);

        expect(view.element.classList.contains('b-cart_disabled')).toBe(true);
    });

    it('Reacts on click event', () => {
        const fn = jest.fn();
        const event = new MouseEvent('click');
        const model = new CartModel(()=>{});
        const view = new CartView(model, fn);

        view.element.dispatchEvent(event);
        expect(fn).toBeCalledWith(event);
    });
});

