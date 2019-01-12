#!/bin/sh
#
# apt install icnsutils

mkdir -p package/macosx
png2icns package/macosx/arch.icns arch.svg.png
cp -r out/artifacts/ws_osc_jar/* package/macosx
javapackager -deploy -native dmg -srcdir out/artifacts/ws_osc_jar -appclass com.tinfig.wsosc.Main -name "WebSocket to OSC" -outdir deploy -outfile WsOsc -v
