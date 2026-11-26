# Personal Notes App

A full-stack personal note management application — built as a portfolio project to demonstrate frontend, backend, database, authentication, and DevOps skills for Software Engineer Intern applications.

## Overview

Personal Notes is a Google-Keep-style note-taking app with full user accounts. Every user registers, logs in, and manages their own private notes — organized with categories, tags, favorites, pins, archiving, and a recoverable trash bin.

## Features

- Register / Login / Logout with JWT authentication
- Full Notes CRUD (create, view, edit, delete)
- Soft delete (Trash) with restore and permanent delete
- Favorite and Pin notes
- Archive / unarchive notes
- Search notes by title, content, or tag
- Filter by category, tag, favorite, pinned, or archived status
- Sort by newest, oldest, last updated, or title (A-Z / Z-A)
- Category management (create, rename, delete — deleting a category never deletes its notes, they become "Uncategorized")
- Tag support with many-to-many relationship
- Profile management (update name, view account info)
- Change password
- Light / dark mode (persisted per device)
- Fully responsive: desktop sidebar, tablet 2-column grid, mobile drawer + 1-column grid

## Tech Stack

**Frontend:** React, Vite, Tailwind CSS, React Router, Axios, lucide-react

**Backend:** Java, Spring Boot, Spring Web, Spring Data JPA, Spring Security, JWT (jjwt), BCrypt, Bean Validation

**Database:** PostgreSQL (local/Docker PostgreSQL or Supabase PostgreSQL in production)

**DevOps:** Docker, Docker Compose, Vercel

## Architecture

```
Browser (React SPA on Vercel)
    │  HTTP + JWT Bearer token (/api/*)
    ▼
Spring Boot REST API (Vercel Container Service)
    │  Spring Data JPA
    ▼
PostgreSQL (Docker locally / Supabase in production)
```

- The frontend is a single-page app; all routing happens client-side via React Router.
- Every write action and every read of private data goes through the REST API — there is no direct database access from the browser.
- Authentication is stateless: the backend issues a signed JWT on login, the frontend stores it and attaches it to every subsequent request. The backend never keeps a server-side session.
- Passwords are hashed with BCrypt before being stored; the plaintext password is never persisted or returned by any endpoint.

## Database Schema

```
users            categories         tags              notes                  note_tags
---------        ----------         ----------        ----------             ----------
id (PK)          id (PK)            id (PK)           id (PK)                note_id (FK)
full_name        name               name              title                  tag_id (FK)
email (unique)   created_at         user_id (FK)       content
password                                               is_favorite
created_at                                             is_pinned
updated_at                                             is_archived
                                                        is_deleted
                                                        created_at
                                                        updated_at
                                                        user_id (FK)
                                                        category_id (FK, nullable)
```

Relationships: `User 1—N Notes`, `User 1—N Categories`, `User 1—N Tags`, `Category 1—N Notes`, `Notes N—N Tags` (via `note_tags`).

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Create a new account |
| POST | `/api/auth/login` | Log in, receive a JWT |
| GET | `/api/auth/me` | Get the current user (requires token) |
| GET | `/api/users/me` | Get current user profile |
| PUT | `/api/users/me` | Update full name |
| PUT | `/api/users/me/password` | Change password |
| GET | `/api/notes` | List notes (supports `search`, `category`, `tag`, `favorite`, `pinned`, `archived`, `sort`) |
| GET | `/api/notes/trash` | List soft-deleted notes |
| GET | `/api/notes/{id}` | Get one note |
| POST | `/api/notes` | Create a note |
| PUT | `/api/notes/{id}` | Update a note |
| DELETE | `/api/notes/{id}` | Soft delete (move to trash) |
| PATCH | `/api/notes/{id}/favorite` | Toggle favorite |
| PATCH | `/api/notes/{id}/pin` | Toggle pin |
| PATCH | `/api/notes/{id}/archive` | Toggle archive |
| PATCH | `/api/notes/{id}/restore` | Restore from trash |
| DELETE | `/api/notes/{id}/permanent` | Permanently delete |
| GET | `/api/categories` | List categories |
| POST | `/api/categories` | Create a category |
| PUT | `/api/categories/{id}` | Rename a category |
| DELETE | `/api/categories/{id}` | Delete a category (notes become Uncategorized) |
| GET | `/api/tags` | List the current user's tag names |

## Folder Structure

```
personal-notes-app/
├── frontend/               React + Vite app
│   └── src/
│       ├── components/     Reusable UI (layout, notes, common)
│       ├── pages/          One file per route
│       ├── services/       Axios calls, grouped by resource
│       ├── context/        Auth, Theme, Toast global state
│       ├── hooks/          useNotes, useTrash, useCategories
│       └── routes/         ProtectedRoute
├── backend/                Spring Boot app
│   └── src/main/java/com/notesapp/backend/
│       ├── entity/         JPA entities
│       ├── repository/     Spring Data repositories
│       ├── service/        Business logic
│       ├── controller/     REST endpoints
│       ├── dto/             Request/response shapes
│       ├── security/       JWT + Spring Security config
│       └── exception/      Centralized error handling
├── docker-compose.yml
└── README.md
```

## Installation & Local Development

### Prerequisites
- Node.js 18+
- Java 17+
- Maven (or use the included wrapper, if present)
- PostgreSQL 16+ running locally, OR use Docker Compose (see below) to run it in a container

### Backend
```bash
cd backend
cp .env.example .env   # edit with your real DB credentials and JWT secret
# Export the variables in .env into your shell, or configure them in your IDE's run config
mvn spring-boot:run
```
The API will be available at `http://localhost:8080/api`.

### Frontend
```bash
cd frontend
cp .env.example .env   # VITE_API_URL should point to your backend
npm install
npm run dev
```
The app will be available at `http://localhost:5173`.

## Docker Setup

The easiest way to run the entire stack (frontend + backend + PostgreSQL) is Docker Compose.

First, create your environment file in the project root:
```bash
cp .env.example .env   # edit values if you want, defaults work for local dev
```

Then:
```bash
docker compose up --build
```

This will:
1. Start PostgreSQL 16 and wait until it's healthy (automatically creating the `notesapp` database)
2. Build and start the Spring Boot backend on port `8080`
3. Build and start the React frontend (served by Nginx) on port `5173`

Then open `http://localhost:5173`.

## Production Deployment (Vercel + Supabase)

This repository uses root `vercel.json` (Vercel Services) to host **both** the Vite React frontend and the containerized Spring Boot backend under a single Vercel project and domain, backed by **Supabase PostgreSQL**.

### 1. Database — Supabase PostgreSQL
1. Create a Supabase project and copy your **Connection Pooler** details (recommended over Direct Connection for IPv4 compatibility and PgBouncer connection pooling).
2. Format your JDBC connection variables:
   ```text
   DB_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0
   DB_USERNAME=postgres.<your-project-ref>
   DB_PASSWORD=<your-supabase-db-password>
   ```
   > Hibernate (`ddl-auto: update`) automatically creates the `users`, `categories`, `tags`, `notes`, and `note_tags` tables on first startup. After the initial deployment creates the schema, you can optionally set `DDL_AUTO=none` in Vercel to speed up backend cold starts.

### 2. Frontend + Backend — Single Vercel Project
1. Import this repository in Vercel with the **repository root** (do not set Root Directory to `frontend`; root `vercel.json` routes `/api/*` to `backend/Dockerfile` and `/*` to `frontend`).
2. Add environment variables in Vercel Project Settings:
   - `DB_URL` — Supabase Pooler JDBC URL from step 1 (`?sslmode=require&prepareThreshold=0`)
   - `DB_USERNAME` — Supabase pooler username (`postgres.<your-project-ref>`)
   - `DB_PASSWORD` — Supabase database password
   - `JWT_SECRET` — random string of at least 32 characters (`openssl rand -base64 32`)
   - `FRONTEND_URL` — your Vercel URL (e.g. `https://your-app.vercel.app,https://*.vercel.app`)
3. `VITE_API_URL` does not need to be set in production because `frontend/src/services/api.js` automatically uses `/api` on the same Vercel origin.
4. `frontend/vercel.json` handles SPA client-side routing fallback to `/index.html` on page refresh.

## Environment Variables

**Backend** (`backend/.env.example`):
```
DB_URL=jdbc:postgresql://localhost:5432/notesapp
DB_USERNAME=postgres
DB_PASSWORD=postgres
JWT_SECRET=change-this-to-a-long-random-secret-before-deploying
FRONTEND_URL=http://localhost:5173
```

**Frontend** (`frontend/.env.example`):
```
VITE_API_URL=http://localhost:8080/api
```

Real `.env` files are gitignored and must never be committed.

