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
