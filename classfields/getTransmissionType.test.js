const getTransmissionType = require('auto-core/react/lib/seo/listingCommon/getTransmissionType');

it('Возвращает полное имя', () => {
    expect(getTransmissionType([ 'ROBOT' ]).fullName).toEqual(' c роботизированной коробкой передач');
});

it('Возвращает сокращенное имя', () => {
    expect(getTransmissionType([ 'MECHANICAL' ]).shortName).toEqual(' с МКПП');
});

it('Если переданы все автоматические коробки передач, возващает автомат', () => {
    expect(getTransmissionType([ 'ROBOT', 'AUTOMATIC', 'VARIATOR', 'AUTO' ]).shortName).toEqual(' с АКПП');
});
