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

# copy aggregator to image
COPY bin/* /opt/aggregator/
RUN chmod -R 755 /opt/aggregator/aggregate.sh

ENTRYPOINT ["/opt/aggregator/aggregate.sh"]


