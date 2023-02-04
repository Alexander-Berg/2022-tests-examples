function openYMaps3(this: WebdriverIO.Browser, name: string) {
    return this.url(`${process.env.YMAPS3_URL}/${name}/index.html?mock=1`);
}

export default openYMaps3;
