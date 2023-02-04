/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import { SECOND } from 'auto-core/lib/consts';

import { changeTabStatus } from './utils';

describe('функция changeTabStatus', () => {
    beforeEach(() => {
        jest.useFakeTimers();
        global.document.title = 'foo bar';
    });

    it('периодически будет менять заголовок страницы и фавиконку', () => {
        changeTabStatus(1);

        expect(getTabTitle()).toBe('Новое сообщение');
        expect(getFavicon().href).toBe('https://auto.ru/static/img/favicons_unread/favicon_unread_01.ico');

        jest.advanceTimersByTime(1.5 * SECOND);

        expect(getTabTitle()).toBe('foo bar');
        expect(getFavicon().href).toBe('https://auto.ru/static/favicon.ico');

        jest.advanceTimersByTime(1.5 * SECOND);

        expect(getTabTitle()).toBe('Новое сообщение');
        expect(getFavicon().href).toBe('https://auto.ru/static/img/favicons_unread/favicon_unread_01.ico');
    });

    it('повторный вызов сбросит предыдущий интервал', () => {
        changeTabStatus(1);

        jest.advanceTimersByTime(2 * SECOND);

        changeTabStatus(2);

        expect(getTabTitle()).toBe('Новые сообщения');
        expect(getFavicon().href).toBe('https://auto.ru/static/img/favicons_unread/favicon_unread_02.ico');

        jest.advanceTimersByTime(SECOND);

        expect(getTabTitle()).toBe('Новые сообщения');
        expect(getFavicon().href).toBe('https://auto.ru/static/img/favicons_unread/favicon_unread_02.ico');
    });

    it('правильно формирует урл, если непрочитанных чатов больше 10', () => {
        changeTabStatus(11);
        expect(getFavicon().href).toBe('https://auto.ru/static/img/favicons_unread/favicon_unread_10.ico');
    });

    it('если все чаты прочитаны, вернет все как было и не будет больше ничего делать', () => {
        changeTabStatus(1);

        jest.advanceTimersByTime(4 * SECOND);

        changeTabStatus(0);

        expect(getTabTitle()).toBe('foo bar');
        expect(getFavicon().href).toBe('https://auto.ru/static/favicon.ico');

        jest.advanceTimersByTime(1.5 * SECOND);

        expect(getTabTitle()).toBe('foo bar');
        expect(getFavicon().href).toBe('https://auto.ru/static/favicon.ico');
    });
});

function getTabTitle() {
    return global.document.title;
}

function getFavicon() {
    return global.document.querySelector<HTMLLinkElement>('link[rel*=\'shortcut icon\']')!;
}
