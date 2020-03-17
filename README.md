# jedio
Uniform, fluent access to files, urls and other resources API for java, kotlin and scala. 

[![Download](https://api.bintray.com/packages/raisercostin/maven/jedio/images/download.svg)](https://bintray.com/raisercostin/maven/jedio/_latestVersion)
[![Build Status](https://travis-ci.org/raisercostin/jedio.svg?branch=master)](https://travis-ci.org/raisercostin/jedio)
<!--
[![Codacy Badge](https://www.codacy.com/project/badge/fe1bb28a7735433d89a238ce6f6305c1)](https://www.codacy.com/app/raisercostin/jedio)
-->

## Usage

### Maven

#### Dependency

```
<dependency>
  <groupId>org.raisercostin</groupId>
  <artifactId>jedio</artifactId>
  <version>0.1</version>
</dependency>
```

#### Repository
```
<repository>
  <id>raisercostin-bintray</id>
  <url>https://dl.bintray.com/raisercostin/maven</url>
  <releases><enabled>true</enabled></releases>
  <snapshots><enabled>false</enabled></snapshots>
</repository>
```

## Other libraries
- scala - https://ammonite.io/#Ammonite-Ops
- https://golb.hplar.ch/2017/09/Pluggable-file-systems-in-Java.html

## Development

- To release `mvn release:prepare release:perform -DskipTests=true -Prelease -Darguments="-DskipTests=true -Prelease"`
