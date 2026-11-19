#!/usr/bin/env python3
"""Durability, duplicate delivery, Rabbit DLQ and Kafka DLT checks in isolated containers."""
import os
import subprocess
import time
from integration import call, until
DOCKER = os.environ.get('DOCKER', 'docker')
ENV_FILE = os.environ.get('ELEGANCE_TEST_ENV', '.env.integration')
COMPOSE = [DOCKER, 'compose', '--env-file', ENV_FILE, '-f', 'docker-compose.yml', '-f', 'docker-compose.test.yml', '-p', os.environ.get('ELEGANCE_PROJECT', 'elegance-integration')]

if os.environ.get('ELEGANCE_CLUSTER') == 'true':
    COMPOSE[COMPOSE.index('-p'):COMPOSE.index('-p')] = ['-f', 'docker-compose.cluster.yml']

def command(*args, payload=None):
    return subprocess.run(COMPOSE + list(args), input=payload, text=True, capture_output=True, check=True).stdout

def main():
    admin = call(19000, '/token?subject=test-admin&role=Admin')['accessToken']
    before = call(8085, '/api/v1/analytics', admin)
    events = call(8086, '/api/v1/audit?size=100', admin)['content']
    event = next(e for e in events if e['eventType'] == 'COMPLETED')
    payload = event['appointmentId'] + ':' + event['payload'] + '\n'
    command('exec', '-T', 'kafka1', 'kafka-console-producer', '--bootstrap-server', 'kafka1:29092', '--topic', 'elegance.services.completed.v1', '--property', 'parse.key=true', '--property', 'key.separator=:', payload=payload)
    time.sleep(5)
    after = call(8085, '/api/v1/analytics', admin)
    assert before['completedServices'] == after['completedServices']
    assert before['serviceValue'] == after['serviceValue']
    initial = call(8081, '/api/v1/admin/queues/appointment.created.dlq', admin)['messageCount']
    call(8083, '/api/v1/notifications/test-dlq', admin, 'POST')
    until(lambda: call(8081, '/api/v1/admin/queues/appointment.created.dlq', admin)['messageCount'] > initial)
    command('exec', '-T', 'kafka1', 'kafka-console-producer', '--bootstrap-server', 'kafka1:29092', '--topic', 'elegance.appointments.events.v1', '--property', 'parse.key=true', '--property', 'key.separator=:', payload='invalid-key:{bad json\n')
    def dead_message():
        value = command('exec', '-T', 'kafka1', 'kafka-console-consumer', '--bootstrap-server', 'kafka1:29092', '--topic', 'elegance.appointments.events.v1.DLT', '--from-beginning', '--max-messages', '1', '--timeout-ms', '10000')
        return '{bad json' in value
    until(dead_message, 60)
    call(8087, '/api/v1/admin/kafka/groups/elegance-audit-v1/offsets', admin, 'POST',
         {'topic': 'elegance.appointments.events.v1', 'partition': 0, 'offset': 0}, 409)
    command('exec', '-T', 'kafka1', 'kafka-console-producer', '--bootstrap-server', 'kafka1:29092', '--topic', 'elegance.analytics.facts.v1.DLT', '--property', 'parse.key=true', '--property', 'key.separator=:', payload=payload)
    text = command('exec', '-T', 'kafka1', 'kafka-get-offsets', '--bootstrap-server', 'kafka1:29092', '--topic', 'elegance.analytics.facts.v1.DLT')
    partition = next(int(line.split(':')[1]) for line in text.splitlines() if int(line.split(':')[2]) > 0)
    replay = call(8087, '/api/v1/admin/kafka/replay', admin, 'POST', {'topic': 'elegance.analytics.facts.v1.DLT', 'partition': partition, 'offset': 0, 'limit': 1})
    assert replay['replayed'] == 1
    saved = call(8083, '/api/v1/notifications', admin)
    assert saved
    command('restart', 'notification-service', 'audit-service')
    until(lambda: call(8083, '/actuator/health'))
    until(lambda: call(8086, '/actuator/health'))
    assert any(n['id'] == saved[0]['id'] for n in call(8083, '/api/v1/notifications', admin))
    assert any(e['eventId'] == event['eventId'] for e in call(8086, '/api/v1/audit?size=100', admin)['content'])
    print('PASS: duplicate replay does not duplicate service value; RabbitMQ DLQ; malformed JSON Kafka DLT; notification and audit persistence across restart.')

if __name__ == '__main__':
    main()
