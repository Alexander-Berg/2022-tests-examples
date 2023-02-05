import * as http from 'http'
import { IncomingMessage, ServerResponse } from 'http'

export class MockServer {
  private server = http.createServer((req: IncomingMessage, res: ServerResponse) => {
    console.log(`Mocking request ${req.url} with response ${this.response}`)
    res.write(this.response)
    res.end()
  })

  constructor(private response: string) {}

  start(port: number) {
    this.server.listen(port)
  }

  stop() {
    this.server.close()
  }
}
