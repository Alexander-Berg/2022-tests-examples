const getText = element => page.evaluate(el => el.textContent, element);

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

const open = async url => {
    await page.goto(url);
    await page.waitForSelector('#app div div');
    await sleep(2000);
};

const waitForResponse = async url => {
    const responseObject = page.waitForResponse(response => response.url().includes(url));

    await sleep(1000);
    return responseObject;
};

const reload = async () => {
    await page.evaluate(() => {
        // eslint-disable-next-line no-undef
        window.location.reload();
    });
    await page.waitForSelector('#app div div');
};

export { getText, sleep, open, reload, waitForResponse };
