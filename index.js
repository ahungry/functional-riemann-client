var p = require("process")
var fnrc = require("./lib/main.js")

console.log(fnrc.test_send)
// fnrc.foo().then(p.exit)

const sleep = () => {
  return new Promise((resolve, reject) => {
    setTimeout(resolve, 100)
  })
}

void async function main () {
  console.log('hi')
  var res = await fnrc.foo()
  var sock = await fnrc.get_socket()
  console.log(sock)
  console.log(res)
  for (let i = 0; i < 200; i++) {
    const x = fnrc.test_send(sock)
    // await sleep()
    console.log(x)
  }
  await sleep(10)
  console.log('all done')
  p.exit()
}()
