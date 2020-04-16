FROM python:2.7

RUN apt-get update

# Install pipenv
RUN pip install pipenv

# Install docker tools
RUN apt-get install -yqq lsb-release
RUN apt-get install -yqq apt-transport-https ca-certificates curl gnupg-agent software-properties-common
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -
RUN add-apt-repository "deb [arch=$(dpkg --print-architecture)] https://download.docker.com/linux/debian $(lsb_release -cs) stable"
RUN apt-get update
RUN apt-get install -yqq docker-ce-cli

# Instal ovs tools
RUN apt-get -yqq install openvswitch-common openvswitch-switch

# ovs-docker-server is listening here.
EXPOSE 5500

# Installing Python deps without a venv (not needed in container).
COPY Pipfile /app/
COPY Pipfile.lock /app/
WORKDIR /app
RUN pipenv install --system --deploy --ignore-pipfile

# Copy the actual code
COPY run.sh /app/
COPY docker_api.py /app/
COPY ovs-scripts/ovs-docker.sh /app/ovs-scripts/ovs-docker.sh

# Calling directly.
ENTRYPOINT ["bash", "run.sh"]
