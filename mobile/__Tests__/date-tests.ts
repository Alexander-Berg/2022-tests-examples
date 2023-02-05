const dateFormat = require('dateformat') // eslint-disable-line

describe('Should parse date', () => {
  it('Parse date', (done) => {
    console.log(Date.parse('2019-12-05T22:21:00.000Z'))
    console.log(dateFormat('2019-12-05T22:21:00.000Z', 'ddd, dd mmm yyyy HH:MM:ss'))
    // Wed, 17 Jul 1996 02:23:25
    done()
  })
})
