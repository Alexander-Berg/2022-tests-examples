const rows = [
    {
        DeviceIDHash: '7071321290416261339', // 'device-hash-1'
        segments: ['test-1']
    },
    {
        DeviceIDHash: 'device-hash-3',
        segments: ['test-3']
    },
    {
        DeviceIDHash: 'device-hash-4',
        segments: ['test-4']
    }
];

const ytDeviceSegments = rows.map((item) => JSON.stringify(item)).join('\n');

export default ytDeviceSegments;
