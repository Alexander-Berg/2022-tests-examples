import {expect} from 'chai';

async function verifyLdJson<T extends object>(this: WebdriverIO.Browser, type: string, value: T): Promise<void> {
    const scripts = await this.$$('script[type="application/ld+json"]');
    const scriptsArray = await Promise.all(scripts.map((script) => script.getHTML()));
    const jsonsArray = scriptsArray.map((html) => JSON.parse(html.replace(/<\/?script.*?>/g, '')));
    const markupJson = jsonsArray.find((json) =>
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        '@graph' in json ? json['@graph'].some((item: any) => item['@type'] === type) : json['@type'] === type
    );
    if (!markupJson) {
        throw new Error(`ld+json markup of type ${type} not found!\nActual:\n${JSON.stringify(jsonsArray, null, 4)}`);
    }
    expect(markupJson).to.be.deep.equal(value);
}

export default verifyLdJson;
