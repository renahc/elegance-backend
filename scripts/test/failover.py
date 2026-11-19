#!/usr/bin/env python3
"""Verify replicated topics and continued processing with one local broker down."""
import os
import subprocess
from integration import call, until, run
DOCKER = os.environ.get('DOCKER', 'docker')
COMPOSE = [DOCKER, 'compose', '--env-file', os.environ.get('ELEGANCE_TEST_ENV', '.env.integration'), '-f', 'docker-compose.yml', '-f', 'docker-compose.cluster.yml', '-f', 'docker-compose.test.yml', '-p', os.environ.get('ELEGANCE_PROJECT', 'elegance-evaluation')]

def main():
    admin = call(19000, '/token?subject=test-admin&role=Admin')['accessToken']
    for topic in ['elegance.appointments.events.v1', 'elegance.services.completed.v1']:
        description = call(8087, '/api/v1/admin/kafka/topics/' + topic, admin)
        assert len(description['partitions']) == 3
        assert all(len(p['replicas']) == 3 for p in description['partitions'])
        until(lambda: all(len(p['isr']) == 3 for p in call(8087, '/api/v1/admin/kafka/topics/' + topic, admin)['partitions']))
    subprocess.run(COMPOSE + ['stop', 'kafka1'], check=True, capture_output=True)
    try:
        # Both producer bootstrap list and AdminClient must include the remaining brokers.
        run()
    finally:
        subprocess.run(COMPOSE + ['start', 'kafka1'], check=True, capture_output=True)
    until(lambda: all(len(p['isr']) == 3 for p in call(8087, '/api/v1/admin/kafka/topics/elegance.appointments.events.v1', admin)['partitions']))
    print('PASS: three replicas per partition, continued writes/consumption with kafka1 down, ISR recovery after restart.')

if __name__ == '__main__':
    main()
