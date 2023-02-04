/* global page, window */

import { allure } from '@realty-front/jest-utils/puppeteer/tests-helpers/allure';

export const reportGateHistory = async() => {
    const gatesHistory = await page.evaluate(() => {
        const result = window.__GatesHistory;

        window.__GatesHistory = [];

        return result || [];
    });

    for (const item of gatesHistory) {
        const { type, path, response, params } = item;
        const title = type === 'pending' ? 'AJAX-запрос: старт' : 'AJAX-запрос: завершён';

        allure.step(title, () => {
            allure.parameter('Таков путь: ', path);
            params && allure.parameter('Параметры: ', JSON.stringify(params));
            allure.parameter('Статус: ', type);
            response && allure.parameter('Данные: ', JSON.stringify(response));
        });
    }
};

