import * as shell from "shelljs";
import yargs, { Options } from "yargs";

const red = "\x1b[31m";
const green = "\x1b[32m";
const reset = "\x1b[0m";

function releasePrepareAndPerform(): void {
  releasePrepare();
  releasePerformLocal();
}

function releasePrepare(argv?: any): void {
  shell.exec(
    'mvn -B release:prepare -DskipTests=true -Prelease -Darguments="-DskipTests=true -Prelease"',
  );
}

function releasePerformLocal(args?: any): void {
  const version = args.releaseVersion;
  const repo = args.repo;
  const localMavenRepo = args.localMavenRepo;
  const groupPath = args.groupPath;
  const artifactId = args.artifactId;

  shell.mkdir("-p", `${repo}/${groupPath}/${artifactId}/${version}`);
  shell.cp(
    `${localMavenRepo}/${groupPath}/${artifactId}/${version}/${artifactId}-${version}*`,
    `${repo}/${groupPath}/${artifactId}/${version}/`,
  );

  // Call createChecksums for each type
  ["", ".pom", ".jar", "-javadoc.jar", "-sources.jar"].forEach((classifier) => {
    createChecksums(
      classifier,
      version,
      repo,
      localMavenRepo,
      groupPath,
      artifactId,
    );
  });

  shell.rm("-rf", `${repo}/${groupPath}/${artifactId}/${version}/*main*`);
  shell.exec(`git -C ${repo} status`);
  shell.exec(`git -C ${repo} add .`);
  shell.exec(
    `git -C ${repo} commit -m "Release ${artifactId}-${version}" || echo "ignore commit failure, proceed"`,
  );
  shell.exec(`git -C ${repo} push`);
  shell.rm("-f", "pom.xml.releaseBackup", "release.properties");
  console.log(`${green}done${reset}`);
}

function normalizePom(): void {
  const cmd =
    'mvn com.github.ekryd.sortpom:sortpom-maven-plugin:sort -Dsort.encoding=UTF-8 -Dsort.sortDependencies=scope,artifactId -Dsort.sortPlugins=artifactId -Dsort.sortProperties=true -Dsort.sortExecutions=true -Dsort.sortDependencyExclusions=artifactId -Dsort.lineSeparator="\\n" -Dsort.ignoreLineSeparators=false -Dsort.expandEmptyElements=false -Dsort.nrOfIndentSpace=2 -Dsort.indentSchemaLocation=true';
  shell.echo("executing>", cmd);
  shell.exec(cmd);
}

function createChecksums(
  classifier: string,
  version: string,
  repo: string,
  localMavenRepo: string,
  groupPath: string,
  artifactId: string,
): void {
  let file = `${repo}/${groupPath}/${artifactId}/${version}/${artifactId}-${version}${classifier}`;
  shell.rm("-f", `${file}.sha1`);
  shell.exec(`sha1sum.exe ${file} | cut -d ' ' -f 1 > ${file}.sha1`);
  shell.rm("-f", `${file}.md5`);
  shell.exec(`md5sum.exe ${file} | cut -d ' ' -f 1 > ${file}.md5`);
}

function runTest(test: string = "LocationsTest"): void {
  shell.exec(`mvn -Dtest=${test} test`);
}

function release(argv?: any) {
  releasePrepare(argv);
  releasePerformLocal(argv);
}

const argsForRelease: { [key: string]: Options } = {
  repo: {
    type: "string",
    demandOption: true,
    describe:
      "Path to git repo with maven libraries like: d:/home/raiser/work/maven-repo",
  },
  //Normally this should not be nedeed
  localMavenRepo: {
    type: "string",
    demandOption: true,
    describe:
      "Path to git repo with maven libraries like: c:/Users/raiser/.m2/repository",
  },
  groupPath: {
    type: "string",
    demandOption: true,
    describe: "Maven groupPath like org/raisercostin",
  },
  artifactId: {
    type: "string",
    demandOption: true,
    describe: "Maven artifactId like jedio",
  },
  releaseVersion: {
    type: "string",
    demandOption: true,
    describe: "Maven released version like 0.72",
  },
};

const argv = yargs
  .scriptName("scripts")
  .command(
    "releasePrepareAndPerform",
    "Executes releasePrepare and releasePerformLocal",
    {},
    releasePrepareAndPerform,
  )
  .command("normalizePom", "Normalizes the POM file", {}, normalizePom)
  .command("release", "Prepares AND release", argsForRelease, release)
  .command("releasePrepare", "Prepares the release", {}, releasePrepare)
  .command(
    "releasePerformLocal",
    "Performs the release locally",
    argsForRelease,
    releasePerformLocal,
  )
  .demandCommand()
  .help()
  .alias("help", "h").argv;
