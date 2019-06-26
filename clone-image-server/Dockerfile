FROM python:2.7

# Install pipenv
RUN pip install pipenv

# CloneImage server is listening here.
EXPOSE 5000

COPY clone_image_api.py /app
COPY diskimage.py /app
COPY Pipfile /app
COPY Pipfile.lock /app

# Setup pipenv for VM Umbox tool
ENV PIPENV_VENV_IN_PROJECT "enabled"
WORKDIR /app
RUN pipenv install --system --deploy --ignore-pipfile

CMD ["pipenv", "run", "python", "clone_image_api.py"]
