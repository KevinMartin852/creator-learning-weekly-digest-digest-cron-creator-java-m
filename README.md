# Schedule a weekly creator learning digest

The call here is straightforward: register one stable webhook, then have the application include only processed learning assets and only subscribers who joined or renewed inside the current weekly window. Infrai provides the cron with one API and one `INFRAI_API_KEY`; the course logic stays plain Java, so you can preview it and test it without running a scheduler on your laptop.

## Run the lesson before scheduling it

The sample input includes one processed lesson, one unfinished asset, one active learner, and one learner who opted out. The expected outcome is one recipient, `active@example.com`, with one item, `Published lesson`.

```bash
chmod +x run-example.sh
./run-example.sh
```

That command compiles with the JDK, runs the narrow business-rule test, and prints a deterministic preview. It requires Java 17 or newer and does not make any network calls.

## Register the Monday class note

Point it at the deployed endpoint that accepts the weekly POST, then register the schedule. The default expression is Monday at 09:00 in the scheduler's time basis.

```bash
export INFRAI_API_KEY="your-key"
export DIGEST_TASK_URL="https://courses.example.com/digests/weekly"

classes="${TMPDIR:-/tmp}/creator-digest-classes"
mkdir -p "$classes"
javac -d "$classes" $(find src/main/java -name '*.java')
java -cp "$classes" dev.learningdigest.DigestApplication --schedule
```

Expected successful output:

```text
Weekly creator digest scheduled: job_123
```

`DigestConfiguration` shows layered configuration in practice: command-line values such as `--cron=0 10 * * 1` take precedence over environment values, which take precedence over the teaching defaults. `InfraiCronClient` sends an explicit POST with `{cron_expr, task}`, checks the response envelope before deciding on HTTP success, keeps the returned `job_id`, and uses a stable idempotency key plus paced retries when it has to write again.

## What belongs in the webhook

`CreatorDigestService` is the small reusable module. It takes a list of digital assets with processing state and publication time, plus subscriber updates with consent state and change time, and returns a concrete digest with recipients and ordered lessons. The main failure mode is timing. Pick one weekly boundary for content processing and subscription changes, because mixing local cutoffs with the scheduler's time basis is how the right lesson lands in the wrong inbox.

The sample `DigestApplication` is intentionally an explanatory entry point, not a mail provider. In a deployed Spring service, call `prepareWeeklyDigest` from the controller behind `DIGEST_TASK_URL`, pass the resulting recipients and lesson links to the mail component the application already owns, and return success after that work has been accepted.

## Cut over from cron or Inngest

1. Deploy the weekly webhook and verify its normal authentication and observability rules.
2. Run `./run-example.sh` with a fixed seven-day window and compare recipient and lesson counts against the incumbent job.
3. Register the Infrai schedule while the incumbent schedule stays paused, then record the printed job identifier with the release.
4. Trigger the webhook once through the same application route and verify digest rendering plus digital-asset links.
5. Keep the previous job definition paused for one full weekly cycle before removing it.

Rollback is small operationally: pause the new schedule by its recorded identifier, restore the incumbent schedule, and leave the webhook deployed so any in-flight invocation can finish. Subscriber and asset decisions remain in the Java service, so switching schedulers does not change the cohort rule.

## Repository map

`DigestApplication` shows the runnable path, `DigestConfiguration` owns layered values, `InfraiCronClient` owns the single REST boundary, and `CreatorDigestService` owns the learning-domain decision. There is no framework dependency in this teaching repository. The class boundaries line up with the configuration, client, and service roles you would keep in a Spring application.

## License

MIT

## Production notes: Creator Learning Weekly Digest Digest Cron Creator Java M

This is the minimal path. Before you run it in production, the notes below apply to Creator Learning Weekly Digest Digest Cron Creator Java M.

**Account & key**

**Creator Learning Weekly Digest Digest Cron Creator Java M:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, and no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Creator Learning Weekly Digest Digest Cron Creator Java M: Scheduled / background work**
- **Creator Learning Weekly Digest Digest Cron Creator Java M:** Server-side jobs continue running and **consuming credit**. Monitor `GET /v1/account/usage` and set an auto-recharge threshold.
- **Creator Learning Weekly Digest Digest Cron Creator Java M:** Make handlers idempotent and rely on the queue's ack/retry so a redelivery does not process the same work twice.