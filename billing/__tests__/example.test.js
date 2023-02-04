const puppeteer = require('puppeteer');
const speedDelay = 1000;

describe('Google', () => {

  it('should display 2', async () => {
    'use strict';

    jest.setTimeout(180000);

    const { spawn } = require('child_process');
    // const script = spawn('python', ['C:\\torvald\\_TEST_TOOLS\\balance-tests\\balance\\real_builders\\invoices2.py']);
    // const script = spawn('sh', ['C:\\torvald\\_TEST_TOOLS\\billing_puppeteer\\scrip.sh']);
    const script = spawn('C:\\torvald\\_TEST_TOOLS\\venvs\\Common\\Scripts\\python.exe', ['C:\\torvald\\_TEST_TOOLS\\balance-tests\\balance\\real_builders\\invoices2.py']);
    // const script = spawn('C:\\torvald\\_TEST_TOOLS\\venvs\\Common\\Scripts\\python.exe', ['-m', 'balance.real_builders.invoices4']);
    // const script = spawn('echo', ['12321312']);

    const chunks = [];

    // зачем комментирии на английском?
    // there is a data chunk from the script available to read
    script.stdout.on('data', (data) => {
      chunks.push(data.toString());
    });

    let invoiceId = null;

    await new Promise((res, rej) => {
      script.once('exit', () => {
        // берем последнюю строку
        invoiceId = chunks[chunks.length - 1];
        res();
      });
    });

    console.log(`Получили invoice_id = ${invoiceId}`);

    // ----------------------------------------------------------------------------

    const browser = await puppeteer.launch({headless: false});
    const page = await browser.newPage();
    await page.setViewport({ width: 1366, height: 768});
    await page.goto('https://www.yandex.ru');

    await page.click('a[role="button"]');

    await page.waitForSelector('#passp-field-login');
    // await page.type('#passp-field-login', 'yb-adm', {delay: speedDelay});
    await page.click('#passp-field-login');
    await page.keyboard.press('y');
    await page.keyboard.press('b');
    await page.keyboard.press('-');
    await page.keyboard.press('a');
    await page.keyboard.press('d');
    await page.keyboard.press('m');
    await page.click('button[type="submit"]');

    await page.waitForSelector('#passp-field-passwd');
    // await page.type('#passp-field-login', 'Qwerty123!', {delay: speedDelay});
    await page.click('#passp-field-passwd');
    await page.keyboard.press('Q');
    await page.keyboard.press('w');
    await page.keyboard.press('e');
    await page.keyboard.press('r');
    await page.keyboard.press('t');
    await page.keyboard.press('y');
    await page.keyboard.press('1');
    await page.keyboard.press('2');
    await page.keyboard.press('3');
    await page.keyboard.press('!');
    await page.click('button[type="submit"]');

    await page.waitForSelector('#text');

    await page.goto('https://admin-balance.greed-tm.paysys.yandex.ru/invoice.xml?invoice_id='.concat(invoiceId));

    await page.waitForSelector('#confirm-payment__btn', {timeout: 120 * 1000});

    page.on('dialog', async dialog => {
      await dialog.accept()
    })

    await page.click('#confirm-payment__btn');

    await page.waitForSelector('#invoice-loading-container>div')

    // await Promise.all([
    //   page.click('#confirm-payment__btn'),
    //   page.waitUntil('domcontentloaded'),
    //   page.waitForNavigation({ waitUntil: 'domcontentloaded' }),
    // ]);

    // await page.click('#confirm-payment__btn');

    await page.waitForSelector('#consumes-container>div')

    await page.screenshot({path: 'example101.png'});
    //
    // await browser.close();

    console.log('THE END');

  })
});