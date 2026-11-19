#!/usr/bin/env python3
"""End-to-end checks against docker-compose.test.yml's isolated signed-JWT fixture."""
import json
import time
import uuid
import urllib.request
import urllib.error
from datetime import date, timedelta

def call(port, path, token=None, method='GET', body=None, expected=200):
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request(f'http://127.0.0.1:{port}{path}', headers=headers, method=method,
                                 data=None if body is None else json.dumps(body).encode())
    try:
        response = urllib.request.urlopen(req, timeout=15)
        status, payload = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, payload = error.code, error.read()
    assert status == expected, f'{method} {port}{path}: expected {expected}, got {status}: {payload[:400]}'
    return json.loads(payload) if payload else None

def until(check, timeout=180):
    deadline = time.monotonic() + timeout
    last = None
    while time.monotonic() < deadline:
        try:
            value = check()
            if value:
                return value
        except Exception as error:
            last = error
        time.sleep(2)
    raise AssertionError(f'Timed out waiting for pipeline: {last}')

def run():
    for port in range(8081, 8088):
        until(lambda: call(port, '/actuator/health'))
    admin = call(19000, '/token?subject=test-admin&role=Admin')['accessToken']
    client = call(19000, '/token?subject=test-client')['accessToken']
    other = call(19000, '/token?subject=other-client')['accessToken']
    call(8084, '/api/v1/services')
    call(8087, '/api/v1/admin/kafka/topics', expected=401)
    call(8087, '/api/v1/admin/kafka/topics', client, expected=403)
    call(8084, '/api/v1/services', client, 'POST', {}, 403)
    call(8087, '/api/v1/admin/kafka/topics', 'invalid', expected=401)
    for query in ['issuer=wrong-issuer', 'audience=wrong-audience', 'expired=true']:
        invalid = call(19000, '/token?subject=test-admin&role=Admin&' + query)['accessToken']
        call(8087, '/api/v1/admin/kafka/topics', invalid, expected=401)
    own = call(8082, '/api/v1/clients/me', client)
    suffix = uuid.uuid4().hex[:8]
    service = call(8084, '/api/v1/services', admin, 'POST', {
        'name': 'Integration ' + suffix, 'description': 'Isolated test', 'category': 'cabello',
        'basePrice': 25000, 'durationMinutes': 60, 'active': True}, 201)
    stylist = call(8082, '/api/v1/stylists', admin, 'POST', {'name': 'Integration ' + suffix, 'isAvailable': True}, 201)
    initial = call(8085, '/api/v1/analytics', admin)
    appointment_day = date.today() + timedelta(days=7)
    if appointment_day.weekday() == 6:
        appointment_day += timedelta(days=1)
    payload = {'clientId': 'forged', 'clientName': 'forged', 'serviceId': service['id'], 'stylistId': stylist['id'],
               'date': str(appointment_day), 'time': '12:00', 'price': 1, 'durationMinutes': 1}
    appointment = call(8081, '/api/v1/appointments', client, 'POST', payload, 201)
    assert appointment['price'] == 25000
    assert appointment['clientId'] == own['id']
    aid = appointment['id']
    call(8081, '/api/v1/appointments/' + aid, other, expected=403)
    call(8081, '/api/v1/appointments', client, 'POST', payload, 409)
    call(8081, f'/api/v1/appointments/{aid}/status?status=en_proceso', client, 'PUT', expected=400)
    call(8081, f'/api/v1/appointments/{aid}/status?status=en_proceso', admin, 'PUT')
    call(8081, f'/api/v1/appointments/{aid}/status?status=completada', admin, 'PUT')
    call(8081, f'/api/v1/appointments/{aid}/status?status=completada', admin, 'PUT')
    def metrics_updated():
        m = call(8085, '/api/v1/analytics', admin)
        return m if m['completedServices'] == initial['completedServices'] + 1 else None
    metrics = until(metrics_updated)
    assert metrics['serviceValue'] == initial['serviceValue'] + 25000
    until(lambda: len([e for e in call(8086, '/api/v1/audit?size=100', admin)['content'] if e['appointmentId'] == aid]) == 4)
    until(lambda: any(n['appointmentId'] == aid for n in call(8083, '/api/v1/notifications', admin)))
    topic = 'elegance.integration.' + suffix
    call(8087, '/api/v1/admin/kafka/topics', admin, 'POST', {'name': topic, 'partitions': 1, 'replicationFactor': 1, 'configs': {'retention.ms': '60000'}}, 201)
    call(8087, f'/api/v1/admin/kafka/topics/{topic}/configs', admin, 'PATCH', {'retention.ms': '120000'})
    call(8087, f'/api/v1/admin/kafka/topics/{topic}/partitions', admin, 'PATCH', {'total': 3})
    description = call(8087, f'/api/v1/admin/kafka/topics/{topic}', admin)
    assert len(description['partitions']) == 3
    assert description['configs']['retention.ms'] == '120000'
    call(8087, f'/api/v1/admin/kafka/topics/{topic}', admin, 'DELETE', expected=204)
    groups = call(8087, '/api/v1/admin/kafka/groups', admin)
    assert any(g['groupId'] == 'elegance-audit-v1' for g in groups)
    until(lambda: all(r['lag'] == 0 for r in call(8087, '/api/v1/admin/kafka/groups/elegance-audit-v1/offsets', admin)))
    print('PASS: JWT/roles, ownership, canonical pricing, overlap, both Kafka topics, analytics, audit, RabbitMQ notifications, Kafka administration and consumer lag.')

if __name__ == '__main__':
    run()
