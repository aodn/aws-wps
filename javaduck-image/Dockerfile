#################################################################
# Dockerfile to build image for running gridded data aggregations
#################################################################

# Amazon linux required to use ECS role to control AWS access
FROM amazonlinux:latest  
MAINTAINER AODN

# Install ecs agent
RUN yum install -y ecs-init && yum clean all

# Install Java.
RUN yum -y install java-1.8.0 && yum clean all

# Install netcdf library - no prebuilt packages for amazon linux so need to build from scratch
RUN \
  yum -y install gcc gcc-c++ m4 diffutils findutils && yum clean all && \  
  mkdir /software && \
  cd /software && \
  curl -L https://github.com/Unidata/netcdf-c/archive/v4.4.1.1.tar.gz -o v4.4.1.1.tar.gz && \
  tar xvf v4.4.1.1.tar.gz && \
  curl -L http://www.zlib.net/zlib-1.2.11.tar.gz -o zlib-1.2.11.tar.gz && \
  tar xvf zlib-1.2.11.tar.gz && \
  curl -L https://support.hdfgroup.org/ftp/HDF5/current18/src/hdf5-1.8.18.tar -o hdf5-1.8.18.tar && \
  tar xf hdf5-1.8.18.tar && \
  ZDIR=/usr/local && \
  cd /software/zlib-1.2.11 && \
  ./configure --prefix=${ZDIR} && \
  make install && \
  cd /software/hdf5-1.8.18 && \
  H5DIR=/usr/local && \
  ./configure --with-zlib=${ZDIR} --prefix=${H5DIR} && \
  make install && \
  cd /software/netcdf-c-4.4.1.1 && \
  NCDIR=/usr/local && \
  CPPFLAGS=-I${H5DIR}/include LDFLAGS=-L${H5DIR}/lib ./configure --prefix=${NCDIR} && \
  make install && \
  rm -rf /software

# copy aggregator to image
COPY bin/* /opt/aggregator/
RUN chmod -R 755 /opt/aggregator/aggregate.sh

ENTRYPOINT ["/opt/aggregator/aggregate.sh"]


