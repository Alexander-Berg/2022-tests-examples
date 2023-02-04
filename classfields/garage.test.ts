import garageSeo from './garage';
import type { State } from './garage';

const CATALOG_IMG_SRC = 'catalog jpg';
const USER_IMG_SRC = 'user photo jpg';
const LOGO_SRC = 'logo jpg';

jest.mock('auto-core/router/auto.ru/react/link', () => jest.fn());

const getState = (userPhoto: string | undefined, catalogPhoto: string | undefined, logo?: string) => {
    return {
        garageCard: {
            data: {
                card: {
                    vehicle_info: {
                        car_info: {
                            configuration: {
                                main_photo: {
                                    sizes: {
                                        'mini-card': catalogPhoto,
                                    },
                                },
                            },
                            mark_info: {
                                logo: {
                                    sizes: {
                                        orig: logo,
                                    },
                                },
                            },
                        },
                        vehicle_images: [ {
                            sizes: {
                                full: userPhoto,
                            },
                        } ],
                    },
                },
            },
        },
    } as unknown as State;
};

it('вернет картинку из каталога, если нет юзерфото', () => {
    const state = getState(undefined, CATALOG_IMG_SRC);
    const result = garageSeo(state);
    expect(result.ogImage).toBe(CATALOG_IMG_SRC);
});

it('вернет картинку из юзерфото, если есть', () => {
    const state = getState(USER_IMG_SRC, CATALOG_IMG_SRC);
    const result = garageSeo(state);
    expect(result.ogImage).toBe(USER_IMG_SRC);
});

it('вернет лого на крайний случай', () => {
    const state = getState(undefined, undefined, LOGO_SRC);
    const result = garageSeo(state);
    expect(result.ogImage).toBe(LOGO_SRC);
});
