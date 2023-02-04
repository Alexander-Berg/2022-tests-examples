'use strict';

const assert = require('assert');
const TEN_SECS = 10000;

describe('Запрос сертификата', () => {

    describe('Положительные', () => {
        it('1. Запрос сертификата', function () {
            // открыть CRT https://crt.test.yandex-team.ru/
            return this.browser.openIntranetPage({pathname: '/'})
                .disableAnimations('*')

                // нажать кнопку "Заказать сертификат" под шапкой сайта
                .waitForExist('.cert-request-form .button2', TEN_SECS)
                .click('.cert-request-form .button2')

                // открылась форма запроса сертификата
                .waitForExist('.cert-request-form__modal-content', TEN_SECS)
                .pause(100)
                .changeModalBackground()
                .assertView('cert-request-form_is_opened', '.cert-request-form__modal-content')
                .restoreModalBackground()

                // выбираем CA name "InternalTestCA"
                .setSuggestVal('.cert-request-form__control-suggest_slug_ca-name', 'InternalTestCA')

                // заполняем параметр Хосты "test.ru"
                .setValue('.cert-request-form__control-textinput_slug_hosts input', 'test.ru')

                // выбираем ABC-сервис "Сертификатор"
                .setValue('.cert-request-form__control-suggest_slug_abc-service input', 'cert')
                .setSuggestVal('.cert-request-form__control-suggest_slug_abc-service', 'Сертификатор')

                // нажать на кнопку “Заказать”
                .click('.cert-request-form__button_type_issue')

                // форма успешно отправилась
                .waitForExist('.cert-request-form__modal-content_submitted', TEN_SECS)
                .pause(100)
                .changeModalBackground()
                .assertView('cert-request-form_is_sent', '.cert-request-form__modal-content')
                .restoreModalBackground()

                // нажать на кнопку “Закрыть”
                .click('.cert-request-form__button_type_close')

                // форма закрылась
                .elements('.cert-request-form__modal-content')
                .then(({value}) => {
                    assert.strictEqual(value.length, 0);
                });
        });
    });
});
