import {get} from 'lodash';

async function setRequestListener(
    url: string,
    browser: WebdriverIO.Browser,
    expectedData: Record<string, string>,
    actualData: Record<string, string>
) {
    await browser.setRequestListener((request) => {
        if (request.url().includes(url)) {
            const postData = JSON.parse(request.postData()!);
            Object.keys(expectedData).forEach((key) => {
                const value = get(postData, key);
                actualData[key] = Array.isArray(value) ? value.join(',') : value;
            });
        }
        return request.continue();
    });
}

export default setRequestListener;
