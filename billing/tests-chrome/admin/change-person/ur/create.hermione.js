const { assertViewOpts } = require('../config');
const { data } = require('./data');
const { elements } = require('../elements');
const { setValue } = require('../helpers');

describe('admin', () => {
    describe('change-person', () => {
        describe('ur_0', () => {
            describe('создание', () => {
                it('заполняет только обязательные поля [smoke]', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client_for_user', [login]);
                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);

                    await browser.ybSetSteps(`Ищет по ИНН. Выбирает из списка.
Стирает все авто-заполненные необязательные поля. В юр. адресе стираем улицу.
Заполняет все оставшиеся обязательные поля.
Нажимает на Зарегистрировать.
Переходит на subpersons.xml.`);

                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu.ur);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);
                    await browser.setValue(elements.name, 'рогачев');
                    await browser.waitForVisible(`.Suggest-Item*=РОГАЧЕВЪ`);
                    await browser.click(`.Suggest-Item*=РОГАЧЕВЪ`);
                    await browser.ybClearValue(elements.ogrn);
                    await browser.setValue(elements.phone, data.add.ur.phone);
                    await browser.ybClearValue(elements.legalAddressStreet);
                    await browser.setValue(elements.legalAddressPostcode, '123456');
                    await setValue(browser, data.add.ur.city);
                    await browser.setValue(elements.postcodeSimple, data.add.ur.postcodeSimple);
                    await browser.setValue(elements.postbox, data.add.ur.postbox);
                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personDetails);
                    await browser.ybAssertView(
                        'create_ur_added-by-required-fields',
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it('ИНН и названия нет в справочнике', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client_for_user', [login]);
                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);

                    await browser.ybSetSteps(`Вводит ИНН и название по которым ничего не найдено.
Вводит все обязательные поля, юр. адрес текстом.
Нажимает на Зарегистрировать.
Переходит на subpersons.xml.`);

                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu.ur);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);
                    await browser.setValue(elements.inn, '3266162051');
                    await browser.setValue(elements.name, 'какая-то контора');
                    await browser.setValue(elements.longname, 'какая-то полная контора');
                    await browser.setValue(elements.kpp, '831344155');
                    await browser.setValue(elements.phone, data.add.ur.phone);
                    await browser.setValue(elements.legaladdress, data.add.ur.legaladdress);
                    await setValue(browser, data.add.ur.city);
                    await browser.setValue(elements.postcodeSimple, data.add.ur.postcodeSimple);
                    await browser.setValue(elements.postbox, data.add.ur.postbox);
                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personDetails);
                    await browser.ybAssertView(
                        'create_ur_added-by-unknown-inn-and-name',
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it('заполняет обязательные поля, показывает все города в выпадающем списке поля Город', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client_for_user', [login]);
                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);

                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu.ur);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.ybSetSteps('Заполняет обязательные поля');
                    await browser.waitForVisible(elements.formChangePerson);
                    await browser.setValue(elements.name, 'рогачев');
                    await browser.waitForVisible(`.Suggest-Item*=РОГАЧЕВЪ`);
                    await browser.click(`.Suggest-Item*=РОГАЧЕВЪ`);
                    await browser.ybClearValue(elements.ogrn);
                    await browser.setValue(elements.phone, data.add.ur.phone);
                    await browser.ybClearValue(elements.legalAddressStreet);
                    await browser.setValue(elements.legalAddressPostcode, '123456');
                    await browser.ybSetSteps('Почтовый адрес меняет на заданный по справочнику');
                    await browser.click('input[name=is-postbox][value="0"]');
                    await browser.ybSetSteps('Вводит в поле city строку "октябрьский"');
                    await browser.setValue('input[name=city]', 'октябрьский');
                    await browser.ybSetSteps('Выбирает "Показать все"');
                    await browser.waitForVisible('.Suggest-Item*=Показать');
                    await browser.click('.Suggest-Item*=Показать');
                    await browser.ybSetSteps('Выбирает вариант "Пичаевский"');
                    await browser.waitForVisible('.Suggest-Item*=Пичаевский');
                    await browser.click('.Suggest-Item*=Пичаевский');
                    await browser.setValue('input[name=postcode]', '555555');
                    await browser.setValue('input[name=postsuffix]', '5 корп 50 стр 500');

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personDetails);
                    await browser.ybAssertView(
                        'subpersons.xml - город выбрали через Показать все',
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it('создание плательщика ИП', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client_for_user', [login]);

                    // Открываем список плательщиков клиента и нажимаем на добавление Нерезидента
                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);

                    await browser.ybSetSteps(`Выбирает добавление Юр. лицо или ПБОЮЛ
Переходит на change-person.xml`);

                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu.ur);
                    await browser.click(elements.btnSubmitAddPerson);
                    await browser.waitForVisible(elements.formChangePerson);
                    await browser.ybAssertView(
                        'create_ur-ip_empty-form',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(`Вводит ИНН, соответствующее ИП.
Заполняет все оставшиеся поля`);

                    await browser.setValue(elements.inn, '503612526896');
                    const item =
                        '.yb-search-item__name[data-text="ИП Кукушкин Владимир Валерьевич"]';
                    await browser.waitForVisible(item);
                    await browser.click(item);
                    await browser.setValue(elements.phone, data.add.ur.phone);
                    await browser.setValue('[name=legal-address-home]', '-');
                    await setValue(browser, data.add.ur.city);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.setValue(elements.postcodeSimple, data.add.ur.postcodeSimple);
                    await browser.setValue(elements.postbox, data.add.ur.postbox);
                    await browser.ybWaitForInvisible(elements.suggestSpin);
                    await browser.ybAssertView(
                        'create_ur-ip_filled-form',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(`Нажимает на кнопку сохранения.
Переходит на subpersons.xml.`);
                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personDetails);
                    await browser.ybAssertView(
                        'create_ur-ip_added',
                        elements.personDetails,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
