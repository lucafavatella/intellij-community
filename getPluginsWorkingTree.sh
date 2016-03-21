#!/bin/sh
#
# ---------------------------------------------------------------------
# plugin working trees fetching script.
# ---------------------------------------------------------------------
#

getWorkingTreeFromGitRepo() {
  test ! -e "${3:?}" || exit 1
  git clone -b "${2:?}" --depth 1 "${1:?}" "${3:?}"
  rm -rf "${3:?}"/.git
}

T="${1:?}" # Git tag or branch e.g. branch `master`, tag `idea/146.975`.

getWorkingTreeFromGitRepo git://git.jetbrains.org/idea/android.git "$T" android
getWorkingTreeFromGitRepo git://git.jetbrains.org/idea/adt-tools-base.git "$T" android/tools-base
