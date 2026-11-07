#!/bin/sh
# Skiko unpacks its native library into ~/.skiko by default, which the sandbox can't write.
exec /app/jre/bin/java \
    -Dcompose.application.configure.swing.globals=true \
    -Dskiko.data.path="${XDG_CACHE_HOME:-$HOME/.cache}/skiko" \
    -jar /app/lib/metrolist/metrolist.jar "$@"
