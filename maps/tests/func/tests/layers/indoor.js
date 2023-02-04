const {WAIT_FOR_UPDATE} = require('../../constants');
const cases = [
    {name: 'Охотный ряд', coords: [55.756357, 37.615909]},
    {name: 'ГУМ', coords: [55.754793, 37.621406]},
    {name: 'Совёнок', coords: [55.795700, 37.593396]},
    {name: 'Савеловский вокзал', coords: [55.794382, 37.588309]},
    {name: 'Аэропорт Шереметьево', coords: [55.966746, 37.415543]}
];

describe('Indoor', () => {
    for (let k = 0; k < cases.length; k++) {
        for (let i = 16; i <= 19; i++) {
            it(`${cases[k].name}: zoom : ${i}`, async ({browser}) => {
                await browser.openMap({center: cases[k].coords, zoom: i});
                await browser.execute(addIndoor).catch((err) => new Error(err));
                await browser.verifyNoErrors();
                await browser.waitForTilesLoaded();
                await browser.pause(WAIT_FOR_UPDATE);
                await browser.verifyScreenshot(`indoor-${k}-${i}`, PO.map());
            });
        }
    }

    it('change floor', async ({browser}) => {
            await browser.openMap({center: [55.755976, 37.614851], zoom: 18});
            await browser.execute(addIndoor).catch((err) => new Error(err));
            await browser.verifyNoErrors();
            await browser.waitForTilesLoaded();
            await browser.pause(WAIT_FOR_UPDATE);
            await browser.waitForVisible(PO.map.controls.button1());

            await changeFloor.call(browser, '1');
            await changeFloor.call(browser, '2');
            await changeFloor.call(browser, '3');
            await changeFloor.call(browser, '4');
    });

    function addIndoor() {
        ymaps.modules.require([
            'control.Button', 'control.TypeSelector',
            'indoor.IndoorPlanProvider', 'coordSystem.geo'
        ]).spread(
            (Button, TypeSelectorControl,
                IndoorPlanProvider, geoCoordSystem) => {
                myMap.controls.add(new TypeSelectorControl());

                var indoorProvider = new IndoorPlanProvider();
                indoorProvider.setMap(myMap);
                var indoorButtons = [];
                var activePlan = null;
                var currentPlans = [];

                checkIndoorPlans();
                indoorProvider.events.add('planschange', checkIndoorPlans);

                myMap.events.add('boundschange', checkActivePlan);
                function checkIndoorPlans() {
                    currentPlans = indoorProvider.getPlans();
                    if (currentPlans) {
                        currentPlans.forEach((plan) => {
                            plan.setVisible(true);
                        });
                    }
                    checkActivePlan();
                }

                function checkActivePlan() {
                    var newPlan = getActivePlan();
                    if (activePlan !== newPlan) {
                        activePlan = newPlan;
                        indoorButtons.forEach((button) => {
                            myMap.controls.remove(button);
                        });
                        indoorButtons = [];
                        if (!activePlan) {
                            return;
                        }
                        activePlan.events.add('activelevelchange', checkSelectedLevel);
                        var levels = activePlan.getLevels();
                        levels.forEach((level) => {
                            var newButton = new Button(level.id);
                            newButton.options.set('selectOnClick', false);
                            newButton.events.add('click', () => {
                                activePlan.setActiveLevel(level.id);
                            });
                            myMap.controls.add(newButton);
                            indoorButtons.push(newButton);
                        });
                        checkSelectedLevel();
                    }
                }

                function getActivePlan() {
                    currentPlans = indoorProvider.getPlans();
                    if (!currentPlans || currentPlans.length === 0) {
                        return null;
                    }

                    if (currentPlans.length === 1) {
                        return currentPlans[0];
                    }

                    var centers = currentPlans.map((plan) => {
                        var bbox = plan.getBounds();
                        return [(bbox[0][0] + bbox[1][0]) / 2, (bbox[0][1] + bbox[1][1]) / 2];
                    });

                    var newActivePlan = null;
                    var minDistance = Infinity;
                    centers.forEach((point, i) => {
                        var distance = geoCoordSystem.getDistance(point, myMap.getCenter());
                        if (distance < minDistance) {
                            newActivePlan = currentPlans[i];
                            minDistance = distance;
                        }
                    });
                    return newActivePlan;
                }

                function checkSelectedLevel() {
                    if (!activePlan) {
                        return;
                    }
                    var activeLevel = activePlan.getActiveLevel();
                    indoorButtons.forEach((button) => {
                        if (button.data.get('content') === activeLevel.id) {
                            button.select();
                        } else {
                            button.deselect();
                        }
                    });
                }
            },
            (err) => {
                throw new Error(err);
            }
        ).fail((err) => {
            throw new Error(err);
        });

        return true;
    }

    async function changeFloor(floor) {
        await this.click(PO.map.controls['button' + floor]());
        await this.pause(WAIT_FOR_UPDATE);
        await this.verifyNoErrors();
        await this.waitForTilesLoaded();
        await this.pause(WAIT_FOR_UPDATE);
        await this.verifyScreenshot(floor, PO.map());
    }
});
