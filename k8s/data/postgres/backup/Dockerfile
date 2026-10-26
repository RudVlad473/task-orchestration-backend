# Image for the nightly production database backup: pg_dump 16, age and the AWS CLI on a pinned Alpine.
#
# Decisions:
# - The pod that runs this image holds the production database password and the object-storage key, so the
#   bytes it executes are built once from this file and pinned by digest in the CronJob, instead of
#   downloading packages from a mirror at the start of every run.
# - The base is pinned by digest and each package by version. The versions come from the Alpine 3.22 index;
#   a point release can drop a pinned version, in which case the build fails loudly instead of drifting.
# - The backup script is not baked in. It reaches the pod as a ConfigMap, so a script edit rolls the CronJob
#   and never needs an image rebuild.
# - The uid is the conventional nobody user. The CronJob also sets it, so the manifest stays correct if this
#   line changes.
FROM alpine:3.22@sha256:5291449c3df73caf6ed85e649dec1b9e818b39a5d8c871e97afc13e9cd5e8fa8

RUN apk add --no-cache \
      age=1.2.1-r10 \
      aws-cli=2.27.25-r0 \
      postgresql16-client=16.15-r0

USER 65534:65534
