FROM python:3.11-alpine

# Trusted host configs used to avoid issues when running behind SSL proxies.
RUN pip config set global.trusted-host "pypi.org pypi.python.org files.pythonhosted.org"
RUN sed -ie "s/https/http/g" /etc/apk/repositories

# Install ovs switch tools and docker dependencies
RUN apk --no-cache add bash iproute2 util-linux openvswitch docker

# ovs-docker-server is listening here.
EXPOSE 5500

# Installing Python deps without a venv (not needed in container).
COPY requirements.txt /app/
WORKDIR /app
RUN pip install -r requirements.txt --default-timeout=100

# Copy the actual code and helped scripts
COPY ovs-scripts/ovs-docker.sh /app/ovs-scripts/ovs-docker.sh
COPY run.sh /app/
COPY config.sh /app/
COPY docker_api.py /app/

# Calling directly.
ENTRYPOINT ["bash", "run.sh"]
