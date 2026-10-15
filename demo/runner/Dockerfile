FROM python:3.13-alpine
WORKDIR /app
COPY run.py test_run.py ./
RUN python -m unittest discover -p 'test_*.py'
CMD ["python", "-u", "/app/run.py"]
