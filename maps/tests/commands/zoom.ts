import {wrapAsyncCommand} from '../lib/commands-utils';
import cssSelectors from '../common/css-selectors';

async function zoom(this: WebdriverIO.Browser, zoomTo: number): Promise<void> {
    const {zoom: initialZoom} = await this.getMapGeoOptions();
    if (initialZoom === zoomTo) {
        return;
    }

    const selector = initialZoom < zoomTo ? cssSelectors.mapControls.zoom.in : cssSelectors.mapControls.zoom.out;
    const increment = initialZoom < zoomTo ? 1 : -1;

    for (let zoom = initialZoom; Math.abs(zoom - zoomTo) !== 0; zoom += increment) {
        await this.waitAndClick(selector);
        await this.waitForUrlContains({query: {z: zoom + increment}}, {partial: true});
    }
}

export default wrapAsyncCommand(zoom);
