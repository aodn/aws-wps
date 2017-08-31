#################################################################
# Dockerfile to build image for running gridded data aggregations
#################################################################

FROM ubuntu:xenial
MAINTAINER AODN

# Install Java.
RUN \
  apt-get update && \
  apt-get install -y openjdk-8-jdk && \
  rm -rf /var/lib/apt/lists/*

# Install netcdf library
RUN \
  apt-get update && \
  apt-get install -y libnetcdf11 && \
  rm -rf /var/lib/apt/lists/*

ADD target/lib /opt/aggregator/lib
ADD target/*.jar /opt/aggregator/

ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/aggregator/*.jar"]


