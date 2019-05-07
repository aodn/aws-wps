#!/bin/bash
set -eu

if grep -q '/docker/' /proc/1/cgroup; then
    echo "INFO: detected that we are inside a Docker container, skipping 'docker build'"
    exit 0
else
    echo "INFO: detected that we are *not* inside a Docker container, running 'docker build'"
    docker build -t javaduck .
fi
