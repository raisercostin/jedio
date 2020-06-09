# jedio
Uniform, fluent access to files, urls and other resources API for java, kotlin and scala. 

[![Download](https://api.bintray.com/packages/raisercostin/maven/jedio/images/download.svg)](https://bintray.com/raisercostin/maven/jedio/_latestVersion)
[![Build Status](https://travis-ci.org/raisercostin/jedio.svg?branch=master)](https://travis-ci.org/raisercostin/jedio)
<!--
[![Codacy Badge](https://www.codacy.com/project/badge/fe1bb28a7735433d89a238ce6f6305c1)](https://www.codacy.com/app/raisercostin/jedio)
-->

## Usage

## ToDo
- [ ] async first library 
  - [ ] readContent should always do a readContentAsync.block to force reading async
  - [ ] readContentSync - forces reading in current thread
- [x] ignore ssl validation
- [ ] copy meta info
  - [ ] for UrlLocations entire header and other details with suffix: `.meta.json`, `-1.meta.json`, etc.
  - [ ] other copy operation could describe the source of file?
- [ ] detect if a specific error is thrown multiple times and stop
  - [ ] https - "javax.net.ssl.SSLHandshakeException: java.security.cert.CertificateException: No subject alternative DNS name matching revomatico.com found." 
- [ ] stop redirects in crawler - we will search for them in meta.json

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
