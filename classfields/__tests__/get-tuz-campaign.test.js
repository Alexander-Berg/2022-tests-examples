const { getTuzCampaign } = require('app/controller/lib/get-tuz-campaign');

describe('getTuzCampaign', () => {
    it('correct find extended tariff', () => {
        const partialCampaign = {
            product: {
                goods: [
                    {
                        id: 'TUZ',
                        cost: {
                            costPerCall: 1
                        }
                    }
                ]
            }
        };

        expect(getTuzCampaign([ partialCampaign ])).toEqual(partialCampaign);
    });

    it('correct find maximum tariff', () => {
        const partialCampaign = {
            product: {
                goods: [
                    {
                        id: 'Maximum',
                        cost: {
                            costPerCall: 1
                        }
                    }
                ]
            }
        };

        expect(getTuzCampaign([ partialCampaign ])).toEqual(partialCampaign);
    });

    it('correct find calls minimum tariff', () => {
        const partialCampaign = {
            product: {
                goods: [
                    {
                        id: 'CallsMinimum',
                        cost: {
                            costPerCall: 1
                        }
                    }
                ]
            }
        };

        expect(getTuzCampaign([ partialCampaign ])).toEqual(partialCampaign);
    });

    it('not found tuz tariff', () => {
        const partialCampaign = {
            product: {
                goods: [
                    {
                        id: 'feed_raise',
                        cost: {
                            costPerCall: 1
                        }
                    }
                ]
            }
        };

        expect(getTuzCampaign([ partialCampaign ])).toEqual(undefined);
    });
});
