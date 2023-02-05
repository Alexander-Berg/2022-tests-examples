import { Router } from '../code/core/router'
import { MockServer } from './mock-server'
import { PublicConfigurationsProvider } from '../code/configurations/registry'

const chai = require('chai')
chai.use(require('chai-http'))
const expect = chai.expect

describe('X-Proxy acceptance tests before auto deploy', () => {
  const proxyPort = 9999
  const localhost = `http://localhost:${proxyPort}`
  const mockServer = new MockServer('{}')
  const mockServer2 = new MockServer('pizza')
  const mockServerPort = 3000
  const mockServerPort2 = 3001
  const router = new Router({
    defaultForwardHost: `http://localhost:${mockServerPort}`,
    pathConfigurations: {
      '/custom_prefix': {
        forwardHost: `http://localhost:${mockServerPort2}`,
        configurationName: 'random500',
        parameters: ['-1'],
      },
    },
  })

  beforeEach(() => {
    mockServer.start(mockServerPort)
    mockServer2.start(mockServerPort2)
    router.start(proxyPort)
  })

  it('should work without configuration', () => {
    return chai
      .request(localhost)
      .get('/api')
      .set('Authorization', 'olala')
      .set('Cookie', 'abyrvalg')
      .then((res) => {
        expect(res.status).to.eql(200)
      })
  })

  const blacklisted = ['random500', 'rps', 'infinite', 'response500', 'image', 'periodicallyInfiniteResponse']
  PublicConfigurationsProvider.allConfigurations().forEach((configurationName) => {
    if (blacklisted.includes(configurationName)) {
      return
    }
    it(`${configurationName}: configuration should work on mock backend`, () => {
      return chai
        .request(localhost)
        .get(`/c/${configurationName}`)
        .then((res) => {
          expect(res.status).to.eql(200)
        })
    })
  })

  it('pizza: should return 500 if no configuration with such name', () => {
    return chai
      .request(localhost)
      .get('/c/pizza')
      .then((res) => {
        expect(res.status).to.eql(500)
      })
  })

  it('random500: should return 500 if 100% fail probability', () => {
    return chai
      .request(localhost)
      .get('/c/random500/100')
      .then((res) => {
        expect(res.status).to.eql(500)
      })
  })

  it('random500: should return 200 if 0% fail probability', () => {
    return chai
      .request(localhost)
      .get('/c/random500/-1')
      .then((res) => {
        expect(res.status).to.eql(200)
      })
  })

  it('random500: should return 500 if invalid fail probability', () => {
    return chai
      .request(localhost)
      .get('/c/random500/abyrvalg')
      .then((res) => {
        expect(res.status).to.eql(500)
      })
  })

  it('readonly: should return 503 if post request', () => {
    return chai
      .request(localhost)
      .post('/c/readonly')
      .then((res) => {
        expect(res.status).to.eql(503)
      })
  })

  it('rps: should return 429 if too much requests', () => {
    const path = '/c/rps/1'
    return chai
      .request(localhost)
      .post(path)
      .then((res1) => {
        expect(res1.status).to.eql(200)
        chai
          .request(localhost)
          .post(path)
          .then((res2) => {
            expect(res2.status).to.eql(429)
          })
      })
  })

  it('infinite: should not response', (done) => {
    setTimeout(() => done(), 1500)
    chai
      .request(localhost)
      .post('/c/infinite')
      .then(() => done(new Error('Received response')))
  })

  it('response500: should return 500 on selected handler', () => {
    return chai
      .request(localhost)
      .post('/c/response500/api@mobile@v1@settings/api/mobile/v1/settings')
      .then((res) => {
        expect(res.status).to.eql(500)
      })
  })

  it('response500: should return 200 on other handler', () => {
    return chai
      .request(localhost)
      .post('/c/response500/api@mobile@v1@settings/api/mobile/v1/search')
      .then((res) => {
        expect(res.status).to.eql(200)
      })
  })

  it('should use custom configuration by custom prefix in config', () => {
    return chai
      .request(localhost)
      .post('/custom_prefix')
      .then((res) => {
        expect(res.text).to.eql('pizza')
      })
  })

  it('should work with default configuration after configuration with custom prefix', () => {
    return chai
      .request(localhost)
      .post('/custom_prefix')
      .then((res) => {
        expect(res.text).to.eql('pizza')
        return chai
          .request(localhost)
          .post('/pizza')
          .then((res) => {
            expect(res.text).to.eql('{}')
          })
      })
  })

  afterEach(() => {
    mockServer.stop()
    mockServer2.stop()
    router.stop()
  })
})
