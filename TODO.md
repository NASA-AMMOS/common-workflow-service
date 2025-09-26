TODO List:

- Add an AI component that verifies from a previous "baseline" run logs (artemis.log, cws.log, catalina.out, etc) and compares that to the newest run and discribes the differences.  This can help track down any potential new errors introduced.  Note for it to also find anything that is missing from the baseline logs.
- Enable the OSS Index analyzer in the Dependency-Check to catch more potential security risks
  - Requires to authenticate (create a new account on ossindex.sonatype.org)
