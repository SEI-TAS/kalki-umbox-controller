FROM python:2.7-alpine

# Install pip and pipenv
RUN apt-get update
RUN apt-get -yqq install python-pip
RUN pip install pipenv

# CloneImage server is listening here.
EXPOSE 5000

COPY clone-image-server/ /app/

# Setup pipenv for VM Umbox tool
ENV PIPENV_VENV_IN_PROJECT "enabled"
WORKDIR /app
RUN pipenv install

WORKDIR /app/$DIST_NAME
CMD ["pipenv", "run", "python", "clone_image_api.py"]
