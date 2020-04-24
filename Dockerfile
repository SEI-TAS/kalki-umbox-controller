#FROM i386/openjdk:8
FROM openjdk:8-alpine

# Install ovs-tools
RUN apk --no-cache add bash iproute2 openvswitch

ARG PROJECT_NAME=kalki-umbox-controller
ARG PROJECT_VERSION=1.4
ARG DIST_NAME=$PROJECT_NAME-$PROJECT_VERSION

# AlertServer is listening here.
EXPOSE 6060

COPY $DIST_NAME.tar /app/
WORKDIR /app
RUN tar -xvf $DIST_NAME.tar

COPY config.json /app/$DIST_NAME

WORKDIR /app/$DIST_NAME
ENTRYPOINT ["bash", "bin/kalki-umbox-controller"]
