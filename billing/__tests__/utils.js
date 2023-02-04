import {
    jsonToFormData,
    createElementFromTemplate,
    subscribeEvent,
    secToMsec
} from '../utils';


describe('Basic tests of utils file', () => {

    it('jsonToFormData generates form data by given JSON object', () => {
        // TODO: check this
        expect(jsonToFormData({
            a: 1, b: 2, c: [1, 2, 3], d: { e: 1, f: 2 }
        })).toEqual('a=1&b=2&c=1&c=2&c=3&d=%7B%22e%22%3A1%2C%22f%22%3A2%7D');
    });

    it('createElementFromTemplate creates right element', () => {
        const element = createElementFromTemplate('p', 'contents', 'class_el', 'id_el');

        expect(element).toBeInstanceOf(Element);
        expect(element.tagName).toBe('P');
        expect(element.innerHTML).toBe('contents');
        expect(element.className).toBe('class_el');
        expect(element.id).toBe('id_el');
    });

    it('subscribeEvent subscribes Element to event', (done) => {
        const element = document.createElement('p');
        const fn = () => {
            done();
            element.removeEventListener('click', fn);
        };
        subscribeEvent(element, 'click', fn);
        element.dispatchEvent(new MouseEvent('click'));
        
    });

    it('secToMsec returns correct value', () => {
        expect(secToMsec(9)).toBe(9000);
    });
});
