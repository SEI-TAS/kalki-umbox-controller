FROM i386/openjdk:8

# Install Python 2.7, pip and pipenv
RUN apt-get update \
&& apt-get -yqq install python python-pip
RUN pip install pipenv

# Install Libvirt (dev)
RUN apt-get -yqq install libvirt-dev

# Install ovs-tools
RUN apt-get -yqq install openvswitch-common openvswitch-switch

ENV PROJECT_NAME umbox_controller
ENV DIST_NAME $PROJECT_NAME-1.0-SNAPSHOT

# AlertServer is listening here.
EXPOSE 6060

COPY $DIST_NAME.tar /app/
WORKDIR /app
RUN tar -xvf $DIST_NAME.tar

COPY config.json /app/$DIST_NAME
COPY vm-umbox-tool/ /app/$DIST_NAME/vm-umbox-tool/
COPY tests/ /app/$DIST_NAME/tests/

# Setup pipenv for VM Umbox tool
ENV PIPENV_VENV_IN_PROJECT "enabled"
WORKDIR /app/$DIST_NAME/vm-umbox-tool
RUN pipenv install

WORKDIR /app/$DIST_NAME
ENTRYPOINT ["bash", "bin/umbox_controller"]
