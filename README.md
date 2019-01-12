# ws-osc - A WebSocket to OSC Bridge

A Java server program with a Swing UI that accepts Websocket connections
and translates JSON into OSC and sends them to one or more UDP OSC endpoints.

This project was developed to translate messages from the 
[Affectiva](https://www.affectiva.com/) Javascript face recognition SDK to 
OSC messages for a live music performance.  Currently these are the only
message types that it can handle. 

It would be easy to extend the program to handle other types of inputs
and send other types of OSC outputs.