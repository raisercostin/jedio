#!/bin/bash

#TODO
# - if someone wants to override env variables defined in config he could define something like K8S_PROFILES_PATH_OVERRIDE. Search it here.

#args - https://stackoverflow.com/questions/192249/how-do-i-parse-command-line-arguments-in-bash
# saner programming env: these switches turn some bugs into errors
set -o errexit -o pipefail -o noclobber -o nounset
#set -o nounset [[ "${DEBUG}" == 'true' ]] && set -o xtrace 

readonly red=`tput setaf 1`
readonly green=`tput setaf 2`
readonly reset=`tput sgr0` 

function releasePrepare(){
  mvn release:prepare -DskipTests=true -Prelease -Darguments=\"-DskipTests=true -Prelease\"
}
function releasePerformLocal(){
  set -x
  local -r version=${1?Missing version like 0.72}
  local -r repo=${2:-d:/home/raiser/work/maven-repo}
  local -r localMavenRepo=${3:-c:/Users/raiser/.m2/repository}
  local -r groupPath=${4:-org/raisercostin}
  local -r artifactId=${5:-jedio}
  
  mkdir -p $repo/$groupPath/$artifactId/$version
  cp $localMavenRepo/$groupPath/$artifactId/$version/$artifactId-$version* $repo/$groupPath/$artifactId/$version/
  rm -rf $repo/$groupPath/$artifactId/$version/*main*
  git -C $repo status
  git -C $repo add .
  git -C $repo commit -m "Release $artifactId-$version"
  git -C $repo push
  set +x
  echo ${green}done${reset}
}

echo Commands
echo ---------
compgen -A function
echo ---------

command="$1"; shift 1;
echo Executing $command
echo ---------
$command $*