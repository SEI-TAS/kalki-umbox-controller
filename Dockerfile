#FROM i386/openjdk:8
FROM openjdk:8-alpine

# Install ovs-tools
RUN apk --no-cache add bash iproute2 openvswitch

# AlertServer is listening here.
EXPOSE 6060

ARG PROJECT_NAME=kalki-umbox-controller
ARG PROJECT_VERSION=1.4
ARG DIST_NAME=$PROJECT_NAME-$PROJECT_VERSION

COPY config.json /$PROJECT_NAME

COPY build/distributions/$DIST_NAME.tar /
RUN tar -xvf $DIST_NAME.tar
RUN rm $DIST_NAME.tar
RUN mv /$DIST_NAME /$PROJECT_NAME

WORKDIR /$PROJECT_NAME
ENTRYPOINT ["bash", "bin/kalki-umbox-controller"]
