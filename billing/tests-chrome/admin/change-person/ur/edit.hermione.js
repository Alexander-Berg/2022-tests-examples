const assert = require('chai').assert;

const { setInnValue, setSuggestValue, setValue } = require('../helpers');
const { assertViewOpts } = require('../config');
const { data } = require('./data');
const { elements } = require('../elements');
const { Roles, Perms } = require('../../../../helpers/role_perm');

describe('admin', () => {
    describe('change-person', () => {
        describe('ur_0', () => {
            describe('редактирование', () => {
                it('создание плательщика, редактирование плательщика', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client_for_user', [login]);

                    // Открываем список плательщиков клиента и нажимаем на добавление Юр. лица
                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);

                    await browser.ybSetSteps(`Выбирает добавление Юр. лицо или ПБОЮЛ
Переходит на  change-person.xml`);

                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu.ur);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.waitForVisible(elements.formChangePerson);
                    await browser.ybAssertView(
                        'edit_ur_empty-form',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(`Вводит название и выбирает найденный вариант.
Вводит ИНН и выбирает найденный вариант.
Заполняет все оставшиеся поля`);

                    await browser.setValue(elements.name, 'рогачев');
                    await browser.waitForVisible(`.Suggest-Item*=РОГАЧЕВЪ`);
                    await browser.click(`.Suggest-Item*=РОГАЧЕВЪ`);
                    await browser.ybAssertView(
                        'edit_ur_check-autofill-by-name',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                    await browser.ybReplaceValue(elements.inn, data.add.ur.inn);
                    await browser.waitForVisible(`.Suggest-Item*=${data.add.ur.name}`);
                    await browser.click(`.Suggest-Item*=${data.add.ur.name}`);
                    await browser.setValue(elements.phone, data.add.ur.phone);
                    await browser.setValue(elements.fax, data.add.ur.fax);

                    // Выпадашка не открывается с первого раза. Баг не воспроизводится в chromium, ff и yabro
                    await browser.click(elements.countryId);
                    await browser.click(elements.countryId);
                    await browser.click(elements.countryId);
                    await browser.click(`.Menu-Text=Бельгия`);
                    await browser.click(elements.deliveryType);
                    await browser.click(`.Menu-Text=${data.add.ur.deliveryType}`);
                    await browser.setValue(elements.representative, data.add.ur.representative);
                    await browser.click(elements.reviseActPeriodType);
                    await browser.click(`.Menu-Text=${data.add.ur.reviseActPeriodType}`);
                    await setValue(browser, data.add.ur.city);
                    await browser.setValue(elements.postcodeSimple, data.add.ur.postcodeSimple);
                    await browser.setValue(elements.postbox, data.add.ur.postbox);
                    await browser.click(elements.invalidBankprops);
                    await browser.setValue(elements.bik, data.add.ur.bik);
                    await browser.setValue(elements.account, data.add.ur.account);
                    await browser.setValue(elements.address, data.add.ur.address);
                    await browser.click(elements.deliveryType);
                    await browser.click(`.Menu-Text=${data.add.ur.deliveryType}`);
                    await browser.click(elements.deliveryCity);
                    await browser.click(`.Menu-Text=${data.add.ur.deliveryCity}`);
                    await browser.click(elements.liveSignature);
                    await browser.setValue(elements.signerPersonName, data.add.ur.signerPersonName);
                    await browser.click(elements.signerPersonGender);
                    await browser.click(`.Menu-Text=${data.add.ur.signerPersonGender}`);
                    await browser.click(elements.signerPositionName);
                    await browser.click(`.Menu-Text=${data.add.ur.signerPositionName}`);
                    await browser.ybReplaceValue(
                        'div[data-detail-id="signerPositionName"] input[name=signer-position-name]',
                        'Повелитель'
                    );
                    await browser.click(elements.authorityDocType);
                    await browser.click(`.Menu-Text=${data.add.ur.authorityDocType}`);
                    await browser.setValue(
                        elements.authorityDocDetails,
                        data.add.ur.authorityDocDetails
                    );
                    await browser.click(elements.vip);
                    await browser.setValue(elements.kbk, data.add.ur.kbk);
                    await browser.setValue(elements.oktmo, data.add.ur.oktmo);
                    await browser.setValue(elements.paymentPurpose, data.add.ur.paymentPurpose);
                    await browser.waitForVisible('.suggest__spin-container', 3000, true);
                    await browser.ybAssertView(
                        'edit_ur_filled-form',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(`Нажимает на кнопку сохранения.
Переходит на  subpersons.xml`);

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personDetails);
                    await browser.ybAssertView(
                        'edit_ur_added',
                        elements.personDetails,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(`Нажимает на ссылку редактирования.
Переходит на  change-person.xml.
Юр. адрес меняет на ввод вручную.
Почтовый адрес меняет на ввод по справочнику`);

                    await browser.click(elements.editLink);

                    await browser.waitForVisible(elements.formChangePerson);
                    // юр. адрес поменям на заданный вручную
                    await browser.click('input[name=legal-addr-type][value="2"]');
                    await browser.setValue(
                        'textarea[name=legaladdress]',
                        'Это юр. адрес, заданный вручную'
                    );
                    // почтовый адрес поменяем на заданный по справочнику
                    await browser.click('input[name="is-postbox"][value="0"]');
                    await setSuggestValue(browser, 'input[name=city]', 'химк', 'Химки');
                    await setSuggestValue(browser, 'input[name=street]', 'липовая', 'Липовая');
                    await browser.setValue('input[name=postcode]', '555555');
                    await browser.setValue('input[name=postsuffix]', '5 корп 50 стр 500');
                    await browser.waitForVisible('.suggest__spin-container', 3000, true);
                    await browser.ybAssertView(
                        'edit_ur_edited-form',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(`Нажимает на кнопку Зарегистрировать.
Переходит на  subpersons.xml.`);

                    await browser.click(elements.btnSubmit);

                    await browser.waitForVisible(elements.personDetails);
                    await browser.ybAssertView(
                        'edit_ur_edited',
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                // https://testpalm.yandex-team.ru/testcase/balanceassessors-28
                it('создание и редактирование, автозаполнение по ИНН', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const { client_id } = await browser.ybRun('create_client_for_user', [login]);

                    // Открываем список плательщиков клиента и нажимаем на добавление Юр. лица
                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);

                    await browser.ybSetSteps(`Выбирает добавление Юр. лицо
Переходит на  change-person.xml`);

                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu.ur);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.ybSetSteps(`В открывшемся окне в поле “ИНН” ввести значение “7736207543”.
Выбрать первую организацию из списка (ООО "ЯНДЕКС")`);

                    await browser.waitForVisible(elements.formChangePerson);
                    await setInnValue(browser, elements.inn, '7736207543', 'Москва');
                    await browser.ybAssertView(
                        'edit_ur_autofill-by-inn',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.ybSetSteps(`Заново ввести в поле "ИНН" значение “773502859451”, в выпадающем списке выбрать первую организацию (ИП Новиков Евгений Витальевич)
Отменить создание плательщика. В открывшемся окна в выпадающем списке выбрать тип “Юр. лицо или ПБОЮЛ” и нажать на кнопку “Создать”`);

                    await browser.setValue(elements.inn, ['\uE051a', 'Delete']);
                    await setInnValue(browser, elements.inn, '773502859451', 'Москва');
                    await browser.ybAssertView(
                        'edit_ur_autofill-by-inn-2',
                        elements.formChangePerson,
                        assertViewOpts
                    );

                    await browser.click(
                        '.src-common-modules-change-person-components-ChangePerson-___styles-module__buttons a'
                    );

                    await browser.waitForVisible(elements.select);
                    await browser.click(elements.select);
                    await browser.click(elements.menu.ur);
                    await browser.click(elements.btnSubmitAddPerson);

                    await browser.ybSetSteps(
                        `В открывшемся окне в поле “ИНН” ввести значение “773502859451”, из выпавшего списка выбрать первую организацию (ИП Новиков Евгений Витальевич)`
                    );

                    await browser.waitForVisible(elements.formChangePerson);
                    await setInnValue(browser, elements.inn, '773502859451', 'Москва');
                    await browser.ybAssertView(
                        'edit_ur_autofill-by-inn-3',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });

                it('редактирование - поиск по названию', async function () {
                    const { browser } = this;

                    const { login } = await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });
                    const {
                        client_id,
                        person_id
                    } = await browser.ybRun('create_client_with_person_for_user', [
                        login,
                        'ur',
                        '0'
                    ]);

                    await browser.ybUrl(
                        'admin',
                        `change-person.xml?person_id=${person_id}&client_id=${client_id}`
                    );

                    await browser.ybSetSteps(`Ищет по названию. Выбирает из списка.
Проверяет что ИНН не изменяется.`);

                    await browser.waitForVisible(elements.formChangePerson);
                    const prevInn = await browser.getValue(elements.inn);
                    await browser.ybReplaceValue(elements.name, 'рогачев');
                    await browser.waitForVisible(`.Suggest-Item*=РОГАЧЕВЪ`);
                    await browser.click(`.Suggest-Item*=РОГАЧЕВЪ`);
                    const nextInn = await browser.getValue(elements.inn);
                    assert.equal(nextInn, prevInn, 'ИНН изменился');
                });

                it('редактирование самозанятого', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({ isAdmin: true, isReadonly: false });
                    const [client_id] = await browser.ybRun('test_selfemployed_ur_person');

                    await browser.ybSetSteps('Создаем клиента с самозанятым плательщиком');
                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.personDetails);

                    await browser.ybSetSteps('Нажимаем на ссылку Редактировать');
                    await browser.click(elements.editLink);
                    await browser.waitForVisible(elements.formChangePerson);
                    await browser.ybAssertView(
                        'форма редактирования плательщика - самозанятый',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                    await browser.ybSetSteps('Меняем ФИО');
                    await browser.ybReplaceValue(
                        'div[data-detail-id=lname] input[type=text]',
                        'Игнатов'
                    );
                    await browser.ybReplaceValue(
                        'div[data-detail-id=fname] input[type=text]',
                        'Игнат'
                    );
                    await browser.ybReplaceValue(
                        'div[data-detail-id=mname] input[type=text]',
                        'Игнатович'
                    );

                    await setValue(browser, data.add.ur.city);

                    await browser.ybClearValue('div[data-detail-id=postcode] input[type=text]');
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(elements.personDetails);
                    await browser.ybAssertView(
                        'subpersons.xml - список плательщиков - отредактированный самозанятый',
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it('редактирование самозанятого, не заполняет отчество', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        isAdmin: true,
                        isReadonly: false
                    });

                    const { client_id } = await browser.ybRun(
                        'create_client_with_selfemployed_person'
                    );

                    await browser.ybSetSteps(
                        'Создаем клиента с самозанятым платеьщиком и переходим на subpersons.xml'
                    );
                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.editLink);

                    await browser.ybSetSteps('Нажимаем на ссылку Редактировать');
                    await browser.click(elements.editLink);
                    await browser.waitForVisible(elements.formChangePerson);

                    await browser.ybSetSteps('Меняем ФИ');
                    await browser.ybReplaceValue(elements.lname, 'Игнатов');
                    await browser.ybReplaceValue(elements.fname, 'Игнат');
                    await browser.ybClearValue(elements.mname);
                    await browser.ybClearValue('div[data-detail-id=postcode] input[type=text]');
                    await browser.ybSetSteps(
                        'Пробуем сохранить - ошибка из-за незаполненного отчества'
                    );
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(elements.error);
                    await browser.ybAssertView(
                        'форма редактирования плательщика - самозанятый - необходимо заполнить отчество',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                    await browser.click(elements.mnameCheckbox);
                    await browser.click(elements.btnSubmit);
                    await browser.waitForVisible(elements.personDetails);
                    await browser.ybAssertView(
                        'subpersons.xml - список плательщиков - отредактированный самозанятый без отчества',
                        elements.personDetails,
                        assertViewOpts
                    );
                });

                it('редактирование самозанятого, нет права PersonPostAddressEdit', async function () {
                    const { browser } = this;

                    await browser.ybSignIn({
                        baseRole: Roles.Support,
                        include: [],
                        exclude: [Perms.PersonPostAddressEdit]
                    });

                    const { client_id } = await browser.ybRun(
                        'create_client_with_selfemployed_person'
                    );

                    await browser.ybSetSteps(
                        'Создаем клиента с самозанятым платеьщиком и переходим на subpersons.xml'
                    );
                    await browser.ybUrl('admin', `subpersons.xml?tcl_id=${client_id}`);
                    await browser.waitForVisible(elements.editLink);

                    await browser.ybSetSteps('Нажимаем на ссылку Редактировать');
                    await browser.click(elements.editLink);
                    await browser.waitForVisible(elements.formChangePerson);

                    await browser.ybSetSteps('Меняем ФИО');
                    await browser.ybReplaceValue(elements.lname, 'Игнатов');
                    await browser.ybReplaceValue(elements.fname, 'Игнат');
                    await browser.ybClearValue(elements.mname);

                    await browser.ybAssertView(
                        'форма редактирования плательщика - самозанятый - нет права PersonPostAddressEdit',
                        elements.formChangePerson,
                        assertViewOpts
                    );
                });
            });
        });
    });
});
