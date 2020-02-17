FROM i386/openjdk:8

# Install ovs-tools
RUN apt-get update
RUN apt-get -yqq install openvswitch-common openvswitch-switch

ENV PROJECT_NAME umbox_controller
ENV DIST_NAME $PROJECT_NAME-1.0-SNAPSHOT

# AlertServer is listening here.
EXPOSE 6060

COPY $DIST_NAME.tar /app/
WORKDIR /app
RUN tar -xvf $DIST_NAME.tar

COPY config.json /app/$DIST_NAME

WORKDIR /app/$DIST_NAME
ENTRYPOINT ["bash", "bin/umbox_controller"]
