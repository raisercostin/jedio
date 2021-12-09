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

function releasePrepareAndPerform(){
  releasePrepare
  releasePerformLocal
}

function releasePrepare(){
  mvn -B release:prepare -DskipTests=true -Prelease -Darguments="-DskipTests=true -Prelease"
}
function releasePerformLocal(){
  local -r version=${1?Missing version like 0.72}
  local -r repo=${2:-d:/home/raiser/work/maven-repo}
  local -r localMavenRepo=${3:-c:/Users/raiser/.m2/repository}
  local -r groupPath=${4:-org/raisercostin}
  local -r artifactId=${5:-jedio}
  
  mkdir -p $repo/$groupPath/$artifactId/$version
  cp $localMavenRepo/$groupPath/$artifactId/$version/$artifactId-$version* $repo/$groupPath/$artifactId/$version/
  rm -f $repo/$groupPath/$artifactId/$version/$artifactId-$version.pom.sha1
  sha1sum.exe $repo/$groupPath/$artifactId/$version/$artifactId-$version.pom | cut -d ' ' -f 1 > $repo/$groupPath/$artifactId/$version/$artifactId-$version.pom.sha1
  rm -f $repo/$groupPath/$artifactId/$version/$artifactId-$version.pom.md5
  md5sum.exe $repo/$groupPath/$artifactId/$version/$artifactId-$version.pom | cut -d ' ' -f 1 > $repo/$groupPath/$artifactId/$version/$artifactId-$version.pom.md5
  rm -rf $repo/$groupPath/$artifactId/$version/*main*
  git -C $repo status
  git -C $repo add .
  git -C $repo commit -m "Release $artifactId-$version" || echo "ignore commit failure, proceed"
  git -C $repo push
  rm -f pom.xml.releaseBackup
  rm -f release.properties
  echo ${green}done${reset}
}
function normalizePom(){
  mvn com.github.ekryd.sortpom:sortpom-maven-plugin:sort -Dsort.encoding=UTF-8 -Dsort.sortDependencies=scope,artifactId -Dsort.sortPlugins=artifactId -Dsort.sortProperties=true \
    -Dsort.sortExecutions=true -Dsort.sortDependencyExclusions=artifactId -Dsort.lineSeparator="\n" -Dsort.ignoreLineSeparators=false -Dsort.expandEmptyElements=false \
    -Dsort.nrOfIndentSpace=2 -Dsort.indentSchemaLocation=true
}
function createPomChecksums(){
  local -r version=${1?Missing version like 0.72}
  local -r repo=${2:-d:/home/raiser/work/maven-repo}
  local -r localMavenRepo=${3:-c:/Users/raiser/.m2/repository}
  local -r groupPath=${4:-org/raisercostin}
  local -r artifactId=${5:-jedio}
  rm -f $repo/$groupPath/$artifactId/$version/$artifactId-$version.pom.sha1
  sha1sum.exe $repo/$groupPath/$artifactId/$version/$artifactId-$version.pom | cut -d ' ' -f 1 > $repo/$groupPath/$artifactId/$version/$artifactId-$version.pom.sha1
  rm -f $repo/$groupPath/$artifactId/$version/$artifactId-$version.pom.md5
  md5sum.exe $repo/$groupPath/$artifactId/$version/$artifactId-$version.pom | cut -d ' ' -f 1 > $repo/$groupPath/$artifactId/$version/$artifactId-$version.pom.md5
}
function runTest(){
  local -r test=${1:-LocationsTest}
  mvn -Dtest=$test test
}

echo Commands
echo ---------
compgen -A function
echo ---------

command="$1"; shift 1;
echo Executing $command
echo ---------
set -x
$command $*
set +x
