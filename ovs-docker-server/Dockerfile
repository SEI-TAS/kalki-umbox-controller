FROM python:2.7

# Install pipenv
RUN apt-get update
RUN pip install pipenv

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

# Calling directly.
ENTRYPOINT ["bash", "run.sh"]
