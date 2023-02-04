import {Table} from 'app/types/consts';

const rows = [
    {
        DeviceIDHash: '7071321290416261339', // 'device-hash-1'
        segments: JSON.stringify(['segment-1'])
    },
    {
        DeviceIDHash: '2000457444287749023', // 'device-hash-2'
        segments: JSON.stringify(['segment-1']),
        updated_time: '2000-01-01 09:46:30.451905'
    }
];

const devices = {
    table: Table.DEVICES,
    rows
};

export {devices};
