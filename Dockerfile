FROM ubuntu:20.04

ARG BUILDER_UID=9999
ARG DEBIAN_FRONTEND=noninteractive

ENV TZ="Australia"
ENV JAVA_HOME /usr/lib/jvm/java-8-openjdk-amd64

RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    libnetcdf15 \
    libgsl23 \
    libudunits2-0 \
    libxml2-utils \
    openjdk-8-jdk \
    python3-dev \
    maven \
    wget \
    netcdf-bin \
    && rm -rf /var/lib/apt/lists/*

RUN update-alternatives --install /usr/bin/python python /usr/bin/python3 10
RUN update-java-alternatives -s java-1.8.0-openjdk-amd64

RUN wget -q https://bootstrap.pypa.io/pip/3.5/get-pip.py \
    && python get-pip.py pip==18.1 setuptools==49.6.0 wheel==0.35.1 \
    && rm -rf get-pip.py

RUN pip install \
    bump2version==1.0.1

RUN useradd --create-home --no-log-init --shell /bin/bash --uid $BUILDER_UID builder
USER builder
WORKDIR /home/builder
