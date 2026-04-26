# Skill Bridge

JavaFX desktop application for the Skill Bridge platform.

This project currently includes:
- a client home/base interface
- an admin dashboard/base interface
- shared JavaFX styling and navigation
- MySQL database connectivity

## Tech Stack

- Java 21
- JavaFX 21.0.6
- Maven
- MySQL
- ControlsFX
- Ikonli / FontAwesome

## Prerequisites

Before running the project, make sure you have:
- JDK 21 installed
- Maven available, or use the included Maven Wrapper
- MySQL running locally
- a database named `pidev`

## Database Configuration

The current database connection is hardcoded in [DataSource.java](src/main/java/com/pidev/utils/DataSource.java):

```java
URL = "jdbc:mysql://localhost:3306/pidev"
USER = "root"
PASSWORD = ""
```

So by default the app expects:
- host: `localhost`
- port: `3306`
- database: `pidev`
- username: `root`
- password: empty

If your local setup is different, update [DataSource.java](src/main/java/com/pidev/utils/DataSource.java) before running.

## Fight Moderation (Docker API)

Group post creation now calls a fight moderation API before inserting posts in DB.
The current endpoint (`/moderate-image`) expects `multipart/form-data` with a required `file` field.
If a post has no local image attachment, moderation is skipped and the post is allowed.

Set these environment variables before running the app:

- `APP_FIGHT_MODERATION_URL` or `FIGHT_MODERATION_URL` (default: `http://127.0.0.1:8010/moderate-image`)
- `FIGHT_MODERATION_ENABLED` (`true/false` or `1/0`, default: `true`)
- `FIGHT_MODERATION_FAIL_OPEN` (`true/false` or `1/0`, default: `false`)
- `FIGHT_MODERATION_DEBUG` (`true/false` or `1/0`, default: `false`, logs moderation HTTP/decision to console)
- `FIGHT_MODERATION_CONFIDENCE` or `FIGHT_MODERATION_BLOCK_THRESHOLD` (default: `0.80`)
- `FIGHT_MODERATION_TIMEOUT` in seconds (default: `8`)

Windows PowerShell example:

```powershell
$env:APP_FIGHT_MODERATION_URL="http://127.0.0.1:8010/moderate-image"
$env:FIGHT_MODERATION_ENABLED="true"
$env:FIGHT_MODERATION_FAIL_OPEN="0"
$env:FIGHT_MODERATION_DEBUG="1"
$env:FIGHT_MODERATION_CONFIDENCE="0.35"
$env:FIGHT_MODERATION_TIMEOUT="25"
.\mvnw.cmd clean javafx:run
```

## Quick Multi-Account Testing

- Open `Sign in` from top navbar.
- In login page, choose an existing DB user from **Testing User** and click **Login As Selected User**.
- Use **Logout (Guest Mode)** or navbar **Logout** to clear session.
- Feed/group posting and joining now require an active signed-in user.

## Reactions (Facebook-style)

- The old single Like action is replaced with reactions:
  - 👍 Like
  - ❤️ Love
  - 😂 Haha
  - 😮 Wow
  - 😢 Sad
  - 😡 Angry
- Reactions are persisted per `(post, user)` and shown in both main feed and group posts.

## Perspective API (Text Moderation)

Text in posts (title + description) is now checked with Perspective API before publish.

If you prefer code-based setup, open:
`src/main/java/com/pidev/Services/PerspectiveModerationService.java`
and replace:
`DEFAULT_API_KEY = "PASTE_YOUR_PERSPECTIVE_API_KEY_HERE"`
with your real key.

Set these env vars before running:

- `PERSPECTIVE_API_KEY` (required unless `PERSPECTIVE_ENABLED=false`)
- `PERSPECTIVE_ENABLED` (`true/false`, default: `true`)
- `PERSPECTIVE_FAIL_OPEN` (`true/false`, default: `false`)
- `PERSPECTIVE_THRESHOLD` (default: `0.20` for strict blocking)
- `PERSPECTIVE_ATTRIBUTES` (optional CSV, default: `TOXICITY,SEVERE_TOXICITY,INSULT,THREAT,IDENTITY_ATTACK,PROFANITY`)
- `PERSPECTIVE_TIMEOUT` in seconds (default: `10`)
- `PERSPECTIVE_DEBUG` (`true/false`, default: `false`)

Windows PowerShell example:

```powershell
$env:PERSPECTIVE_API_KEY="YOUR_KEY_HERE"
$env:PERSPECTIVE_ENABLED="true"
$env:PERSPECTIVE_FAIL_OPEN="0"
$env:PERSPECTIVE_THRESHOLD="0.20"
$env:PERSPECTIVE_ATTRIBUTES="TOXICITY,SEVERE_TOXICITY,INSULT,THREAT,IDENTITY_ATTACK,PROFANITY"
$env:PERSPECTIVE_TIMEOUT="10"
$env:PERSPECTIVE_DEBUG="1"
.\mvnw.cmd clean javafx:run
```

## How To Run

### IntelliJ IDEA

1. Open the project.
2. Set the project SDK to Java 21.
3. Make sure Maven dependencies are loaded.
4. Run [Main.java](src/main/java/com/pidev/Main.java).

### Maven Wrapper

Windows:

```powershell
.\mvnw.cmd clean javafx:run
```

macOS / Linux:

```bash
./mvnw clean javafx:run
```

## Current Entry Point

The application starts from:

- [Main.java](src/main/java/com/pidev/Main.java)

It loads:

- [base.fxml](src/main/resources/Fxml/client/base.fxml)

and then displays:

- [home.fxml](src/main/resources/Fxml/client/home.fxml)

## Project Structure

```text
src/main/java/com/pidev
  Controllers/
    admin/
    client/
  models/
  utils/

src/main/resources
  Fxml/
    admin/
    client/
  styles/
  images/
```

## Notes For Teammates

- Do not work inside `target/`; it contains generated build output.
- Make code and UI changes in `src/main/java` and `src/main/resources`.
- Some navigation is still UI-first and may point to placeholder or unfinished pages.
- If the app launches but some views fail to open, check whether the related FXML file exists.

## Recommended Team Workflow

1. Clone the repo.
2. Create your own branch from `main`.
3. Pull before starting work.
4. Commit small, focused changes.
5. Open a pull request before merging back to `main`.

## Troubleshooting

### `JAVA_HOME not found`

Set your JDK 21 path in `JAVA_HOME`.

### Database connection fails

Make sure:
- MySQL is running
- the `pidev` database exists
- your username/password match [DataSource.java](src/main/java/com/pidev/utils/DataSource.java)

### A page does not load

That usually means:
- the FXML path is wrong, or
- the referenced view has not been created yet

## Git Ignore

The repo already ignores:
- `target/`
- IDE-specific files
- build outputs

Make sure you do not commit generated files from `target/classes`.
