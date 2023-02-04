const commonRewardData = {
    description: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit.',
    rewardImage: {
        appMiddle: 'http://site.com/image.png'
    }
};

const rewards = [
    {
        ...commonRewardData,
        name: 'Награда 1',
        year: 2018
    },
    {
        ...commonRewardData,
        name: 'Награда 2',
        year: 2019
    },
    {
        ...commonRewardData,
        name: 'Награда 3'
    }
];

export const developerWithRewards = {
    id: '1',
    name: 'Самолет',
    isExtended: true,
    rewards
};

const partialRewards = [
    {
        rewardImage: {
            appMiddle: 'http://site.com/image.png'
        }
    },
    {
        ...commonRewardData
    },
    {
        name: 'Награда 3',
        rewardImage: {
            appMiddle: 'http://site.com/image.png'
        }
    }
];

export const developerWithPartialRewards = {
    id: '1',
    name: 'Самолет',
    isExtended: true,
    rewards: partialRewards
};
