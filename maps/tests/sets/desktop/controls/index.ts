import cssSelectors from '../../../common/css-selectors';
import getSelectorByText from '../../../../tests/lib/func/get-selector-by-text';

describe('Контролы.', () => {
    it('Включение слоя ОТ', async function () {
        await this.browser.openPage('?ll=37.625886,55.753960&z=10');
        await this.browser.waitAndClick(cssSelectors.headerControls.masstransit);
        await this.browser.waitForVisible(cssSelectors.headerControls.masstransitChecked);
        await this.browser.waitForUrlContains({path: 'transport'}, {partial: true});
    });

    it('Выключение слоя ОТ', async function () {
        await this.browser.openPage('?ll=37.625886,55.753960&z=10&masstransit[layer]=true');
        await this.browser.waitAndClick(cssSelectors.headerControls.masstransitChecked);
        await this.browser.waitForUrlContains({path: 'transport'}, {partial: true, inverse: true});
    });

    it('Переключение со слоя ОТ на пробки', async function () {
        await this.browser.openPage('?ll=37.625886,55.753960&z=10');
        await this.browser.waitAndClick(cssSelectors.headerControls.masstransit);
        await this.browser.waitForUrlContains({path: 'transport'}, {partial: true});
        await this.browser.waitAndClick(cssSelectors.headerControls.traffic);
        await this.browser.waitForVisible(cssSelectors.headerControls.trafficChecked);
        await this.browser.waitForVisible(cssSelectors.headerControls.masstransitChecked);
        await this.browser.waitForUrlContains({query: {l: 'trf,trfe,masstransit'}}, {partial: true});
    });

    it('Переключение с пробок на слой ОТ', async function () {
        await this.browser.openPage('?ll=37.625886,55.753960&z=10');
        await this.browser.waitAndClick(cssSelectors.headerControls.traffic);
        await this.browser.waitForUrlContains({query: {l: 'trf,trfe'}}, {partial: true});
        await this.browser.waitAndClick(cssSelectors.headerControls.masstransit);
        await this.browser.waitForVisible(cssSelectors.headerControls.trafficChecked);
        await this.browser.waitForVisible(cssSelectors.headerControls.masstransitChecked);
        await this.browser.waitForUrlContains({query: {l: 'trf,trfe,masstransit'}}, {partial: true});
    });

    it('Переключение со слоя ОТ на парковки', async function () {
        await this.browser.openPage('/213/moscow/transport/?ll=37.622504%2C55.753215&z=17');
        await this.browser.waitAndClick(cssSelectors.map.layers.control);
        await this.browser.waitAndClick(getSelectorByText('Парковки', cssSelectors.map.layers.view));
        await this.browser.waitForUrlContains({path: 'transport', query: {l: 'carparks'}}, {partial: true});
        await this.browser.waitForVisible(cssSelectors.headerControls.masstransitChecked);
    });

    it('Переключение с парковок на слой ОТ', async function () {
        await this.browser.openPage('/213/moscow/?l=carparks&ll=37.611020%2C55.764572&z=15');
        await this.browser.waitAndClick(cssSelectors.headerControls.masstransit);
        await this.browser.waitForUrlContains({path: 'transport', query: {l: 'carparks'}}, {partial: true});
        await this.browser.waitForVisible(cssSelectors.headerControls.masstransitChecked);
    });

    it('Переключение с включенных слоёв пробок и ОТ на слой панорам', async function () {
        await this.browser.openPage('/213/moscow/?l=trf%2Ctrfe%2Cmasstransit&ll=37.618779%2C55.757573&z=15.73');
        await this.browser.waitAndClick(cssSelectors.mapControls.panoramaPhoto.mainControl);
        await this.browser.waitForUrlContains({query: {l: 'stv,sta'}}, {partial: true});
        await this.browser.waitForHidden(cssSelectors.headerControls.masstransitChecked);
        await this.browser.waitForHidden(cssSelectors.headerControls.trafficChecked);
    });

    it('Включение слоя парковок', async function () {
        await this.browser.openPage('?ll=37.625886,55.753960&z=15');
        await this.browser.waitAndClick(cssSelectors.map.layers.control);
        await this.browser.waitAndClick(getSelectorByText('Парковки', cssSelectors.map.layers.view));
        await this.browser.waitForUrlContains({query: {l: 'carparks'}}, {partial: true});
    });

    it('Переключение со слоя парковок на пробки', async function () {
        await this.browser.openPage('?ll=37.625886,55.753960&z=15');
        await this.browser.waitAndClick(cssSelectors.map.layers.control);
        await this.browser.waitAndClick(getSelectorByText('Парковки', cssSelectors.map.layers.view));
        await this.browser.waitForUrlContains({query: {l: 'carparks'}}, {partial: true});
        await this.browser.waitAndClick(getSelectorByText('Пробки', cssSelectors.map.layers.view));
        await this.browser.waitForUrlContains({query: {l: 'trf,trfe'}}, {partial: true});
    });

    it('Клик в логотип', async function () {
        await this.browser.openPage('/');
        await this.browser.waitAndVerifyLink(cssSelectors.mapControls.logo, 'https://yandex.ru/');
    });
});
