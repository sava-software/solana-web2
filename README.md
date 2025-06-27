![](https://github.com/sava-software/sava/blob/003cf88b3cd2a05279027557f23f7698662d2999/assets/images/solana_java_cup.svg)

# Solana Web2 [![Gradle Check](https://github.com/sava-software/solana-web2/actions/workflows/build.yml/badge.svg)](https://github.com/sava-software/solana-web2/actions/workflows/build.yml) [![Publish Release](https://github.com/sava-software/solana-web2/actions/workflows/publish.yml/badge.svg)](https://github.com/sava-software/solana-web2/actions/workflows/publish.yml)

## Documentation

User documentation lives at [sava.software](https://sava.software/).

* [Dependency Configuration](https://sava.software/quickstart)
* [Web 2.0](https://sava.software/libraries/web2)

## Contribution

Unit tests are needed and welcomed. Otherwise, please open a discussion, issue, or send an email before working on a
pull request.

## Build

[Generate a classic token](https://github.com/settings/tokens) with the `read:packages` scope needed to access
dependencies hosted on GitHub Package Repository.

#### ~/.gradle/gradle.properties

```properties
savaGithubPackagesUsername=GITHUB_USERNAME
savaGithubPackagesPassword=GITHUB_TOKEN
```

```shell
./gradlew check
```
