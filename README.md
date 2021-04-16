# jedio
Uniform, fluent access to files, urls and other resources API for java, kotlin and scala. 

[![Download](https://api.bintray.com/packages/raisercostin/maven/jedio/images/download.svg)](https://bintray.com/raisercostin/maven/jedio/_latestVersion)
[![Build Status](https://travis-ci.org/raisercostin/jedio.svg?branch=master)](https://travis-ci.org/raisercostin/jedio)
<!--
[![Codacy Badge](https://www.codacy.com/project/badge/fe1bb28a7735433d89a238ce6f6305c1)](https://www.codacy.com/app/raisercostin/jedio)
-->

## Usage

## ToDo
- [ ] add deserializers
  - to specific implementations
  - to interfaces https://stackoverflow.com/questions/25387978/how-to-add-custom-deserializer-to-interface-using-jackson
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


- [ ] If quasar is needed the client should configure pom.xml to use quasar-agent
```
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-dependency-plugin</artifactId>
        <version>3.1.1</version>
        <executions>
          <execution>
            <id>one</id>
            <phase>prepare-package</phase>
            <goals>
              <goal>copy</goal>
            </goals>
            <configuration>
              <artifactItems>
                <artifactItem>
                  <groupId>co.paralleluniverse</groupId>
                  <artifactId>quasar-core</artifactId>
                  <classifier>jdk8</classifier>
                  <outputDirectory>${project.build.directory}</outputDirectory>
                  <destFileName>quasar-agent.jar</destFileName>
                </artifactItem>
              </artifactItems>
            </configuration>
          </execution>
          <execution>
            <id>second</id>
            <phase>prepare-package</phase>
            <goals>
              <goal>copy-dependencies</goal>
            </goals>
          </execution>
        </executions>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>2.22.1</version>
        <configuration>
          <argLine>-javaagent:${co.paralleluniverse:quasar-core:jar}</argLine>
          <useSystemClassLoader>false</useSystemClassLoader>
        </configuration>
      </plugin>
```
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
