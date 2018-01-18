# -*- encoding: utf-8 -*-

from __future__ import print_function

from tooling import changed_files, fprint as print, make_decision


def should_run_tests(task, travis_event_type):
    """
    Should we run the tests?

    We skip doing the publish/deploy step when running a build on master that
    doesn't have any relevant changes since the last deploy.
    """
    if travis_event_type == 'cron':
        print('*** We always run tests in cron!')
        return True

    subprocess.check_call(['git', 'fetch', 'origin'])

    assert travis_event_type in ('pull_request', 'push')

    if travis_event_type == 'pull_request':
        changed_files = changed_files('HEAD', 'master')
    else:
        changed_files = changed_files(os.environ['TRAVIS_COMMIT_RANGE'])

    return make_decision(
        changed_files=changed_files,
        task=task,
        action='run tests'
    )
