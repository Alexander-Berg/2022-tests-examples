// eslint-disable-next-line @typescript-eslint/no-explicit-any
function checkListBodyShape(body: any): void {
    expect(body.hasOwnProperty('meta')).toBeTruthy();
    expect(typeof body.meta).toEqual('object');

    expect(body.meta.hasOwnProperty('limit')).toBeTruthy();
    expect(typeof body.meta.limit).toEqual('number');

    expect(body.meta.hasOwnProperty('offset')).toBeTruthy();
    expect(typeof body.meta.offset).toEqual('number');

    expect(body.meta.hasOwnProperty('total')).toBeTruthy();
    expect(typeof body.meta.total).toEqual('number');

    expect(body.hasOwnProperty('data')).toBeTruthy();
    expect(Array.isArray(body.data)).toBeTruthy();
}

export {checkListBodyShape};
