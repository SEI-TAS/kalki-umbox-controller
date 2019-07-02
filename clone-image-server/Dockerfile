FROM python:2.7

# Install pipenv
RUN pip install pipenv

# CloneImage server is listening here.
EXPOSE 5000

COPY clone_image_api.py /app/
COPY diskimage.py /app/
COPY Pipfile /app/
COPY Pipfile.lock /app/

# Installing Python deps without a venv.
WORKDIR /app
RUN pipenv install --system --deploy --ignore-pipfile

# Calling directly.
CMD ["python", "clone_image_api.py"]
