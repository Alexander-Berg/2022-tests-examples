import getMaskPhone from 'realty-core/view/react/libs/get-phone-mask';
import { encrypt } from 'realty-core/app/lib/crypto_phone';

const phone = '+79876543210';
const commonDeveloperParams = {
    id: '1',
    name: 'Самолет',
    geoStatistic: {
        current: {}
    }
};

export const onlyTitleDeveloper = {
    ...commonDeveloperParams
};

export const developerWithLogo = {
    ...commonDeveloperParams,
    logotype: {
        appMiddle: 'http://site.com/image.png'
    }
};

export const developerWithAllObjects = {
    ...developerWithLogo,
    geoStatistic: {
        current: {
            statistic: {
                finished: {
                    houses: 50,
                    sites: 25
                },
                unfinished: {
                    houses: 78,
                    sites: 33
                },
                suspended: {
                    houses: 3,
                    sites: 1
                }
            }
        }
    },
    born: '1998-06-31T21:00:00Z'
};

export const developerWithOneItemAtAllObjects = {
    ...developerWithLogo,
    geoStatistic: {
        current: {
            statistic: {
                finished: {
                    houses: 1,
                    sites: 1
                },
                unfinished: {
                    houses: 1,
                    sites: 1
                },
                suspended: {
                    houses: 1,
                    sites: 1
                }
            }
        }
    },
    born: '1998-06-31T21:00:00Z'
};

export const developerWithSomeObjects = {
    ...developerWithLogo,
    geoStatistic: {
        current: {
            statistic: {
                finished: {
                    houses: 50,
                    sites: 25
                }
            }
        }
    },
    born: '1998-06-31T21:00:00Z'
};

export const developerWithFewRegions = {
    ...developerWithAllObjects,
    geoStatistic: {
        ...developerWithAllObjects.geoStatistic,
        regions: [
            {
                locativeSubjectFederationName: 'в Санкт-Петербурге и ЛО',
                rgid: 741964,
                totalSites: 21
            },
            {
                locativeSubjectFederationName: 'в Республике Карелия',
                rgid: 123321,
                totalSites: 12
            }
        ]
    }
};

export const developerWithLotRegions = {
    ...developerWithAllObjects,
    geoStatistic: {
        ...developerWithAllObjects.geoStatistic,
        regions: [
            {
                locativeSubjectFederationName: 'в Санкт-Петербурге и ЛО',
                rgid: 741964,
                totalSites: 21
            },
            {
                locativeSubjectFederationName: 'в Республике Карелия',
                rgid: 123321,
                totalSites: 12
            },
            {
                locativeSubjectFederationName: 'в Ярославской области',
                rgid: 123123,
                totalSites: 8
            },
            {
                locativeSubjectFederationName: 'в Псковской области',
                rgid: 321321,
                totalSites: 2
            },
            {
                locativeSubjectFederationName: 'в Ростовской области',
                rgid: 321123,
                totalSites: 1
            }
        ]
    }
};

export const developerWithDescription = {
    ...developerWithAllObjects,
    description: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. ' +
        'Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. ' +
        'Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. ' +
        'Donec pede justo, fringill.'
};

export const developerWithLongDescription = {
    ...developerWithAllObjects,
    description: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. ' +
        'Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. ' +
        'Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. ' +
        'Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, ' +
        'venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dapibus.'
};

export const developerWithAllSocialNetworks = {
    ...developerWithLogo,
    isExtended: true,
    vk: 'https://vk.com',
    youtube: 'https://www.youtube.com'
};

export const developerWithSeveralSocialNetworks = {
    ...developerWithLogo,
    isExtended: true,
    vk: 'https://vk.com'
};

export const developerWithSocialNetworksWithoutLogo = {
    ...commonDeveloperParams,
    isExtended: true,
    vk: 'https://vk.com',
    youtube: 'https://www.youtube.com'
};

export const developerWithDescriptionExtended = {
    ...developerWithAllSocialNetworks,
    description: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. ' +
        'Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. ' +
        'Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. ' +
        'Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet a, ' +
        'venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dapibus.'
};

export const developerWithLongDescriptionExtended = {
    ...developerWithAllSocialNetworks,
    description: 'Lorem ipsum dolor sit amet, consectetuer adipiscing elit. Aenean commodo ligula eget dolor. ' +
        'Aenean massa. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. ' +
        'Donec quam felis, ultricies nec, pellentesque eu, pretium quis, sem. Nulla consequat massa quis enim. ' +
        'Donec pede justo, fringilla vel, aliquet nec, vulputate eget, arcu. In enim justo, rhoncus ut, imperdiet ' +
        'a, venenatis vitae, justo. Nullam dictum felis eu pede mollis pretium. Integer tincidunt. Cras dapibus. ' +
        'Vivamus elementum semper nisi. Aenean vulputate eleifend tellus. Aenean leo ligula, porttitor eu, consequat ' +
        'vitae, eleifend ac, enim. Aliquam lorem ante, dapibus in, viverra quis, feugiat a, tellus. Phasellus ' +
        'viverra nulla ut metus varius laoreet. Quisque rutrum. Aenean imperdiet. Etiam ultricies nisi vel augue.'
};

export const developerWithContacts = {
    ...developerWithDescriptionExtended,
    phone: {
        phoneWithMask: getMaskPhone(phone, '××\u00a0××'),
        phoneHash: encrypt(phone)
    },
    backCallTrafficInfo: {}
};

export const developerWithLongDescriptionAndLotRegions = {
    ...developerWithLongDescription,
    ...developerWithLotRegions
};

export const developerWithLongDescriptionAndLotRegionsExtended = {
    ...developerWithLongDescriptionExtended,
    ...developerWithLotRegions
};
