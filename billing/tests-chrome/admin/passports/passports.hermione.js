const assert = require('chai').assert;
const { Roles, Perms } = require('../../../helpers/role_perm');

const elements = {
    passport: '.yb-passports-section__passports',
    passportSubmit: '.yb-passports-section__passports [type=submit]',
    passportLinkLink:
        '.yb-passports-section__passports table tbody tr:first-child .yb-passports-section__column-link a',
    passportUidLink:
        '.yb-passports-section__passports table tbody tr:first-child .yb-passports-section__column-uid a',
    passportEmailLink:
        '.yb-passports-section__passports table tbody tr:first-child .yb-passports-section__column-email a',
    reps: '.yb-passports-section__representatives',
    repsUnlinkLink:
        '.yb-passports-section__representatives tbody tr:first-child .yb-passports-section__column-unlink a',
    repsUidLink:
        '.yb-passports-section__representatives tbody tr:first-child .yb-passports-section__column-uid a',
    repsEmailLink:
        '.yb-passports-section__representatives tbody tr:first-child .yb-passports-section__column-email a',
    accountReps: '.yb-passports-section__accountant-representatives',
    accRepsUidLink:
        '.yb-passports-section__accountant-representatives tbody tr:first-child .yb-passports-section__column-uid a',
    accountEmailLink:
        '.yb-passports-section__accountant-representatives tbody tr:first-child .yb-passports-section__column-email a',
    serviceData: '.yb-passports-section__service-data',
    empty: '.yb-passports-section__empty',
    uid: 'input[name=uid]',
    login: 'input[name=login]'
};

const assertViewOpts = {
    hideElements: ['.yb-passports-section__column-is-main']
};

describe('admin', () => {
    describe('passports', () => {
        it('поиск, ссылки UID в таблице поиска представителей и привязанных представителей, удаление и добавление', async function () {
            const { browser } = this;

            const idToLink = '3000284089';
            const loginToLink = 'atkaya';

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            await browser.ybSetSteps(
                `Отвязываем всех клиентов для логина ${loginToLink} (${idToLink})`
            );
            await browser.ybRun('unlink_all_clients_from_login', [idToLink]);

            const { client_id } = await browser.ybRun('create_client');

            await browser.ybUrl('admin', `passports.xml?tcl_id=${client_id}`);

            await browser.ybReplaceValue(elements.login, loginToLink);
            await browser.click(elements.passportSubmit);
            await browser.ybWaitForInvisible('.yb-search-filter__button-search_progress');
            await browser.ybAssertView(
                'найти представителей клиента - поиск',
                elements.passport,
                assertViewOpts
            );

            const passportUidHref = await browser.getAttribute(elements.passportUidLink, 'href');
            await browser.ybSetSteps(
                `Проверяет ссылку UID в таблице поиска представителей. Должна содержать "/choose-role.xml?passport-id=${idToLink}"`
            );
            assert(
                passportUidHref.indexOf(`/choose-role.xml?passport-id=${idToLink}`) >= 0,
                `некорректная ссылку UID в таблице поиска представителей: "${passportUidHref}", должна содержать "/choose-role.xml?passport-id=${idToLink}"`
            );

            const passportEmailHref = await browser.getAttribute(
                elements.passportEmailLink,
                'href'
            );
            const email = `mailto:${loginToLink}@yandex.ru`;
            await browser.ybSetSteps(
                `Проверяет ссылку Email в таблице поиска представителей. Должна быть равна "${email}`
            );
            assert(
                passportEmailHref === email,
                `некорректная ссылку Email в таблице поиска представителей: "${passportEmailHref}", должна быть равна "${email}"`
            );

            await browser.ybSetSteps(`Нажимаем на ссылку Добавить`);
            await browser.click(elements.passportLinkLink);
            await browser.waitForVisible(elements.repsUidLink);

            await browser.ybSetSteps('Проверяет, что запись появилась в таблице Представители');
            await browser.waitForVisible(`${elements.reps} table`);
            await browser.ybSetSteps('Нажимает на ссылку Удалить');
            await browser.click(elements.repsUnlinkLink);
            await browser.ybSetSteps('Проверят, что запись исчезла из таблицы Представители');
            await browser.waitForVisible(`${elements.reps} ${elements.empty}`);
        });

        it('пустые списки', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });
            const { client_id } = await browser.ybRun('create_client');

            await browser.ybUrl('admin', `passports.xml?tcl_id=${client_id}`);

            await browser.waitForVisible(`${elements.reps} ${elements.empty}`);
            await browser.ybAssertView('нет представителей', elements.reps, assertViewOpts);

            await browser.waitForVisible(`${elements.accountReps} ${elements.empty}`);
            await browser.ybAssertView(
                'нет бухгалтерских логинов',
                elements.accountReps,
                assertViewOpts
            );

            await browser.waitForVisible(`${elements.serviceData} ${elements.empty}`);
            await browser.ybAssertView(
                'нет мультивалютности',
                elements.serviceData,
                assertViewOpts
            );
        });

        it('представители клиента и бухгалтерские логины, ссылки uid, email', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            const clientId = 2388459;
            const repsUid = 346949883;
            const accRepsUid = 105532920;
            await browser.ybUrl('admin', `passports.xml?tcl_id=${clientId}`);

            await browser.waitForVisible(`${elements.reps} table`);
            await browser.ybAssertView('представители клиента', elements.reps, assertViewOpts);

            const repsUidHref = await browser.getAttribute(elements.repsUidLink, 'href');
            await browser.ybSetSteps(
                `Проверяет ссылку UID в таблице представителей. Должна содержать "/choose-role.xml?passport-id=${repsUid}"`
            );
            assert(
                repsUidHref.indexOf(`/choose-role.xml?passport-id=${repsUid}`) >= 0,
                `некорректная ссылка UID в таблице представителей: "${repsUidHref}", должна содержать "/choose-role.xml?passport-id=${repsUid}"`
            );

            const repsEmailHref = await browser.getAttribute(elements.repsEmailLink, 'href');
            await browser.ybSetSteps(
                `Проверяет ссылку Email в таблице представителей. Должна содержать "mailto:"`
            );
            assert(
                repsEmailHref.indexOf('mailto:') >= 0,
                `некорректная ссылка Email в таблице представителей: "${repsEmailHref}", должна содержать "mailto:"`
            );

            await browser.waitForVisible(`${elements.accountReps} table`);
            await browser.ybAssertView(
                'бухгалтерские логины',
                elements.accountReps,
                assertViewOpts
            );

            const accRepsUidHref = await browser.getAttribute(elements.accRepsUidLink, 'href');
            await browser.ybSetSteps(
                `Проверяет ссылку UID в таблице бух. логинов. Должна содержать "/choose-role.xml?passport-id=${accRepsUid}"`
            );
            assert(
                accRepsUidHref.indexOf(`/choose-role.xml?passport-id=${accRepsUid}`) >= 0,
                `некорректная ссылка UID в таблице бух. логинов: "${accRepsUidHref}", должна содержать "/choose-role.xml?passport-id=${accRepsUid}"`
            );

            const accRepsEmailHref = await browser.getAttribute(elements.accountEmailLink, 'href');
            await browser.ybSetSteps(
                `Проверяет ссылку Email в таблице бух. логинов. Должна содержать "mailto:"`
            );
            assert(
                accRepsEmailHref.indexOf('mailto:') >= 0,
                `некорректная ссылка Email в таблице бух. логинов: "${accRepsEmailHref}", должна содержать "mailto:"`
            );
        });

        it('мультивалютность', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });

            await browser.ybUrl('admin', `passports.xml?tcl_id=69965370`);
            await browser.waitForVisible(`${elements.serviceData} table`);
            await browser.ybAssertView('мультивалютность', elements.serviceData, assertViewOpts);
        });

        it('нет права BillingSupport', async function () {
            const { browser } = this;

            const { id: uid, login } = await browser.ybSignIn({
                baseRole: Roles.BackOffice,
                include: [Perms.NewUIEarlyAdopter],
                exclude: []
            });

            const { client_id } = await browser.ybRun('create_client_for_user', [login]);

            await browser.ybUrl('admin', `passports.xml?tcl_id=${client_id}`);

            await browser.waitForVisible(elements.reps);

            await browser.ybSetSteps(`Проверяет, что блока поиска нет на странице`);
            await browser.ybWaitForInvisible(
                '.yb-page-section__title*=Найти представителей клиента'
            );
            await browser.waitForVisible(`${elements.reps} table`);
            await browser.ybSetSteps(
                'Проверяет отсутствие колонки Удалить в шапке таблицы Представители'
            );
            await browser.ybAssertView('представители - нет колонки Удалить', elements.reps, {
                ...assertViewOpts,
                hideElements: [...assertViewOpts.hideElements, `${elements.reps} tbody`]
            });
            await browser.ybRun('unlink_all_clients_from_login', [uid]);
        });
        it('Переход по ссылкам на странице клиента', async function () {
            const { browser } = this;

            await browser.ybSignIn({
                isAdmin: true,
                isReadonly: false
            });
            await browser.ybUrl('admin', 'passports.xml?tcl_id=393872');
            await browser.ybWaitForLoad();
            await browser.ybAssertLink(
                '.src-common-components-Client-___style-module__client-header-item a',
                'editclient.xml?tcl_id=393872'
            );
            await browser.ybAssertLink(
                '.yb-client-info__deferpays',
                'deferpays.xml?client_id=393872'
            );
            await browser.ybAssertLink('.yb-client-info__orders', 'orders.xml?client_id=393872');
            await browser.ybAssertLink(
                '.yb-client-info__invoices',
                'invoices.xml?client_id=393872'
            );
            await browser.ybAssertLink('.yb-client-info__acts', 'acts.xml?client_id=393872');
            await browser.ybAssertLink(
                '.yb-client-info__contracts',
                'contracts.xml?agency_id=393872'
            );
            await browser.ybAssertLink(
                '.yb-client-info__partner-contracts',
                'partner-contracts.xml?client_id=393872'
            );
            await browser.ybAssertLink('.yb-client-info__crm', 'client.aspx?id=393872');
            await browser.ybAssertLink(
                '.yb-client-info__addtclient',
                'addtclient.xml?agency_id=393872'
            );
        });
    });
});
