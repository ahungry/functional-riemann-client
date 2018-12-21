# Functional Riemann Client

See http://riemann.io

Soon will have support for:

- npm / nodejs
- clojurescript
- clojure

Currently sample message sends are benchmarking at a rate of around
10000 / second on my Lenovo Thinkpad W530 when running the compiled
clojurescript via a nodejs process.

Next up will add robust connection establishment, so users never have
to worry about connecting/disconnecting from TCP/UDP, just define a
config object and give it to your 'send' method and it will figure out
the best way to pipe events to Riemann.

What I notice is missing in a few other implementations is batching
events in any meaningful way (they simply send one Msg per Event, even
though a Msg can support many Events).
