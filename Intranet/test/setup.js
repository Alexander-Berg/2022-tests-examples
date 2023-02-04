import 'babel-polyfill';
import 'polyfill-library/polyfills/Node/prototype/contains/polyfill';
import 'polyfill-library/polyfills/Element/prototype/closest/polyfill';
import {configure} from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

configure({adapter: new Adapter()});

process.env.BEM_LANG = 'ru';
window.fetch = () => Promise.resolve({
    ok: true,
    json() {
        return Promise.resolve({
            results: []
        });
    }
});
