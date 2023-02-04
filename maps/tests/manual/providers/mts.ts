import * as fs from 'fs';
import {SocksProxyAgent} from 'socks-proxy-agent';
import {URL, URLSearchParams} from 'url';
import {v4 as uuidv4} from 'uuid';
import {json2xml, xml2json} from 'xml-js';
import * as Got from 'got';
import {loadSecret} from '../../../app/lib/secrets';
import {config} from '../../../app/config';

const got = Got.default.extend({
    agent: {
        https: new SocksProxyAgent(config['mtsBank.socksProxyUrl']!)
    },
    retry: 0
});

const MTS_USERNAME = loadSecret('mtsUsername');
const MTS_PASSWORD = loadSecret('mtsPassword');

export interface RegisterOrderResponse {
    orderId: string;
    formUrl: string;
}

interface OrderStatusAttribute {
    name: string;
    value: string;
}

export interface OrderStatusResponse {
    orderStatus: 0 | // заказ зарегистрирован, но не оплачен
                 1 | // предавторизованная сумма захолдирована (для двухстадийных платежей)
                 2 | // проведена полная авторизация суммы заказа
                 3 | // авторизация отменена
                 4 | // по транзакции была проведена операция возврата
                 5 | // инициирована авторизация через ACS банка-эмитента
                 6;  // авторизация отклонена
    errorCode: string;
    errorMessage: string;
    orderNumber: string;
    amount: number;
    currency: string;
    date: number;
    attributes: OrderStatusAttribute[];
    bindingInfo: {
        clientId: string;
        bindingId: string;
    };
    cardAuthInfo: {
        maskedPan: string;
    };
    authRefNum: string; // rrn
    paymentAmountInfo: {
        approvedAmount: number;
    };
    ip: string;
}

interface Binding {
    bindingId: string;
    maskedPan: string;
    expiryDate: string;
}

export interface BindingsResponse {
    errorCode: string;
    errorMessage: string;
    bindings: Binding[];
}

interface RefundResponse {
    errorCode: number;
}

const tzoffset = (new Date()).getTimezoneOffset() * 60000; // offset in milliseconds
const currentDate = (new Date(Date.now() - tzoffset)).toISOString().substring(0, 10); // MSK time
const salt = Math.floor(Math.random() * 1e4);
const TMP_FILE_PATH = '/tmp/mts-operation-ordinal-number.txt';
function getOperationNumber(): number {
    let data = '';
    if (fs.existsSync(TMP_FILE_PATH)) {
        data = fs.readFileSync(TMP_FILE_PATH).toString();
    }
    let [date, counter] = data.split(' ');

    if (date !== currentDate) {
        date = currentDate;
        counter = salt + '00';
    }

    let result = parseInt(counter, 10);
    fs.writeFileSync(TMP_FILE_PATH, `${date} ${++result}`);

    return result;
}

interface PreprocessExternalTransferPayloadParams {
    maskedPan: string;
    amountInCents: number;
    phoneNumber: string;
    rrn: string;
}

function getPreprocessExternalTransferPayload(params: PreprocessExternalTransferPayloadParams) {
    const operationNumber = String(getOperationNumber()).padStart(6, '0');
    return json2xml(JSON.stringify({
        _declaration: {
            _attributes: {version: '1.0', encoding: 'UTF-8'}
        },
        FDX: {
            ServerInfo: {
                MsgUID: uuidv4(),
                RqUID: uuidv4(), // pass the same value to external transfer
                MsgReceiver: 'PHUB',
                SPName: 'BPA_YANDEXPARK',
                ServerDt: new Date(Date.now() - tzoffset).toISOString().substring(0, 19), // MSK time
                MsgType: 'preprocessExternalTransfer'
            },
            BankSvcRq: {
                preprocessExternalTransfer: {
                    document: {
                        type: 'BUDGETPAY',

                        // format:
                        // [1-16] 1044525232002268
                        // [17-24] current date DDMMYYYY
                        // [25-26] 72
                        // [27-32] operation ordinal number for the whole day
                        documentId: `1044525232002268${currentDate.split('-').reverse().join('')}72${operationNumber}`,
                        sourceProductType: 'CARD',
                        sourceProductId: params.maskedPan,
                        currencyISOCode: 643, // RUB
                        sum: params.amountInCents,
                        totalSum: params.amountInCents,

                        // AMPP info
                        receiverBankBIC: '004525988',
                        receiverBankCorAccount: '40102810545370000003',
                        receiverBankName: 'ГУ Банка России по ЦФО//УФК по г. Москве',
                        receiverAccount: '03222643450000007300',
                        receiverName: 'Департамент финансов города Москвы  (ГКУ "АМПП" л/с 2178031000451830)',
                        receiverINN: '7714887870',
                        receiverKPP: '771401001',

                        payCode: 0,
                        taxInfo: {
                            taxPayGround: 0,
                            taxType: 0,
                            taxKBK: '00000000000000000006',
                            taxStatus: '01',
                            taxPeriod: 0,
                            taxDocNumber: 0,
                            taxDocDate: 0,
                            oktmo: '45334000'
                        },
                        purpose: 'Оплата парковок АМПП. НДС не облагается',
                        additionalFieldList: {
                            additionalField: {
                                name: 'NUM_TELEFON',
                                value: params.phoneNumber
                            }
                        },
                        payerInfo: {
                            payerName: 'ОБЩЕСТВО С ОГРАНИЧЕННОЙ ОТВЕТСТВЕННОСТЬЮ "Яндекс"',
                            payerINN: '7736207543',
                            payerId: '2007736207543997750001', // 200 + payerINN + payerKPP
                            payerKPP: '997750001'
                        },
                        personPayerEqual: 'false'
                    }
                }
            }
        }
    }), {compact: true, spaces: 4});
}

interface ExternalTransferPayloadParams {
    requestId: string;
    externalDocumentId: number;
    rrn: string;
}

function getExternalTransferPayload(params: ExternalTransferPayloadParams) {
    return json2xml(JSON.stringify({
        _declaration: {
            _attributes: {version: '1.0', encoding: 'UTF-8'}
        },
        FDX: {
            ServerInfo: {
                MsgUID: uuidv4(),
                RqUID: params.requestId,
                MsgReceiver: 'PHUB',
                SPName: 'BPA_YANDEXPARK',
                ServerDt: new Date(Date.now() - tzoffset).toISOString().substring(0, 19), // MSK time
                MsgType: 'externalTransfer'
            },
            BankSvcRq: {
                externalTransfer: {
                    document: {
                        type: 'BUDGETPAY',
                        docExtId: params.externalDocumentId,
                        additionalFieldList: {},
                        RRN: params.rrn
                    }
                }
            }
        }
    }), {compact: true, spaces: 4});
}

class MtsPaymentProvider {
    async _request(method: string, params: Record<string, string | undefined>, gotOptions: Got.Options = {}) {
        const searchParams = new URLSearchParams(params);
        searchParams.append('userName', MTS_USERNAME);
        searchParams.append('password', MTS_PASSWORD);

        [...searchParams.entries()].forEach(([key, value]) => {
            if (value === 'undefined') {
                searchParams.delete(key);
            }
        });

        const url = new URL(`https://web.rbsuat.com/mtsbank/rest/${method}.do`);
        url.search = new URLSearchParams(searchParams).toString();

        // tslint:disable-next-line: no-console
        console.log(`${gotOptions.method || 'GET'} ${url}`);
        const response: Got.Response<any> = await got(url, {
            method: gotOptions.method,
            responseType: 'json',
            throwHttpErrors: false
        });

        if (response.statusCode !== 200) {
            throw new Error(`Unexepected status code ${response.statusCode || 'UNKNOWN'} when executing ${url}`);
        }

        return response.body;
    }

    async registerOrder(amount: number, clientId: string | undefined = undefined): Promise<RegisterOrderResponse> {
        return this._request('register', {
            orderNumber: `yandex-test-${Date.now()}`,
            clientId: clientId,
            currency: '643', // RUB
            language: 'ru',
            amount: amount.toString(),
            returnUrl: 'http://localhost/success/',
            failUrl: 'http://localhost/fail/'
        });
    }

    async getOrderStatus(orderId: string): Promise<OrderStatusResponse> {
        return this._request('getOrderStatusExtended', {
            orderId: orderId,
            language: 'ru'
        });
    }

    async getBingings(clientId: string): Promise<BindingsResponse> {
        return this._request('getBindings', {clientId});
    }

    async payOrderByBinding(mdOrder: string, bindingId: string, ip: string): Promise<BindingsResponse> {
        return this._request('paymentOrderBinding', {mdOrder, bindingId, ip}, {method: 'POST'});
    }

    async removeBinding(bindingId: string): Promise<any> {
        return this._request('unBindCard', {bindingId}, {method: 'POST'});
    }

    async refund(orderId: string, amount: number): Promise<RefundResponse> {
        return this._request('refund', {orderId, amount: amount.toString()});
    }

    async _request2(payload: string) {
        const URL = 'https://dp-test.mtsbank.ru:9296/YandexParkHttpFacade/Payments';
        const response: Got.Response<any> = await got(URL, {
            method: 'POST',
            throwHttpErrors: false,
            headers: {
                'Content-type': 'application/x-www-form-urlencoded'
            },
            body: payload,
            https: {
                certificate: loadSecret('mtsCertificate'),
                key: loadSecret('mtsPrivateKey'),
                certificateAuthority: loadSecret('mtsCertificateAuthority')
            }
        });
        // tslint:disable-next-line: no-console
        console.log(`POST ${URL}\nPayload:\n${payload}\nResponse:\n${response.body}`);
        return response.body;
    }

    async makeExternalTransfer(params: PreprocessExternalTransferPayloadParams): Promise<any> {
        const preprocessExternalTransferPayload = getPreprocessExternalTransferPayload(params);
        const preprocessExternalTransferResponse = await this._request2(preprocessExternalTransferPayload);
        const preprocessData = JSON.parse(xml2json(preprocessExternalTransferResponse, {compact: true, spaces: 4}));

        const externalTransferPayload = getExternalTransferPayload({
            requestId: preprocessData.FDX.ServerInfo.RqUID._text,
            externalDocumentId: preprocessData.FDX.BankSvcRs.preprocessExternalTransfer.document.docExtId._text,
            rrn: params.rrn
        });
        const externalTransferResponse = await this._request2(externalTransferPayload);
        const externalTransferData = JSON.parse(xml2json(externalTransferResponse, {compact: true, spaces: 4}));

        return {
            requestId: preprocessData.FDX.ServerInfo.RqUID._text,
            documentId: preprocessData.FDX.BankSvcRs.preprocessExternalTransfer.document.documentId._text,
            status: externalTransferData.FDX.BankSvcRs.externalTransfer.status._text
        };
    }
}

export const mtsProvider = new MtsPaymentProvider();
