'use strict';

const assert = require('assert');
const TEN_SECS = 10000;

describe('Карточка сертификата', () => {

    describe('Положительные', () => {

        it('1. Перезапрос сертификата из карточки', function () {
            // открыть CRT https://crt.test.yandex-team.ru/
            return this.browser.openIntranetPage({pathname: '/'})

                // кликнуть по первой строке таблицы
                .waitForExist('.certs-table__tr:last-child', TEN_SECS)
                .click('.certs-table__tr')

                // открылась карточка сертификата, в которой присутствуют кнопки действий над сертификатом
                .waitForExist('.cert-card', TEN_SECS)
                .waitForExist('.cert-card__actions', TEN_SECS)
                .assertView('cert-card__actions', '.cert-card__actions')

                // нажать кнопку Перезапросить
                .click('.cert-card__button_type_reissue')

                // открылась предзаполненная форма запроса сертификата
                .waitForExist('.cert-request-form__modal-content', TEN_SECS)
                .pause(100)
                .changeModalBackground()
                .assertView('cert-request-form_is_filled', '.cert-request-form__modal-content')
                .restoreModalBackground()

                // нажать кнопку "Запросить"
                .click('.cert-request-form__button_type_issue')

                // форма успешно отправилась
                .waitForExist('.cert-request-form__modal-content_submitted', TEN_SECS)
                .changeModalBackground()
                .pause(100)
                .assertView('cert-request-form_is_sent', '.cert-request-form__modal-content')
                .restoreModalBackground()

                // нажать кнопку "Закрыть"
                .click('.cert-request-form__button_type_close')

                // форма закрылась
                .elements('.cert-request-form__modal-content')
                .then(({value}) => {
                    assert.strictEqual(value.length, 0);
                });
        });

    });

});
