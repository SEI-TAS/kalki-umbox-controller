FROM python:3.8

# Install pipenv
RUN pip install pipenv

# Install ovs switch tools and docker dependencies
RUN apt-get update && apt-get install -y \
    openvswitch-common openvswitch-switch \
    lsb-release apt-transport-https ca-certificates curl gnupg-agent software-properties-common

# Install docker.
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add - && \
    add-apt-repository "deb [arch=$(dpkg --print-architecture)] https://download.docker.com/linux/debian $(lsb_release -cs) stable" && \
    apt-get update && apt-get install -y docker-ce-cli

# Remove docker installation dependencies.
RUN apt-get purge -y lsb-release apt-transport-https ca-certificates curl gnupg-agent software-properties-common && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*

# ovs-docker-server is listening here.
EXPOSE 5500

# Installing Python deps without a venv (not needed in container).
COPY Pipfile /app/
COPY Pipfile.lock /app/
WORKDIR /app
RUN pipenv install --system --deploy --ignore-pipfile

# Copy the actual code and helped scripts
COPY ovs-scripts/ovs-docker.sh /app/ovs-scripts/ovs-docker.sh
COPY run.sh /app/
COPY docker_api.py /app/

# Calling directly.
ENTRYPOINT ["bash", "run.sh"]
