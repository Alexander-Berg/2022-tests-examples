import {wrapAsyncCommand} from '../lib/commands-utils';
import cssSelectors from '../common/css-selectors';

async function twoFingersMapRotate(this: WebdriverIO.Browser, clockwise?: boolean): Promise<void> {
    await this.perform(async () => {
        const map = await this.$(cssSelectors.map.container);

        await this.performActions([
            {
                type: 'pointer',
                id: 'finger1',
                parameters: {pointerType: 'touch'},
                actions: [
                    {type: 'pointerMove', duration: 0, origin: map, x: 75, y: 0},
                    {type: 'pointerDown', button: 0},
                    {type: 'pointerMove', duration: 100, origin: map, x: 75, y: clockwise ? 75 : -75},
                    {type: 'pointerMove', duration: 100, origin: map, x: -75, y: clockwise ? 75 : -75},
                    {type: 'pointerUp', button: 0}
                ]
            },
            {
                type: 'pointer',
                id: 'finger2',
                parameters: {pointerType: 'touch'},
                actions: [
                    {type: 'pointerMove', duration: 0, origin: map, x: -75, y: 0},
                    {type: 'pointerDown', button: 0},
                    {type: 'pointerMove', duration: 100, origin: map, x: -75, y: clockwise ? -75 : 75},
                    {type: 'pointerMove', duration: 100, origin: map, x: 75, y: clockwise ? -75 : 75},
                    {type: 'pointerUp', button: 0}
                ]
            }
        ]);
        await this.releaseActions();
    }, `Совершить поворот карты двумя пальцами ${clockwise ? 'по' : 'против'} часовой стрелки`);
}

export default wrapAsyncCommand(twoFingersMapRotate);
