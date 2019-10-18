#FROM ubuntu:16.04
FROM python:2.7

# Install Python, pip andpipenv
RUN apt-get update
#RUN apt-get install -yqq python python-pip
RUN pip install pipenv

# Install qemu-img
RUN apt-get install -yqq qemu-utils

# CloneImage server is listening here.
EXPOSE 5000

# Copy python deps and install them
COPY Pipfile /app/
COPY Pipfile.lock /app/

# Installing Python deps without a venv.
WORKDIR /app
RUN pipenv install --system --deploy --ignore-pipfile

# Copy the actual code
COPY clone_image_api.py /app/
COPY diskimage.py /app/

# Calling directly.
CMD ["python", "clone_image_api.py"]
