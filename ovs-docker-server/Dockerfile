FROM python:3.8-alpine

# Install pipenv
RUN pip install pipenv==2018.11.26

# Install ovs switch tools and docker dependencies
RUN apk --no-cache add bash iproute2 util-linux openvswitch docker

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
COPY config.sh /app/
COPY docker_api.py /app/

# Calling directly.
ENTRYPOINT ["bash", "run.sh"]
