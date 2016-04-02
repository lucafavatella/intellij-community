#!/bin/sh
#
# ---------------------------------------------------------------------
# java to charva conversion script.
# ---------------------------------------------------------------------
#

java2charva() {
  find "${1:?}" -type f -name '*.java' -print0 | xargs -0 perl -pi -e 's/java.awt./charva.awt./g;' -e 's/javax.swing./charvax.swing./g;'
}

R="${1:?}" # Root directory to convert from java to charva

java2charva "$R"
