const cases = [
    {name: 'Варшавское кольцо', coords: [52.073990, 23.679559]},
    {name: 'Проспект независимости', coords: [53.908941, 27.575514]},
    {name: 'Вулица Франциска Скорины', coords: [53.933610, 27.699195]},
    {name: 'Набережная Воинов-Интернационалистов', coords: [53.912777, 27.548228]},
    {name: 'Слепянская водная система', coords: [53.908460, 27.617722]}
];

describe('Labels on the corner', () => {
    for (let k = 0; k < cases.length; k++) {
        for (let i = 12; i < 19.5; i += 0.5) {
            it(`${cases[k].name}: zoom : ${i}`, async ({browser}) => {
                await browser.openMap({center: cases[k].coords, zoom: i});
                await browser.verifyScreenshot(k + '-zoom-' + i, PO.map());
            });
        }
    }
});
