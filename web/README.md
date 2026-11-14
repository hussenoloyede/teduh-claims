# Teduh Claims web app

The React front end for Teduh Claims (Vite, React, TypeScript, Storybook).

| Command | What it does |
| --- | --- |
| `npm run dev` | Runs the app on http://localhost:5173 and forwards `/api` to Spring Boot on port 8080 |
| `npm run storybook` | Opens the component library on http://localhost:6006 |
| `npm test` | Runs every story as a test in headless Chromium, with accessibility checks |
| `npm run lint` | Lints with oxlint |
| `npm run build` | Type checks, then builds to `dist/` |
| `npm run api:types` | Regenerates `src/api/schema.d.ts` from `openapi.json` (see below) |

Colours, spacing and type come from `src/tokens.css`, copied from the Figma variables.

## API types

`openapi.json` is a saved copy of the API's OpenAPI document (`/v3/api-docs`). The Java test `ApiDocsTest` rewrites it and fails whenever the API changes, so it can't drift. `npm run api:types` turns it into TypeScript types in `src/api/schema.d.ts`, which `src/api/client.ts` uses to check every call. After an API change: run the Java tests, run `npm run api:types`, fix what TypeScript complains about, and commit both files.

The generator runs through `npx` with TypeScript 5, because it uses TypeScript's JavaScript API, which TypeScript 7 (the app's compiler) no longer includes.
