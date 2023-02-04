/* tslint:disable */

import {configure} from 'enzyme';
import * as Adapter from '@wojtekmaj/enzyme-adapter-react-17';
import * as JestFetchMock from 'jest-fetch-mock';
import * as ReactDOM from 'react-dom';
import * as i18n from './lib/i18n-mock';

configure({
    adapter: new Adapter()
});

window.ymaps = {
    ready: jest.fn(() => {})
};

window.fetch = JestFetchMock;
// Мокает глобальную i18n, поскольку в тестах не происходит замены i18n() на перевод и требуется функция
// (i18n меняется только вебпаком при сборке).
window.i18n = i18n;

if (!Promise.prototype.finally) {
    Promise.prototype.finally = function (fn: any): Promise<any> {
        return this.then(
            (res) => Promise.resolve(fn()).then(() => res),
            (e) => Promise.resolve(fn()).then(() => {throw e})
        );
    }
}

window.IntersectionObserver = class IntersectionObserver {
    observe(): void {
        // Pass
    }

    disconnect(): void {
        // Pass
    }
};


// Портал падает в снепшотах. Надо, чтобы для тех мест, где в качестве элемента используется body,
// рендерилось так, как будто портала нет.
const createPortal = 'createPortal';
const originalPortal = ReactDOM.createPortal;
ReactDOM[createPortal] = function (): any {
    // Переопределяем только случаи с body, т.к. иначе можно поломать enzyme.
    if (arguments[1] === document.body) {
        return arguments[0];
    }

    return originalPortal.apply(null, arguments);
};
