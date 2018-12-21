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
  console.log(res)
  for (let i = 0; i < 20; i++) {
    const x = await fnrc.test_send()
    await sleep()
    console.log(x)
  }
  console.log('all done')
  p.exit()
}()
