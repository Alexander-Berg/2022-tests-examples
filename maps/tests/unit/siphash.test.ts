import getSipHash from 'app/v3/helpers/get-siphash';

describe('getSipHash():', () => {
    it('should return value', async () => {
        const result = getSipHash('9f89c84a559f573636a47ff8daed0d33');
        expect(result).toEqual('16799602254536830119');
    });
});
