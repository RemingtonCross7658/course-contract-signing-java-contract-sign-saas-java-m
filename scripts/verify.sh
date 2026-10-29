#!/bin/sh
set -eu
classes="${TMPDIR:-/tmp}/course-contract-classes"
rm -rf "$classes"
mkdir -p "$classes"
javac -d "$classes" $(find src/main/java src/test/java -name '*.java')
java -ea -cp "$classes" edu.coursecontracts.domain.TenantSigningPolicyTest
