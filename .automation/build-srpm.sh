#!/bin/bash -xe

# Ensure the build validation passes
make validations

# git hash of current commit from GH action or from the HEAD commit
if [ "${GITHUB_SHA}" == "" ]; then
  GIT_HASH=$(git rev-parse --short HEAD)
else
  GIT_HASH=$(git rev-parse --short $GITHUB_SHA)
fi

# Prepare the RELEASE string (same work as patching `{?release_suffix}` in .copr/Makefile)
export RELEASE=".git${GIT_HASH}"
export MILESTONE=master

# Create RPM build directories
export top_dir="${PWD}/rpmbuild"
test -d "${top_dir}" && rm -rf "${top_dir}" || :
mkdir -p "${top_dir}/SOURCES"

# Get the tarball
make dist
mv *.tar.gz rpmbuild/SOURCES

# Create the src.rpm
rpmbuild \
    -D "_topdir ${top_dir}" \
    -D "release_suffix ${RELEASE}" \
    -ts rpmbuild/SOURCES/*.gz
