#!/bin/sh
set -eu
classes="${TMPDIR:-/tmp}/creator-digest-classes"
mkdir -p "$classes"
javac -d "$classes" $(find src/main/java -name '*.java') $(find src/test/java -name '*.java')
java -cp "$classes" dev.learningdigest.CreatorDigestServiceTest
java -cp "$classes" dev.learningdigest.DigestApplication
