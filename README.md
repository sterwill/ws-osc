# ws-osc - A WebSocket to OSC Bridge

A Java server program with a Swing UI that accepts Websocket connections
and translates JSON into OSC and sends them to one or more UDP OSC endpoints.

This project was developed to translate messages from the 
[Affectiva](https://www.affectiva.com/) Javascript face recognition SDK to 
OSC messages for a live music performance.  Currently these are the only
message types that it can handle. 

It would be easy to extend the program to handle other types of inputs
and send other types of OSC outputs.

## Build Process

There's a Maven POM file, but full release builds aren't done
with Maven because I didn't figure out how to automate the whole
process of packaging for macOS.

Instead I use IntelliJ's `Build > Build Artifacts...` feature,
which writes to `out/artifacts` and then run `package-app.sh` to
build a macOS app.
