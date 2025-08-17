# 🎯 System Design Interview: RepCheck – AI Gym Tracker (MVP)

## 1️⃣ Clarify the Problem

Interviewer Prompt:
"Design RepCheck, a mobile app for lifters to track the 4 core lifts, record videos, and get AI feedback on form."

Candidate (You) Response:

"First, I want to clarify assumptions:

- MVP only supports the 4 lifts: Squat, Bench, Deadlift, Overhead Press.
- Feedback is post-set, not real-time.
- Solo use; no social features yet.
- Offline recording allowed; mobile app syncs later."

This shows you ask questions before diving in, which is key in interviews.

## 2️⃣ Functional Requirements (User Stories)

Framed as: "As a user, I want to..."

- 📈 Log workouts (sets, reps, weight) for core lifts
- 🎥 Record/upload a video of a set
- 🤖 Receive AI feedback on lift form (depth, bar path, symmetry)
- 📊 View training history and track PRs
- 🔒 Securely store data, retrievable on any device

## 3️⃣ Non-Functional Requirements

Framed as: "I would like my system to..."

- ✅ Availability: works offline → syncs later
- ⏱ Latency: AI feedback within ~10s for good UX
- 📈 Scalability: support 1k → 100k → 1M users
- 💾 Durability: no user data or videos are lost
- 🔐 Security: encryption at rest & in transit
- 💰 Cost efficiency: minimize GPU inference costs

## 4️⃣ High-Level Architecture

### Components

- Mobile App (Client)
  - Workout & set logging
  - Video recording
  - Offline storage (SQLite/Realm)
  - Syncs with backend when online

 

#### Backend API (Ktor/Spring)

* Auth Service: login, JWT, refresh tokens
* Workout Service: CRUD workouts & sets
* Video Service:
	+ Video metadata CRUD
	+ Presigned S3 URLs
	+ AI queue jobs
* PR Service: compute personal records

#### Database

* Postgres
* Tables: users, lifts, workouts, sets, videos, ai_feedback
* ACID compliance, structured queries, joins

#### Object Storage

* S3
* Stores actual video files
* Metadata stays in Postgres

#### Async Queue

* RabbitMQ / Kafka / SQS
* Decouples heavy AI processing from main backend

#### AI Video Service

* Preprocessing (resize, compress, extract frames)
* Model inference → depth, bar path, symmetry
* Returns JSON feedback → backend updates ai_feedback table

 

### Data Flow

User logs workout → app stores locally

App uploads sets + video metadata → backend inserts into Postgres

Video uploaded to S3 via presigned URL

Backend pushes job to queue

AI Service consumes job → processes video → updates ai_feedback table

Mobile app polls backend → displays feedback + PRs

## 5️⃣ Database Design

Users: id, email, password_hash, timestamps

Lifts: id, name (Squat, Bench, Deadlift, OHP), category, timestamps

Workouts: id, user_id, date, notes, timestamps

Sets: id, workout_id, lift_id, weight, reps, timestamp

Videos: id, user_id, workout_id, s3_url, status, timestamps

AI Feedback: id, video_id, depth_score, bar_path_score, symmetry_score, comments, timestamps

Why lifts table?

Flexible → add new lifts anytime

Store metadata (muscle group, compound/isolation)

Joins make PR calculations easier

## 6️⃣ Storage & Networking Considerations

Video estimates:

720p HD ~1 MB/sec

20-second video → 20 MB

3 videos per user/day × 1k users = 60,000 MB = ~60 GB/day

Use S3 for scalable storage

Networking:

Mobile uploads via HTTPS → presigned S3 URL

Async queue decouples AI processing → avoids backend blocking

Optional: background uploads, resumable uploads for slow connections

## 7️⃣ Tradeoffs & Interview Talking Points

Postgres vs NoSQL:

Relational ACID compliance → ensures PRs and history are consistent

NoSQL good for unstructured data, but we need structured joins

Video Storage: S3 → cost-effective, scalable, avoids bloating Postgres

AI Inference: async → scalable, backend remains responsive

Offline Mode: SQLite/Realm on mobile → sync later

Extensibility: lifts table → future-proof design