/* eslint-disable camelcase, @typescript-eslint/camelcase */
import * as assert from 'assert'
import axios from 'axios'
import MockAdapter from 'axios-mock-adapter'
import { TvmClient } from '../code/tvm-client'

describe('TVMClient', () => {
  const mock = new MockAdapter(axios)
  mock.onGet('/checkusr', { headers: { 'X-Ya-User-Ticket': 'USR', Authorization: 'AUTH' } }).reply(200, {
    default_uid: 270127198,
    uids: [270127198],
    scopes: ['login:email', 'login:info', 'music:content', 'music:read', 'music:write', 'quasar:pay'],
    debug_string: 'ticket_type=user;expiration_time=1574267203;default_uid=270127198;uid=270127198;env=0',
    logging_string:
      '3:user:CPdTEMPS1e4FGmEKBgjeoOeAARDeoOeAARoLbG9naW46ZW1haWwaCmxvZ2luOmluZm8aDW11c2ljOmNvbnRlbnQaCm11c2ljOnJlYWQaC211c2ljOndyaXRlGgpxdWFzYXI6cGF5INyPeigA:',
  })
  mock
    .onGet('/checksrv', {
      params: { dst: 2016689 },
      headers: { 'X-Ya-Service-Ticket': 'SRV', Authorization: 'AUTH' },
    })
    .reply(200, {
      src: 2000860,
      dst: 2016689,
      scopes: null,
      debug_string: 'ticket_type=service;expiration_time=1574309593;src=2000860;dst=2016689;scope=;',
      logging_string: '3:serv:CIFUENmd2O4FIggI3I96ELGLew:',
      issuer_uid: null,
    })
  mock.onGet('/tickets', { params: { dsts: '2011346', src: 2016689 }, headers: { Authorization: 'AUTH' } }).reply(200, {
    mobapi: {
      ticket:
        '3:serv:CIFUEOuJ2O4FIggIsYt7ENLheg:B7zGeTXafr5JK0TRMKOENUIUJ50ynTaiFMEqDQ4jOGR1UYGukLm-_Qmwx1ZXe4A2y5OImY-LNSz2nQiVPgouBTXUJRMZQLCKRc8nlhFo1MdgmS7MwapHiK02EVlqSpu2UP3O0CWP14Ms-uDnCYw36FPuVyiUCTori1zm3RG9DBo__d5zJtdp9jOtDxBDOoMYO2hA6llkgwhYhUO16wbXfJgjz8Gq2bM18-phfHe3SSwgYwv87wTHxKaVckmqz-thaK6vkBknRNrEDnaDxDJKABk5WqYC0FBcdIuvCVfhMSJNTqgfxtFjyHccZ7rXaW1X0iXTNPV_iZ29SIB3TJCWSA',
      tvm_id: 2011346,
    },
  })

  const client = new TvmClient('http://localhost:2222', 'AUTH')

  it('should check user ticket', async () => {
    const response = await client.checkUser('USR')
    console.log(response.defaultUid)
  })

  it('should check service ticket', async () => {
    const response = await client.checkService(TvmClient.milesTvmId, 'SRV')
    assert.strictEqual(response.src, TvmClient.megamindTvmId)
    assert.strictEqual(response.dst, TvmClient.milesTvmId)
  })

  it('should get service ticket', async () => {
    const ticket = await client.getServiceTicket([TvmClient.mobileApiTvmId], TvmClient.milesTvmId)
    console.log(ticket)
  })
})
