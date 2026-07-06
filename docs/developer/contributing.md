# Building & Contributing

CWS is open source under the [Apache License 2.0](https://github.com/NASA-AMMOS/common-workflow-service/blob/main/LICENSE).
Contributions are welcome.

## Building

See the [Installation → Building from Source](../install/building.md) guide for
the full build procedure, including the personal dev script, running tests, and
dependency checks.

## Contributing workflow

The full contribution guidelines are in
[CONTRIBUTING.md](https://github.com/NASA-AMMOS/common-workflow-service/blob/main/CONTRIBUTING.md).
The highlights:

1. **Fork** the repository and create a feature branch from `develop`.
2. **Make your changes** — follow existing coding patterns and conventions.
3. **Test** — run `./test.sh` to execute unit and integration tests.
4. **Open a pull request** against `develop` with a clear description of the
   change.

## Branching

| Branch | Purpose |
| --- | --- |
| `main` | Release branch — tagged releases are cut from here. |
| `develop` | Default development branch — PRs target this. |

## Code of conduct

All contributors are expected to follow the project's
[Code of Conduct](https://github.com/NASA-AMMOS/common-workflow-service/blob/main/CODE_OF_CONDUCT.md).

## Reporting issues

[Open an issue](https://github.com/NASA-AMMOS/common-workflow-service/issues/new/choose)
on GitHub for bugs, feature requests, or documentation gaps.
