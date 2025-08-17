🎯 System Design Interview: RepCheck – AI Gym Tracker
1. Clarify the Problem

Prompt (interviewer):
"Design RepCheck, a mobile app where lifters can:

Track PRs (personal records) for the 4 core lifts (Squat, Bench Press, Deadlift, Press).

Record their form via video and get AI feedback on technique.

Store training history and progress over time."

✅ Candidate clarifications:

Do we need real-time feedback while lifting or is post-set analysis fine?

Do we need social features (sharing PRs, comparing with friends) or just solo use?

Should the app work offline (gyms often have poor internet)?

Assumptions for MVP:

Post-set feedback (upload a video, get form feedback).

Solo use (no social/community features yet).

Offline recording allowed, sync later when online.

2. Define Requirements
   Functional Requirements (User Stories)

📈 As a user, I want to log workouts (sets, reps, weight) for the 4 core lifts.

🎥 As a user, I want to record/upload a video of a set.

🤖 As a user, I want AI feedback on my lift form (depth, bar path, symmetry).

📊 As a user, I want to view my training history and PR progress over time.

🔒 As a user, I want my data securely stored and retrievable on any device.

Non-Functional Requirements

Availability: Offline recording + sync when online.

Latency: AI feedback within ~10s.

Scalability: Support growth from 1k → 100k → 1M users.

Durability: Training history must never be lost.

Security: Videos & data encrypted in transit & at rest.

Cost Efficiency: Minimize GPU/AI inference costs.

3. High-Level Architecture

Conceptual Components:

Client (Mobile App)

Workout logging UI

Video recording/upload

Local offline storage + sync service

Backend API (Kotlin/Spring Boot)

Auth service (JWT tokens)

Workout logging service

Video upload & metadata management

PR calculation service

AI Service

Video processing pipeline

Model inference (form analysis)

Returns structured feedback JSON to backend

Data Storage

Relational DB (Postgres) → Users, workouts, PRs, metadata

Object Storage (S3, GCS, or MinIO) → User videos

Cache (Redis) → Session tokens, hot workout stats

4. Deep Dive / Tradeoffs

Video storage: Object storage instead of DB → scalable & cost-efficient

AI inference: Async processing → user uploads video → notified when feedback is ready

Offline sync: Mobile app keeps SQLite/Realm DB locally, syncs to Postgres when online

Scalability:

Stateless backend → horizontal scaling

CDN for video delivery

Separate AI service → scale GPUs independently

5. Walkthrough (User Flow)

User records squat → stored locally on device

User submits → video uploaded to backend

Backend stores video in S3 + inserts metadata in Postgres

Backend calls AI Service → processes video → sends structured feedback back

User sees feedback in app + training history updated

8) What to say in interviews (a short script)
   State assumptions (users, videos/day, avg size).

Compute storage and bandwidth with the formulas above.

Show a few percentiles for upload time using realistic bandwidth numbers.

Pick SLOs and justify them with UX expectations and cost tradeoffs.

Propose mitigations for slow uploads: resumable uploads, on-device value, background sync, recommend Wi-Fi for cloud opt-in, and lifecycle policies.

9) Quick cheatsheet (copy-paste)
   Video size estimate: size_MB ≈ bitrate_MBps × duration_seconds

Storage (GB/month): = size_MB × videos_per_user_month × users / 1024

Upload time (s): = (size_MB × 8) / upload_Mbps

Average ingest (MB/s): = (videos_per_day × size_MB) / 86400

Plan peak: peak ≈ avg × 5 (start) — refine with real telemetry later.

1️⃣ Framing Requirements
Functional Requirements (FR)

Focus on what the user wants to do.

Use the template:

As a user, I want to… so that I can…

Examples for RepCheck MVP:

📈 As a user, I want to log workouts (sets, reps, weight) for Squat, Bench, Deadlift, and Overhead Press so that I can track my progress.

🎥 As a user, I want to upload a video of a set so that I can get AI feedback on my form.

📊 As a user, I want to view my PR history so that I can see improvements over time.

✅ Always tie it back to user value.

Non-Functional Requirements (NFR)

Focus on how the system behaves rather than what the user does.

Use the template:

I want my system to… or The system should…

Examples for RepCheck MVP:

🔒 I want my system to store workout data securely and reliably so that user data is never lost.

⚡ I want my system to process AI feedback within 10 seconds so that users get timely insights.

🌐 I want my system to work offline and sync later so that gym users with poor internet can still log workouts.

✅ NFRs are usually scalability, latency, reliability, security, cost, etc.

2️⃣ Why SQL / Postgres?

If an interviewer asks: “Why are you using SQL/Postgres for storage?”, you can answer systematically:

Reasoning:

Structured data:

Workouts, sets, users, and video metadata have clear relationships (1 user → many workouts → many sets → optional videos).

SQL handles relations naturally with foreign keys and joins.

Maturity & reliability:

Postgres is battle-tested, has ACID transactions → important for user data safety.

Backups, replication, and high durability are built-in.

Query flexibility:

You might want to calculate PRs, aggregates, history, or query specific lifts over time.

SQL makes these queries simple with GROUP BY, JOIN, ORDER BY.

Integration:

Works well with Kotlin + Spring Boot / Ktor + ORM (Exposed, Hibernate) → fast development.

Scaling is OK for MVP:

1k users × metadata ≈ tiny storage and query load.

Later, if needed, you can scale Postgres with read replicas or partitioning.

Optional contrast (if they push further)

Why not NoSQL?

Could store videos + metadata in MongoDB, but relational queries like “all bench press PRs last month” are harder or slower.

Metadata is structured → SQL is simpler and safer.

✅ Interview tip:

Show you considered alternatives, then justify Postgres as the best tradeoff for MVP: reliable, structured, queryable, and scalable enough for your user base.

If you want, we can next do the PR calculation + AI feedback pipeline and I’ll show how to design it end-to-end — that’s a common interview “deep dive” question.

Do you want to go there?


Relational DB (Postgres)
Non-relational DB (MongoDB)
SQL vs NoSQL
1️⃣ Relational Databases (SQL / Postgres)
a) Structure

SQL databases store data in tables with columns and rows.

Relationships are explicit:

1 user → many workouts

1 workout → many sets

Optional: 1 set → video

Can enforce data integrity using:

Primary keys (unique identifiers)

Foreign keys (relationships between tables)

Constraints (e.g., weight > 0, reps > 0)
1️⃣ Relational Databases (SQL / Postgres)
a) Structure

SQL databases store data in tables with columns and rows.

Relationships are explicit:

1 user → many workouts

1 workout → many sets

Optional: 1 set → video

Can enforce data integrity using:

Primary keys (unique identifiers)

Foreign keys (relationships between tables)

Constraints (e.g., weight > 0, reps > 0)

) ACID Properties

ACID is critical for data reliability, especially when dealing with user workouts, videos, and AI feedback.

Atomicity – “All or nothing”

Example: When a user uploads a workout + video metadata:

Either both workout and video record are saved, or neither.

Prevents inconsistent data.

Consistency – Database rules are always followed

Foreign keys ensure that a video always belongs to a valid workout.

Constraints ensure lifts/reps/weights make sense.

Isolation – Transactions don’t interfere

Two users logging workouts at the same time won’t overwrite each other’s data.

Durability – Once committed, data isn’t lost

Even if the server crashes after saving a workout, Postgres guarantees it’s persisted.

✅ For MVP, ACID is perfect because you cannot afford lost or corrupted user workout/video data.

c) Query Flexibility

Structured queries like:

SELECT lift, MAX(weight) as PR
FROM sets
WHERE user_id = 123
GROUP BY lift;


Easy to calculate PRs, history, averages.

Joins let you combine metadata + AI feedback + videos in one query.

2️⃣ NoSQL Databases

NoSQL databases like MongoDB or DynamoDB are document-based or key-value stores.

Pros:

Can scale horizontally very easily.

Great for unstructured data (e.g., AI feedback JSON, optional fields, variable sets).

Cons:

Harder to do complex queries (PRs, joins, aggregates).

Transactions are more limited (though modern NoSQL DBs support them).

Can lead to data duplication if not designed carefully.

RepCheck Example:

Video metadata + AI feedback could technically go in MongoDB:

{
"workout_id": 123,
"video_url": "s3://...",
"ai_feedback": {"depth": 8, "bar_path": 7}
}


But querying “best squat PR for user X” becomes trickier and slower.

3️⃣ Interview Framing

If asked SQL vs NoSQL, you can say:

✅ “We’re using Postgres because workouts, sets, and videos are highly relational.

✅ We need ACID transactions to prevent lost or corrupted data.

✅ Queries like PRs and history are easier in SQL.

✅ For unstructured AI feedback, we can use JSON fields in Postgres or even a hybrid approach, but MVP simplicity favors relational.”

4️⃣ Teaching Point

Rule of thumb:

Structured + relational + integrity + queries → SQL (Postgres)

Huge scale, flexible schema, unstructured data → NoSQL

MVP: SQL is safer, simpler, and easy to reason about.