class Response {
    getHeader() { }
    setHeader() { }
}

class Request {
    constructor(originalUrl) {
        this.originalUrl = originalUrl;

        this.query = {};

        this.url = 'https://realty.yandex.ru' + originalUrl;

        this.is404 = true;
    }
}

const nextFunction = jest.fn();

module.exports = {
    Response,
    Request,
    nextFunction
};
