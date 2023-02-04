/* globals window */

const fs = require('fs').promises;
const { join } = require('path');

const { ContentType } = require('allure-js-commons');

require('expect-puppeteer');

const { getBundleName, toMatchImageSnapshotFn } = require('@realty-front/jest-utils/puppeteer/tests-setup/utils');
const { encrypt, decrypt } = require('@realty-front/jest-utils/proxy/libs/encription');
const { RUN_TYPE, getBaseMockDirectory } = require('@realty-front/jest-utils/proxy/libs/utils');
const { waitForAllImagesLoaded } = require('@realty-front/jest-utils/puppeteer/tests-helpers/waitForAllImagesLoaded');

const { name: PROJECT_NAME } = require(join(process.cwd(), 'package.json'));

const isDebug = Boolean(process.env.IS_DEBUG) || Boolean(process.env.VNC_DEBUG);

jest.setTimeout(isDebug ? 10005000 : 180000);

if (process.env.CI) {
    // на CI добавляем ретраи для флапающих тестов
    jest.retryTimes(3);
}

const _goto = page.goto;
const { MOCK } = process.env;
const HOSTNAME = {
    'realty-www': 'https://realty-frontend.realty.local.dev.vertis.yandex.ru',
    'realty-mobile-www': 'https://realty-frontend.m.realty.local.dev.vertis.yandex.ru',
    'realty-lk-www': 'https://realty-frontend.realty.local.dev.vertis.yandex.ru',
    'realty-arenda': 'https://realty-frontend.arenda.local.dev.vertis.yandex.ru'
}[PROJECT_NAME];

page.on('error', async e => {
    // eslint-disable-next-line no-undef
    if (typeof __allure__ !== 'undefined' && __allure__) {
        const screenshot = await page.screenshot({ fullPage: true });

        // eslint-disable-next-line no-undef
        screenshot && __allure__.attachment(`Скриншот перед ошибкой: ${e.toString()}`, screenshot, ContentType.PNG);
    }
});

// Хак для загрузки нужного бандла с тестами
page.goto = async(url, pageConfig) => {
    const { testPath, currentTestName } = expect.getState();
    const bundleName = getBundleName(testPath);

    if (url.indexOf('?test=') !== -1) {
        url = `${url}&bundle=${bundleName}`;
    }

    // eslint-disable-next-line no-console
    console.log(currentTestName, url);

    const gotoResult = await _goto.call(page, url, pageConfig).catch(() => {});
    const hasTestsReadyFlag = await page.evaluate('window.testsReady === false', { timeout: 5000 });

    if (hasTestsReadyFlag) {
        await page.waitForFunction('window.testsReady', { timeout: 10000 });
    }

    return gotoResult;
};

customPage.scrollToTop = async() => {
    await page.evaluate(() => {
        window.scrollTo({ top: 0 });
    });

    await page.waitForFunction(() => window.scrollY === 0);
};

// нестабильный метод, не рекомендуется к использованию
// основной кейс использования - дожидаемся отрисовки карты без пинов
customPage.waitForYmaps = async() => {
    // дожидаемся, пока карта появится в доме
    await page.waitForSelector('ymaps');
    // после инициализации карты необходимо время для простановки пинов и отрисовки полигонов
    await page.waitFor(500);
};

// основной метод для ожидания отрисовки карты
customPage.waitForYmapsPins = async() => {
    // предполагаем, что отрисовались все пины
    await page.waitForSelector('ymaps [class^="MapPlacemark"]');
};

customPage.disableFrameAnimations = async frame => {
    // eslint-disable-next-line max-len
    await frame.addStyleTag({ content: '*,*:after,*:before{image-rendering:auto!important;transition-delay:0s!important;transition-duration:0s!important;animation-duration:0.0001s!important;animation-play-state:paused!important;animation-delay:-0.0001s!important;caret-color:transparent!important;}' });
};

customPage.getYKassaCardFormFrame = async() => {
    await page.waitForSelector('[name="YKassaCardForm"]');

    const frames = await page.frames();
    const frame = await frames.find(item => item.name() === 'YKassaCardForm');

    if (! frame) {
        throw new Error('Seems, like you are not using component \'YandexKassaCardForm\' or it not rendered');
    }

    await customPage.disableFrameAnimations(frame);

    await frame.waitForSelector('.yoomoney-checkout-cardpayment__form');

    return frame;
};

customPage.login = async({ user }) => {
    const { login, password } = user;
    const cookiePath = join(getBaseMockDirectory(), 'cookies.json.encrypted');

    await fs.mkdir(getBaseMockDirectory(), { recursive: true });

    await page.setViewport({ width: 1000, height: 800 });

    // await page.evaluateOnNewDocument(() => {
    //     // eslint-disable-next-line no-undef
    //     localStorage.clear();
    // });

    // Имитируем режим инкогнито - чистим всё
    const client = await page.target().createCDPSession();

    await client.send('Network.clearBrowserCookies');
    await client.send('Network.clearBrowserCache');

    // не тестируем эксперименты - отключаем все, чтоб не мешали
    await client.send('Network.setCookie', {
        url: HOSTNAME,
        name: 'autotest_uaas_ids',
        value: '-1'
    });

    if (MOCK !== RUN_TYPE.CREATE) {
        const cookiesRaw = await fs.readFile(cookiePath);
        const cookiesJSON = decrypt(cookiesRaw.toString());
        const savedCookies = JSON.parse(cookiesJSON);

        if (! savedCookies[login]) {
            throw new Error(`No cookies for user ${login}`);
        }

        await page.setCookie(...savedCookies[login]);
    } else {
        // Редиректим на страницу профиля, чтобы не делать лишних запросов в наше приложение при авторизации
        const retpath = 'https://passport-test.yandex.ru/profile';

        await page.setRequestInterception(true);

        let isIntercepted = true;

        page.once('request', async request => {
            // После обновления puppeteer перестал работать "once" и "off" на событие запроса О_о
            if (! isIntercepted) {
                return;
            }

            const data = {
                method: 'POST',
                postData: `retpath=${retpath}&login=${login}&password=${password}`,
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
                }
            };

            request.continue(data);

            isIntercepted = false;

            await page.setRequestInterception(false);
        });

        await page.goto('https://passport-test.yandex.ru/passport?mode=embeddedauth');

        const cookies = await page.cookies();

        let allCookies = {};

        try {
            const allCookiesRaw = await fs.readFile(cookiePath);

            allCookies = JSON.parse(decrypt(allCookiesRaw.toString()));
        } catch (e) {}

        allCookies[login] = cookies;

        await fs.writeFile(cookiePath, encrypt(JSON.stringify(allCookies, null, 2)));
    }
};

const onLoadInterceptionHandler = request => {
    const url = new URL(request.url());

    // Сторонний скрипт падает на странице, что приводит к падению тестов
    if (url.pathname.includes('safeframe-bundles')) {
        return request.respond({
            status: 404,
            body: ''
        });
    }

    // На странице паспорта падает ошибка см. https://st.yandex-team.ru/REALTYFRONT-13249
    // Честно было бы игнорить ошибки на не наших страницах, но хз как это сделать.
    // Временное решение такое - нам нечего проверять на чужих страницах, поэтому контент не важен
    if (url.toString().includes('https://passport-test.yandex.ru')) {
        return request.respond({
            status: 200,
            body: 'Passport page =)'
        });
    }

    return request.continue();
};

customPage.path = async path => {
    await page.setRequestInterception(true);

    page.on('request', onLoadInterceptionHandler);

    const response = await page.goto(HOSTNAME + path, {
        waitUntil: 'networkidle2',
        timeout: 30000
    });

    page.off('request', onLoadInterceptionHandler);

    await page.setRequestInterception(false);

    return response;
};

expect.extend({
    toMatchImageSnapshot: toMatchImageSnapshotFn
});

customPage.waitForAllImagesLoaded = () => page.evaluate(waitForAllImagesLoaded);

// Увеличивает системное время на заданный промежуток,
// запускает все таймеры которые должны были выполниться за это время
customPage.tick = timeParam => {
    return page.evaluate(time => {
        if (! window.__clock) {
            throw new Error('FakeTimers are not enabled in AppProvider');
        }

        window.__clock && window.__clock.tick(time);
    }, timeParam);
};
