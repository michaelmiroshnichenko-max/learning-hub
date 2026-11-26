# Learning Hub — Requirements

Status: Draft v0.1 · Sources: docs/vision.md, stakeholder interview (2026-11-11)
Scope of FRs: phase 2 (weeks 5-7). Technology choices are architecture and are not covered here.

## 1. Goals / Non-goals

### Goals
- G-1 Keep learning notes, tags and source links in one private place.
- G-2 Answer "what did I learn about X?" in seconds by text, tags and course week.
- G-3 Show learning progress across the 26-week course.
- G-4 Support multiple users from the start, with strict per-user privacy.
- G-5 By the end of week 7: deployed at a public URL, CI green, >90% of code written
  by the agent, development cost (AI credits) known.

### Non-goals (phase 2)
- AI features (semantic search, Q&A over notes, quizzes, agents). See Later.
- Collaboration or sharing between users.
- A general-purpose note tool. Scope is the learning course.

## 2. Users and roles

| Role    | Can                                                                 | Cannot                                                     |
|---------|---------------------------------------------------------------------|------------------------------------------------------------|
| Learner | Create, view, edit, delete, search own notes; track own progress    | See or change any other user's data                        |
| Admin   | Create accounts, disable accounts, reset passwords                  | Read any user's notes, tags, source links or progress      |

- One person may hold both roles.
- No anonymous access. No self-registration.
- The first Admin account is created at deployment.

## 3. Functional requirements (phase 2)

### FR-1 Sign in and sign out
As a user, I want to sign in and out so that only I can use my account.
- When a user submits a valid username or email and password for an active, unlocked account,
  the system shall start a session.
- If the credentials are invalid, then the system shall reject the sign-in without revealing
  which credential was wrong.
- If an account reaches 5 consecutive failed sign-in attempts, then the system shall lock it for 15 minutes
  (counter reset and attempts during lockout: OQ-21).
- While an account is locked, the system shall reject every sign-in attempt for it, including
  attempts with valid credentials.
- When a session has been inactive for 8 hours, the system shall end the session
  (what counts as activity: OQ-15).
- If a request is made in a session that has ended, then the system shall reject it and require sign-in.
- When a user signs out, the system shall end the session immediately.
- While a user is not signed in, the system shall deny access to every function except sign-in.

### FR-2 Manage user accounts
As an Admin, I want to create, disable and reset accounts so that only invited people use the hub.
- When an Admin creates an account with a username or email not used by another account, an
  initial password and one or more roles, the system shall create an active account
  (identifier and uniqueness rules: OQ-16).
- If the username or email is already in use, then the system shall reject the creation and say why.
- When an Admin disables an account, the system shall end all its active sessions and block further sign-ins.
- When an Admin resets a user's password, the system shall set a new initial password and end
  that user's active sessions.
- If a user without the Admin role requests an account-management function, then the system shall deny it.

### FR-3 Create a note
As a Learner, I want to save a Markdown note so that my learning is in one place.
- When a Learner submits a note with a title and a Markdown body, the system shall save it as
  owned by that Learner and record its created and updated times.
- If the title or body is empty, then the system shall reject the note and name the missing field
  (meaning of "empty": OQ-17).
- If the body is larger than 100 KB, then the system shall reject the note and state the limit
  (how size is measured: OQ-17).
- Where a course week is given, the system shall accept only a whole number from 1 to 26.
- If a given course week is not a whole number from 1 to 26, then the system shall reject the
  note and state the allowed range.

### FR-4 Tags and source links
As a Learner, I want to add tags and source links to a note so that I can group notes and find the originals.
- When a Learner saves a note with tags, the system shall store each tag in lowercase and treat
  tags that differ only by case as one tag.
- If a note has more than 10 distinct tags, then the system shall reject it and state the limit.
- If a source link is not a valid http or https URL, then the system shall reject the note and
  identify the invalid link.
- The system shall not check whether source links are reachable.

### FR-5 View, edit and delete own notes
As a Learner, I want to read, change and remove my notes so that they stay correct.
- When a Learner opens one of their notes, the system shall show its title, rendered body, tags,
  source links, course week, created and updated times.
- When the system renders a note body, the system shall not execute scripts and shall not render
  raw HTML from the body as markup.
- When a Learner saves changes to a note, the system shall apply the rules of FR-3 and FR-4 and
  update the updated time (concurrent edits and saves after deletion: OQ-12).
- When a Learner deletes a note, the system shall remove it permanently from all views and search
  results (effect of backup restore: OQ-18).

### FR-6 Search notes by text
As a Learner, I want to search my notes by text so that I can find what I learned about a topic.
- When a Learner enters a text query, the system shall return the Learner's notes whose title
  or body matches the query, ignoring letter case (matching rule: OQ-1).
- When a Learner opens the note list without a query or filters, the system shall show all their notes.
- The system shall sort results by updated time, newest first, 20 notes per page.
- If no notes match, then the system shall show a "no notes found" message.

### FR-7 Filter notes by tags and course week
As a Learner, I want to narrow results by tags and week so that I see only relevant notes.
- When a Learner selects one or more tags, the system shall return only notes that have all selected tags.
- When a Learner selects a course week, the system shall return only notes linked to that week.
- When a Learner combines a text query, tags and a course week, the system shall return only
  notes that meet every criterion.

### FR-8 Track progress by course week
As a Learner, I want to see and update my progress per course week so that I know where I stand.
- When a Learner sets their course start date, the system shall derive the date range of each of the 26 weeks from it.
- If the course start date is not a valid calendar date, then the system shall reject it and keep
  the previous start date.
- When a Learner opens the progress view, the system shall show for each week the number of
  linked notes and its status: not started, in progress or done.
- The system shall show "not started" for every week until the Learner changes it.
- When a Learner changes a week's status, the system shall save it and update overall progress.
- If a status change names a week outside 1-26 or a status other than not started, in progress
  or done, then the system shall reject it and leave all progress unchanged.
- The system shall show overall progress as the percentage of weeks with status "done".
- While the current date is inside a course week, the system shall highlight that week.
- If no course start date is set, then the system shall not highlight any week.

### FR-9 Note privacy
As a Learner, I want my data visible only to me so that I can write freely.
- The system shall allow only the owner to view, change, delete, search or count their notes,
  tags, source links and progress.
- If a user requests a note they do not own, then the system shall respond as if the note does not exist.
- Where a user holds the Admin role, the system shall still not show other users' notes, tags,
  source links or progress.
- The system shall show, where notes are written, a notice that work or confidential data must not be entered.

## 4. Non-functional requirements

- NFR-1 Cost: Hosting, data storage, CI and backups shall run on free tiers; recurring cost shall be $0/month.
- NFR-2 Development cost: AI credits used for development shall be recorded per week; the total
  shall be known at the end of week 7.
- NFR-3 Capacity: The system shall support up to 10 users, 5,000 notes per user and 1 GB of total
  data while meeting NFR-4 (whether these are hard limits: OQ-19).
- NFR-4 Performance: At NFR-3 capacity, excluding cold starts, search shall respond within 1 s
  and other user actions within 2 s, at the 95th percentile (test profile: OQ-20).
- NFR-5 Availability: Best effort, no SLA. After an idle period, the first request shall receive
  a complete, correct response within 30 s.
- NFR-6 Backup: Data shall be backed up automatically every day and each backup kept for 7 days.
  A restore shall be tested successfully at least once before week 7.
- NFR-7 Transport: The public URL shall serve content only over HTTPS.
- NFR-8 Credentials: Passwords shall never be stored or logged in plain text.
- NFR-9 Logging: Logs shall not contain note content.
- NFR-10 Data policy: Only fake data or the user's own public data shall be stored; no work data.
- NFR-11 Delivery: By the end of week 7 the system shall be deployed at a public URL, CI shall be
  green, and >90% of the code shall be written by the agent (how this is measured: OQ-8).

## 5. Later (phases 3-6)
- L-1 MCP access: the user's Copilot sessions can read and add notes (week 9). Also the path for
  migrating learning-log.md.
- L-2 Semantic search over notes (weeks 14-15).
- L-3 "Ask my notes": answers grounded in the user's notes (weeks 14-15).
- L-4 Quiz generation from notes and evaluation of answers (weeks 16-17).
- L-5 Personal agent on top of the hub (weeks 22-23).
- L-6 Export of all own notes as Markdown files.

## 6. Out of scope
- Note sharing, collaboration, real-time co-editing.
- Public or anonymous access; self-registration; social login / SSO.
- Admin access to users' notes or progress.
- Mobile apps; offline mode.
- File or image uploads (links only).
- Note version history or trash (delete is permanent).
- Import from browser bookmarks; bulk import in phase 2.
- Time/hour tracking.
- Checking source link reachability.
- Paid features and billing.

## 7. Open questions
- OQ-1 Text search matching: all words or any word? Partial words, word forms, phrases? Are tags and links searched too?
- OQ-2 Password policy (length, complexity)? Must users change the initial password at first sign-in?
- OQ-3 Can an Admin delete an account (not only disable)? What happens to that user's notes?
- OQ-4 How is disabling or locking out the last active Admin prevented or recovered?
- OQ-5 Which time zone defines the current course week and displayed times?
- OQ-6 Is the course always 26 weeks for every user, or configurable per user (e.g. colleagues)?
- OQ-7 Which clients must be supported: browsers, screen sizes (mobile browser?), accessibility level, UI language(s)?
- OQ-8 How is ">90% of code written by the agent" measured?
- OQ-9 Is an audit trail of Admin actions required?
- OQ-10 Can an Admin unlock a locked account early? Is anyone notified of a lockout?
- OQ-11 Limits for title length, tag length/characters, number of source links per note?
  *Partly answered (S1): title max 200 characters. Tag and source-link limits still open.*
- OQ-12 What happens when the same note is edited in two sessions at once, and when a note is
  saved after it was deleted in another session? What does the other session see?
- OQ-13 Is self-service password reset wanted in a later phase?
- OQ-14 Can an Admin re-enable a disabled account? If so, is the user's data kept unchanged?
- OQ-15 Which actions count as activity for the 8-hour inactivity timeout? Do automatic
  background requests reset it?
- OQ-16 Does an account have a username, an email, or both? Are they unique ignoring letter case?
- OQ-17 Is a title or body that contains only whitespace "empty"? Is the 100 KB body limit
  measured in bytes or characters?
  *Answered (S1): whitespace-only counts as empty; the limit is 102,400 bytes of UTF-8
  (config `learninghub.note.body-max-bytes`).*
- OQ-18 May restoring a backup bring back notes that a Learner deleted permanently?
- OQ-19 Are the NFR-3 numbers hard limits? If so, what happens when a limit is reached
  (new user, new note, total data)?
- OQ-20 Performance test profile for NFR-4: how many simultaneous users, which actions, and
  where response time is measured?
- OQ-21 When does the failed sign-in counter reset (after a successful sign-in, after the lockout
  ends)? Do attempts made during a lockout count or extend it?

## 8. Glossary
- Learner: user role that owns notes and progress.
- Admin: user role that manages accounts; has no access to notes.
- Note: a titled Markdown text owned by one Learner, with optional tags, source links and course week.
- Tag: lowercase label used to group notes; max 10 per note.
- Source link: http/https URL pointing to where the knowledge came from.
- Course week: number 1-26 of the 6-month study course; dated from the Learner's course start date.
- Week status: Learner-set state of a course week: not started, in progress, done.
- Overall progress: percentage of course weeks with status "done".
- Session: period of signed-in access; ends on sign-out, 8 h inactivity, disable or password reset.
- Lockout: 15-minute sign-in block after 5 consecutive failed attempts.
- Cold start: delay of the first request after the free-tier host has been idle.
