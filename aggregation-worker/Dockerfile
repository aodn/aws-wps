#################################################################
# Dockerfile to build image for running gridded data aggregations
#################################################################

FROM ubuntu:20.04
MAINTAINER AODN

# Install Java.
RUN \
  apt-get update && \
  apt-get install -y openjdk-11-jdk \
  libnetcdf15 \
  && rm -rf /var/lib/apt/lists/*

ADD target/lib /opt/aggregator/lib
ADD target/*.jar /opt/aggregator/

ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/aggregator/aggregation-worker.jar"]


