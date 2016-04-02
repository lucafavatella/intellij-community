#!/bin/sh
#
# ---------------------------------------------------------------------
# lanterna jar fetching script.
# ---------------------------------------------------------------------
#

getMavenJar() {
  test ! -e "${4:?}" || exit 1
  # Ref Maven [REST API](https://search.maven.org/#api)
  curl -o "${4:?}" "https://search.maven.org/remotecontent?filepath=${1:?}/${2:?}/${3:?}/$(jarName ${2:?} ${3:?})"
}

jarName() {
  echo "${1:?}-${2:?}.jar"
}

describeJar() {
  ls -l "${1:?}"
  file "${1:?}"
  jar tf "${1:?}" || true
}

V="${1:?}" # Lanterna version
TD="{2:?}" # Target directory

J=$(jarName "lanterna" "$V")
getMavenJar "com/googlecode/lanterna" "lanterna" "$V" "${TD}/${J}"
describeJar "${TD}/${J}"
