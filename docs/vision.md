# Learning Hub - vision

## Problem
I study AI engineering for 6 months, 10 hours a week. Notes, links, articles
and quiz answers are spread over learning-log.md, browser bookmarks and chats.
I cannot quickly find "what did I learn about X" or see my progress.

## Users
- Me (main user). Later: maybe colleagues, so multi-user from the start.
- Admin: manages users. No anonymous access.

## Core scenarios (phase 2, weeks 5-7)
1. Save a note (Markdown) with tags and source links.
2. Find notes by text and tags.
3. See learning progress by course week.
4. Log in; nobody sees my notes.

## Later (phases 3-6)
- MCP server: my Copilot sessions can read and add notes (week 9).
- Semantic search and "ask my notes" with RAG (weeks 14-15).
- Quiz generation from notes, evals of answers (weeks 16-17).
- My own agent on top of the hub (weeks 22-23).

## Constraints
- Java 25 + Quarkus (as at work), PostgreSQL, Docker, GitHub Actions.
- Budget: free tiers only. Fake or my own public data only, no work data.
- I do not write code by hand; the agent does.

## Success by week 7
Deployed at a public URL, CI green, >90% code written by the agent,
cost of development known (AI credits).