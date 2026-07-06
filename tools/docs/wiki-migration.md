# Wiki Migration & Sanitization Procedure

This document defines the repeatable process for migrating CWS documentation
from the internal JPL Confluence wiki (the `cws` space) into this public
documentation site. **Every imported page must pass through all four steps
below before it is committed.**

The wiki is used as *supplemental* material: the code-verified in-repo sources
(`README.md`, module READMEs, config templates, source annotations) are the
source of truth. Wiki content fills gaps and adds narrative, but any claim it
makes is verified against the current codebase before publishing.

## Scope

- **Migrate:** the *living* (non-version-suffixed) pages of the `cws` space —
  User's Guide, CWS Web Console pages, Modeling, Initiators, Task Types,
  Adaptation, Administration, Install, Docker, REST API, Upgrade/Migration.
- **Ignore:** the per-release `(vX.Y)` snapshot trees (mike versioning replaces
  that pattern) and pre-2016 working-group meeting notes.

## The Four Steps

### 1. Import

Use the `jpl-wiki` skill to pull a page as Markdown:

```bash
python3 scripts/get_page.py --id <PAGE_ID> --save-to /tmp/wiki-<id>.md
```

Place the converted Markdown at the matching path under `docs/` per the site
information architecture (see `mkdocs.yml` `nav`).

### 2. Verify against code

Correct anything stale before it is published. Common drift to check:

| Claim in wiki | Verify against |
| --- | --- |
| CWS version (wiki is anchored at v2.6) | `utils.sh` `CWS_VER`, root `pom.xml` |
| Camunda / Spring / Java versions | `dependency-compatibility.md`, `pom.xml` |
| Configuration keys & defaults | `install/example-cws-configuration.properties`, `installerPresets.properties` |
| REST endpoint paths / params | `cws-service` `RestService.java` annotations |
| Built-in task types | `cws-tasks` module source |
| Console page behavior | `install/cws-ui/*.ftl` |
| Tomcat / paths / ports | `install/` config, `utils.sh` |

### 3. Sanitize (JPL denylist)

Strip or genericize all JPL-internal content. This is a **hard requirement**;
the `sanitize-lint.sh` guard fails CI on any of these:

- Internal hostnames / the `*.jpl.nasa.gov` domain and `cae-*` hosts
- The **CAM** system, **MOZART**/**PGE** examples
- Internal ticket IDs (`IDS-####`), developer-initial scripts, internal email
  lists, internal Artifactory URLs
- **The entire AWS subtree** — IAM roles, security groups, AMI IDs, account
  numbers, VPC/subnet specifics, ARNs. Generalize to vendor-neutral guidance or
  omit the page entirely. Do **not** publish JPL-account-specific steps.
- Present JPL-local defaults (e.g. the `America/Los_Angeles` timezone) as
  *configurable examples*, never as requirements.

Keep the public Maven coordinates (`gov.nasa.jpl.ammos.ids.cws`) and Java
package names (`jpl.cws.*`) where they are genuine code references — these are
part of the open-source artifact, not internal data.

Run the guard locally before committing:

```bash
tools/docs/sanitize-lint.sh docs
```

### 4. Review

A human reviews every wiki-imported page before merge — both for residual
JPL-internal content the denylist might miss and for technical accuracy.

## Suggested migration order

1. User Guide (console pages, launching/scheduling)
2. Modeling (BPMN examples, best practices, script recipes)
3. Initiators (file, cron, message arrival, repeating delay, internal/external)
4. Developer & Adaptation
5. Administration
6. Deployment (Docker; AWS generalized last, given the sanitization load)
