'use strict';

const assert = require('assert');
const TEN_SECS = 10000;

describe('Фильтры', () => {

    describe('Положительные', () => {

        afterEach(async function () {
            await this.browser.deleteCookie(['just_updated']);
        });

        it('1. Работа фильтров после запроса сертификата', function () {
            // открыть CRT https://crt.test.yandex-team.ru/
            return this.browser.openIntranetPage({pathname: '/'})
                .disableAnimations('*')

                // без фильтров в таблице есть сертификаты, таблица не пуста
                .waitForExist('.certs-table__tr[data-id="2665293"]', TEN_SECS)
                .elements('.certs-table__tr[data-id="2665293"]')
                .then(({value}) => {
                    assert.strictEqual(value.length, 1);
                })

                // в поле фильтра Common name вводим «123»
                .waitForExist('.filters__filter_type_textinput input', TEN_SECS)
                .setValue('.filters__filter_type_textinput input', '123')

                // при заданных фильтрах таблица пуста [certs-table-no-data]
                .waitForExist('.certs-table__td_type_no-data', TEN_SECS)
                .assertView('certs-table-no-data', '.certs-table')

                // заходим в форму запроса сертификата
                .waitForExist('.cert-request-form .button2', TEN_SECS)
                .click('.cert-request-form .button2')

                // выбираем CA name "InternalTestCA"
                .setSuggestVal('.cert-request-form__control-suggest_slug_ca-name', 'InternalTestCA')

                // заполняем параметр Хосты "test.ru"
                .setValue('.cert-request-form__control-textinput_slug_hosts input', 'test.ru')

                // выбираем ABC-сервис "Сертификатор"
                .setValue('.cert-request-form__control-suggest_slug_abc-service input', 'cert')
                .setSuggestVal('.cert-request-form__control-suggest_slug_abc-service', 'Сертификатор')

                // нажать на кнопку “Заказать”
                .click('.cert-request-form__button_type_issue')

                // нажать на кнопку “Закрыть”
                .waitForExist('.cert-request-form__modal-content_submitted', TEN_SECS)
                .click('.cert-request-form__button_type_close')

                // при заданных фильтрах таблица пуста [certs-table-no-data]
                .waitForExist('.certs-table__td_type_no-data', TEN_SECS)
                .assertView('certs-table-no-data', '.certs-table');
        });
    });
});
