# Repository Guidelines

## Project Structure & Module Organization
- `pom.xml` defines a Maven WAR module (`sem-app`) targeting Java 17.
- Java sources live in `src/main/java/org/example`; expand new packages beneath `org.example`.
- Web assets are in `web/`: JSP/HTML (`index.jsp`, `simple.html`), JS in `web/js`, CSS in `web/css`, and servlet config in `web/WEB-INF/web.xml`.
- Build output lands in `target/sem-app.war`; deploy to a Servlet 4 container (Tomcat/Jetty).

## Build, Test, and Development Commands
- `mvn clean package` — compile and assemble the WAR from `web/` and `src/main/java`.
- `mvn test` — run unit tests in `src/test/java` (Surefire picks up `*Test`); currently none exist.
- Deploy locally by dropping `target/sem-app.war` into Tomcat’s `webapps/`, then restart the container.

## Coding Style & Naming Conventions
- Java 17, 4-space indentation, UTF-8; avoid tabs.
- Packages lowercase (`org.example.*`), classes PascalCase, methods/fields camelCase, constants UPPER_SNAKE.
- Keep servlet endpoints and JSP names kebab-case to mirror existing assets; align JS/CSS filenames with that pattern.
- Favor small, single-responsibility classes; keep servlet wiring centralized in `WEB-INF/web.xml`.

## Testing Guidelines
- Place tests under `src/test/java`; name classes `SomethingTest` so Maven runs them.
- Prefer JUnit 5; add Mockito for servlet/unit isolation if/when dependencies are introduced.
- For servlet-facing code, mock request/response objects and assert generated HTML/JSON snippets.
- When adding JS utilities, include minimal usage examples or DOM-free tests via a JS harness, or document manual steps if no runner is present.

## Commit & Pull Request Guidelines
- Follow existing history: short, action-oriented summaries (Chinese or English), e.g., `完善请求路由` or `Add request validation`.
- Reference related tickets/issues in the body and note behavior changes plus testing performed.
- For UI changes, attach before/after screenshots and note browsers tested.
- Include deployment notes if WAR structure or `web.xml` mappings change.

## Security & Configuration Tips
- Keep secrets out of the repo; use container-level env vars or context params instead of hardcoded constants.
- Validate and sanitize incoming parameters in servlets/JS; avoid reflecting raw input.
- Pin dependency versions in `pom.xml`, and keep container APIs on `provided` scope.

## language
- always answer in Simplified Chinese