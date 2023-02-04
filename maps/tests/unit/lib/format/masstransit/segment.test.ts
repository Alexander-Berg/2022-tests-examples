import {expect} from 'chai';
import {
    formatWalkSegmentDescription,
    formatTransportSegmentDescription,
    formatTransferSegmentDescription
} from 'app/lib/format/masstransit/segment';

describe('formatWalkSegmentDescription', () => {
    describe('"ru" lang', () => {
        it('should format walk segment with destination stop', () => {
            expect(formatWalkSegmentDescription({
                distance: 128,
                duration: 92,
                nextSegmentFirstStop: {
                    id: 'stop__9642272',
                    name: 'Битцевская аллея'
                }
            })).to.equal('До остановки «Битцевская аллея»\n130 м, 2 мин в пути');
        });

        it('should consider destination stop type', () => {
            expect(formatWalkSegmentDescription({
                distance: 128,
                duration: 92,
                nextSegmentFirstStop: {
                    id: 'station__lh_9601291',
                    name: 'Бутово'
                }
            })).to.equal('До станции «Бутово»\n130 м, 2 мин в пути');
        });

        it('should format walk segment without destination stop', () => {
            expect(formatWalkSegmentDescription({
                distance: 128,
                duration: 92
            })).to.equal('130 м, 2 мин в пути');
        });
    });
});

describe('formatTransportSegmentDescription', () => {
    describe('"ru" lang', () => {
        it('should format underground transport segment', () => {
            // Route example: "м Пражская", "м Парк культуры"
            expect(formatTransportSegmentDescription({
                duration: 1260,
                transports: [
                    {
                        id: '100000078',
                        name: 'Серпуховско-Тимирязевская линия',
                        type: 'underground'
                    }
                ],
                lastStop: {
                    id: 'station__9858903',
                    name: 'Серпуховская'
                }
            })).to.equal('До станции «Серпуховская»\n21 мин в пути');
        });

        it('should format non-underground transport with transport numbers', () => {
            // Route example: "Днепропетровская улица, 16А", "Чертановская улица, 2к2с3"
            expect(formatTransportSegmentDescription({
                duration: 209,
                transports: [
                    {
                        id: '213_674_bus_mosgortrans',
                        name: '674',
                        type: 'bus'
                    },
                    {
                        id: '674k_bus_default',
                        name: '674к',
                        type: 'bus'
                    },
                    {
                        id: '213_674m_minibus_default',
                        name: '674к',
                        type: 'minibus'
                    }
                ],
                lastStop: {
                    id: 'stop__9642104',
                    name: 'Кинотеатр Ашхабад'
                }
            })).to.equal('Автобусы №№ 674, 674к\nМаршрутка № 674к\nДо остановки «Кинотеатр Ашхабад»\n3 мин в пути');
        });
    });
});

describe('formatTransferSegmentDescription', () => {
    describe('"ru" lang', () => {
        it('should format transfer between underground stations', () => {
            // Route example: "м Пражская", "м Парк культуры"
            expect(formatTransferSegmentDescription({
                previousSegmentTransport: {
                    id: '100000078',
                    name: 'Серпуховско-Тимирязевская линия',
                    type: 'underground'
                },
                nextSegmentTransport: {
                    id: '100000088',
                    name: 'Кольцевая линия',
                    type: 'underground'
                },
                nextSegmentFirstStop: {
                    id: 'station__9858810',
                    name: 'Добрынинская'
                }
            })).to.equal('Переход на станцию «Добрынинская» (Кольцевая линия)');
        });
    });
});
