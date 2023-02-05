// Генератор Таймстампов. В настоящее время генерирует даты где день двузначен
// Двузначность дня проверяется после перевода в локальное время по timeZone.
describe('timestamp generator', () => {
  it('should generate', (done) => {
    console.log('[')
    const start = new Date(2019, 11, 6, 1, 21)
    let printed = 0
    let currentDate = 0
    while (printed <= 100) {
      const d = new Date(start)
      d.setDate(d.getDate() - currentDate)
      let day = parseInt(d.toDateString().slice(8, 10), 10)
      while (day < 10) {
        d.setDate(d.getDate() - 1)
        day = parseInt(d.toDateString().slice(8, 10), 10)
        currentDate++
      }
      console.log(`'${d.toISOString()}',`)
      printed++
      currentDate++
    }
    console.log(']')
    done()
  })
})
