#!/bin/sh
#
# ---------------------------------------------------------------------
# charva-lanterna jars building script.
# ---------------------------------------------------------------------
#

getCharvaLanternaGitTreeIsh() {
  test ! -e "${3:?}" || exit 1
  git clone "${1:?}" "${3:?}"
  ( cd "${3:?}" && git checkout "${2:?}" )
}

buildCharvaLanterna() {
  ( cd "${1:?}" && mvn clean package )
}

copyCharvaLanternaJar() {
  test ! -e "${3:?}/${1:?}.jar" || exit 1
  cp -p "${2:?}/${1:?}/target/${1:?}"-*.jar "${3:?}/${1:?}.jar"
}

describeJar() {
  ls -l "${1:?}"
  file "${1:?}"
  jar tf "${1:?}" || true
}

GU="${1:?}" # charva-lanterna git URL
GT="${2:?}" # charva-lanterna git treeish
WS="${3:?}" # Workspace
TD="${4:?}" # Target directory

getCharvaLanternaGitTreeIsh "$GU" "$GT" "$WS"
buildCharvaLanterna "$WS"
ls "$WS"/*/target/*.jar
for J in $(ls "$WS"/*/target/*.jar); do describeJar "$J"; done
for J in charva charva-showcase charva-lanterna; do copyCharvaLanternaJar "$J" "$WS" "$TD"; done
