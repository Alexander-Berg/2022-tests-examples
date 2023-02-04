import {expect} from 'chai';
import {getHost, Tld} from '../../lib/func/url';

const CASES: {tld: Tld; lang: string}[] = [
    {
        tld: 'ru',
        lang: 'ru'
    },
    {
        tld: 'tr',
        lang: 'tr'
    },
    {
        tld: 'com',
        lang: 'en'
    }
];

describe('prefetch.txt', () => {
    CASES.forEach(({tld, lang}) => {
        describe(`для домена ${tld}`, () => {
            beforeEach(async function () {
                const url = getHost({tld}) + '/prefetch.txt';
                await this.browser.url(url);
            });

            it('возвращает нужные ссылки', async function () {
                await this.browser.perform(async () => {
                    const body = await this.browser.getText('body');

                    const links = body.split('\n');
                    expect(links).to.have.lengthOf(5);
                    expect(links[links.length - 1]).to.be.match(new RegExp(`.${lang}.js$`));
                }, 'Проверить, что prefetch.txt содержит 5 ссылок корректного формата.');
            });
        });
    });
});
