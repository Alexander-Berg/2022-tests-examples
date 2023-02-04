const props = require('./configs/autotests/properties.js');
const objs = require('./configs/autotests/objects.js');

describe('Map customization. Colors.', () => {
  for (const obj in objs) {
    describe(`In ${obj}`, () => {
      for (const color in props.colors.values) {
        it(`Color value: ${props.colors.values[color]}`, async ({browser}) => {
          const styler = {};
          styler[`${props.colors.name}`] = props.colors.values[color];

          await browser.openCustomMap({
            center: objs[obj].mapSpan.center,
            zoom: objs[obj].mapSpan.zoom,
            custom: [
              {
                "types": obj,
                "stylers": [
                  styler
                ]
              }
            ]
          });
          await browser.pause(15000);
          await browser.verifyScreenshot(`custom-color-${obj}-${props.colors.values[color]}`, PO.map());

        })
      }
    })
  }
});

describe('Map customization. Visibility.', () => {
  for (const obj in objs) {
    it(`In ${obj}. Value: ${props.visibility.values[0]}`, async ({browser}) => {
      const styler = {};
      styler[`${props.visibility.name}`] = props.visibility.values[0];

      await browser.openCustomMap({
          center: objs[obj].mapSpan.center,
          zoom: objs[obj].mapSpan.zoom,
          custom: [
            {
              "types": obj,
              "stylers": [
                styler
              ]
            }
          ]
        });
      await browser.pause(15000);
      await browser.verifyScreenshot(`custom-visibility-${obj}-${props.visibility.values[0]}`, PO.map());

    })
  }
})
