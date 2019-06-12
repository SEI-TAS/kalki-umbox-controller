FROM openjdk:8

# Install Python 2.7, pip and pipenv
RUN apt update \
&& apt -yqq install python python-pip \
&& pip install pipenv

# Install Libvirt (dev)
RUN apt -yqq install libvirt-dev

# Install ovs-tools
RUN apt -yqq install openvswitch-common openvswitch-switch

ENV PROJECT_NAME dni
ENV DIST_NAME $PROJECT_NAME-1.0-SNAPSHOT

# AlertServer is listening here.
EXPOSE 6060

COPY $DIST_NAME.tar /app/
WORKDIR /app
RUN tar -xvf $DIST_NAME.tar

COPY config.json /app/$DIST_NAME

# Setup pipenv for VM Umbox tool
WORKDIR /app/$DIST_NAME/vm-umbox-tool
RUN pipenv install

WORKDIR /app/$DIST_NAME
CMD ["bash", "bin/dni"]
