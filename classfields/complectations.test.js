const complectations = require('./complectations');

it('должен вернуть список комплектаций', () => {
    const groupingInfo = {
        complectations: [
            { id: '20838807', name: 'Basis' },
            { id: '21597097', name: 'Advanced' },
            { id: '20838858', name: 'Sport' },
        ],
    };
    expect(complectations(groupingInfo)).toEqual('Basis, Advanced, Sport');
});

it('должен вернуть одно обрезанное название комплектации', () => {
    const groupingInfo = {
        complectations: [
            { id: '21597097', name: 'Advanced Super Ultra Premium' },
        ],
    };
    expect(complectations(groupingInfo, 15)).toEqual('Advanced Super…');
});

it('должен вернуть список комплектаций с обрезанным первым названием и дополнением "и ещё 2"', () => {
    const groupingInfo = {
        complectations: [
            { id: '21597097', name: 'Advanced Super Ultra Premium' },
            { id: '20838858', name: 'Sport' },
            { id: '68756435', name: 'Comfort' },

        ],
    };
    expect(complectations(groupingInfo, 30)).toEqual('Advanced Super Ultra… и ещё 2');
});

it('должен вернуть список комплектаций с дополнением "и ещё 2"', () => {
    const groupingInfo = {
        complectations: [
            { id: '20838807', name: 'Basis' },
            { id: '21597097', name: 'Advanced Super Ultra Premium' },
            { id: '20838858', name: 'Sport' },
            { id: '68756435', name: 'Comfort' },
        ],
    };
    expect(complectations(groupingInfo, 50)).toEqual('Basis, Advanced Super Ultra Premium и ещё 2');
});
