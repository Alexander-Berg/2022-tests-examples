const snippetEquipmentInfo = require('./snippetEquipmentInfo');

const equipmentDictionary = require('auto-core/react/dataDomain/equipmentDictionary/mocks/dictionary').default;
const presentEquipmentMock = require('autoru-frontend/mockData/state/presentEquipment.mock.js');

const OFFER = {
    vehicle_info: {
        complectation: {
            available_options: [
                '15-inch-wheels',
            ],
        },
        equipment: {
            '15-inch-wheels': true,
            'dark-interior': true,
            'automatic-lighting-control': true,
            'seats-5': true,
            'cruise-control': true,
            asr: true,
            esp: true,
        },
    },
};

it('должен вернуть списки названий опций и короткий без неважных (сначала дополнительные, потом базовые, с учётом веса) и общее количество опций', () => {
    expect(snippetEquipmentInfo(OFFER, presentEquipmentMock, equipmentDictionary.data, 4)).toStrictEqual({
        equipmentList: [
            'Тёмный салон', // 304780
            'Автоматический корректор фар', // 208099
            'Круиз-контроль', // 203590
            'Антипробуксовочная система (ASR)', // 202510
            'Система стабилизации', // 202340
        ],
        shortEquipmentList: [
            'Тёмный салон',
            'Автоматический корректор фар',
            'Круиз-контроль',
            'Антипробуксовочная система (ASR)',
        ],
        totalEquipmentCount: 7,
    });
});
