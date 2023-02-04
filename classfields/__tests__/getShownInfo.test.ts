import { getMetroFromLocation } from '../../libs/getMetroFromLocation';
import { getShownInfo } from '../getShownInfo';

import { LOCATION, MAIN_PHOTO_URL } from '../../__tests__/mocks';

describe('getShownInfo', () => {
    it('Возвращает ничего', () => {
        expect(getShownInfo({ mainPhotoUrl: undefined })).toMatchSnapshot();
    });

    it('Возвращает частично заполненный объект', () => {
        expect(getShownInfo({ mainPhotoUrl: MAIN_PHOTO_URL })).toMatchSnapshot();
    });

    it('Возвращает заполненный объект с информацией о новостройке', () => {
        expect(
            getShownInfo({
                mainPhotoUrl: MAIN_PHOTO_URL,
                updateTime: 0,
                siteShownInfo: {
                    metro: getMetroFromLocation({ location: LOCATION }),
                },
            })
        ).toMatchSnapshot();
    });

    it('Возвращает заполненный объект с информацией об оффере', () => {
        expect(
            getShownInfo({
                mainPhotoUrl: MAIN_PHOTO_URL,
                updateTime: 0,
                offerShownInfo: {
                    metro: getMetroFromLocation({ location: LOCATION }),
                },
            })
        ).toMatchSnapshot();
    });
});
