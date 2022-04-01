# jedio
Uniform, fluent access to files, urls and other resources API for java, kotlin and scala. 

[![Versions](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fraw.githubusercontent.com%2Fraisercostin%2Fmaven-repo%2Fmaster%2Forg%2Fraisercostin%2Fjedio%2Fmaven-metadata.xml)](https://github.com/raisercostin/maven-repo/tree/master/org/raisercostin/jedio)
[![Build Status](https://travis-ci.org/raisercostin/jedio.svg?branch=master)](https://travis-ci.org/raisercostin/jedio)
<!--
[![Codacy Badge](https://www.codacy.com/project/badge/fe1bb28a7735433d89a238ce6f6305c1)](https://www.codacy.com/app/raisercostin/jedio)
-->

## Usage

## ToDo
- [ ] check https://codehaus-plexus.github.io/plexus-utils/apidocs/org/codehaus/plexus/util/DirectoryScanner.html
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


## Quasar/Comsat/Fibers config
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
    <!-- <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <version>3.1.0</version>
        <configuration>
          <archive>
            <manifestEntries>
              <Premain-Class>co.paralleluniverse.fibers.instrument.JavaAgent</Premain-Class>
              <Agent-Class>co.paralleluniverse.fibers.instrument.JavaAgent</Agent-Class>
              <Can-Redefine-Classes>true</Can-Redefine-Classes>
              <Can-Retransform-Classes>true</Can-Retransform-Classes>
            </manifestEntries>
          </archive>
        </configuration>
      </plugin> -->
```
  - This will run using the quasar agent (with it's own dependencies)
    ```
    mvn install -DskipTests
    java -javaagent:target/dependency/quasar-core-0.7.10-jdk8.jar -Xbootclasspath/a:target/dependency/kryo-serializers-0.42.jar;target/dependency/kryo-4.0.0.jar;target/dependency/guava-26.0-jre.jar -Dco.paralleluniverse.fibers.verifyInstrumentation=true -Dco.paralleluniverse.fibers.detectRunawayFibers=false -jar <yourapp>.jar --server.port=8080
    ```
  - macOS run command:
    ```
    java -javaagent:target/dependency/quasar-core-0.7.10-jdk8.jar -Xbootclasspath/a:target/dependency/kryo-serializers-0.42.jar:target/dependency/kryo-4.0.0.jar:target/dependency/guava-26.0-jre.jar -Dco.paralleluniverse.fibers.detectRunawayFibers=false -jar <yourapp>.jar --server.port=8080 
    ```
### Configure eclipse with quasar
  Add quasar agent
   In 'Eclipse -> Preferences -> Java -> Intalled JREs -> edit your jre -> Default VM arguments' add this:
   (or after maven copies them in target/dependency:
   `-javaagent:target/dependency/quasar-core-0.7.10-jdk8.jar -Xbootclasspath/a:target/dependency/kryo-serializers-0.42.jar;target/dependency/kryo-4.0.0.jar;target/dependency/guava-26.0-jre.jar -Dco.paralleluniverse.fibers.verifyInstrumentation=true`
   )
   old: `-javaagent:C:\Users\$USERNAME\.m2\repository\co\paralleluniverse\quasar-core\0.7.10\quasar-core-0.7.10-jdk8.jar`
- Fibers are good in blocking threards waiting for IO. If CPU is used too much errors are sent to System.error. In production you can add the following setting to disable it
  `-Dco.paralleluniverse.fibers.detectRunawayFibers=false`
  For more details see https://github.com/puniverse/quasar/blob/master/quasar-core/src/main/java/co/paralleluniverse/fibers/FiberTimedScheduler.java
- To disable CPU complain in fibers configure system properties to with `-Dco.paralleluniverse.fibers.detectRunawayFibers=false`
      
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
  <id>raisercostin-github</id>
  <url>https://raw.githubusercontent.com/raisercostin/maven-repo/master/</url>
  <snapshots><enabled>false</enabled></snapshots>
</repository>
```

## Other libraries
- scala - https://ammonite.io/#Ammonite-Ops
- https://golb.hplar.ch/2017/09/Pluggable-file-systems-in-Java.html

## Development

- To release 
  - `npm run normalize-pom release-prepare release-perform-local`
- Deprecated
  - `mvn release:prepare release:perform -DskipTests=true -Prelease -Darguments="-DskipTests=true -Prelease"`
