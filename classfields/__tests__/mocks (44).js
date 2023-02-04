const commonData = {
    site: {
        id: '1',
        name: 'РЕКА',
        rgid: '193370'
    },
    sliderPhoto: {
        mainPhoto: {
            optimize: 'http://site.com/image.png'
        },
        desktopPhoto: {
            optimize: 'http://site.com/image.png'
        },
        mobilePhoto: {
            optimize: 'http://site.com/image.png'
        }
    }
};

const pictureSlide = {
    ...commonData,
    type: 'PICTURE',
    title: 'Баннер',
    button: true,
    buttonText: 'Узнать больше'
};

const titleSlide = {
    ...commonData,
    type: 'TEXT',
    title: 'Баннер'
};

const featuresSlide = {
    ...commonData,
    type: 'TEXT',
    title: 'Баннер',
    titlePriority: [ 'Лучший ЖК – 1', 'Лучший ЖК – 2' ]
};

const buttonSlide = {
    ...commonData,
    type: 'TEXT',
    title: 'Баннер',
    titlePriority: [ 'Лучший ЖК – 1', 'Лучший ЖК – 2' ],
    button: true,
    buttonText: 'Узнать больше'
};

export const developerWithOneSlide = {
    id: '1',
    name: 'Самолет',
    isExtended: true,
    sliders: [ pictureSlide ]
};

export const developerWithSeveralSlides = {
    id: '1',
    name: 'Самолет',
    isExtended: true,
    sliders: [ pictureSlide, titleSlide, featuresSlide, buttonSlide ]
};
