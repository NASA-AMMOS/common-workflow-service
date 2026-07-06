"""MkDocs-macros hook for the CWS documentation site.

Single-sources the CWS version so pages can reference ``{{ cws_version }}``
instead of hard-coding a release number. Resolution order:

1. ``CWS_VERSION`` environment variable (set by CI), then
2. the ``CWS_VER`` export in ``utils.sh`` at the repo root, then
3. ``"dev"`` as a last resort.

mkdocs runs with its working directory set to the folder containing
``mkdocs.yml`` (the repo root), so ``utils.sh`` is readable directly.
"""

import os
import re
import shutil

_UTILS_SH = "utils.sh"
_VER_RE = re.compile(r"""CWS_VER=['"]?([^'"\s#]+)""")

# Where the aggregated Javadoc is generated (maven-javadoc-plugin
# javadoc:aggregate default), overridable for CI. When present, it is copied
# into the built site under /javadoc so it ships with every mike version.
_JAVADOC_SRC = os.environ.get("CWS_JAVADOC_DIR", "target/site/apidocs")


def _version_from_utils():
    try:
        with open(_UTILS_SH, "r", encoding="utf-8") as fh:
            for line in fh:
                m = _VER_RE.search(line)
                if m:
                    return m.group(1)
    except OSError:
        pass
    return None


def define_env(env):
    version = os.environ.get("CWS_VERSION") or _version_from_utils() or "dev"
    env.variables["cws_version"] = version


def on_post_build(env):
    """Copy generated Javadoc into the built site under /javadoc.

    No-op when the Javadoc has not been generated (e.g. local doc-only builds
    without a JDK), so the site still builds cleanly. CI generates the Javadoc
    before building, so it is included in every published version.
    """
    if not os.path.isdir(_JAVADOC_SRC):
        return
    site_dir = env.conf["site_dir"]
    dest = os.path.join(site_dir, "javadoc")
    shutil.copytree(_JAVADOC_SRC, dest, dirs_exist_ok=True)
