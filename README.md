# Watchlist v5 — Full Discovery UI Redesign

This version is based on the working Watchlist/TMDB Discovery project and focuses on the visual redesign requested by the user.

## What's new
- Dark purple/navy movie-discovery visual style
- Orange/pink/purple accent palette
- Movie List header and modern search bar
- Category chips: All, Movies, Series, Animation, Top
- Trending Now section
- New Releases section
- Upcoming section
- Popular Movies section
- Popular Series section
- Poster cards with ratings, year/type and Add button
- Cleaner section headers and spacing
- TMDB loading/error states
- My List call-to-action
- Existing Movie/Series/Collection/PC Manager features are retained
- Existing TMDB API layer and Add-to-Watchlist flow retained
- Fixed MainActivity system-bar color handling

## TMDB setup
Create/update `local.properties` in the project root:

TMDB_API_KEY=YOUR_TMDB_READ_ACCESS_TOKEN

Then Sync Project with Gradle Files and rebuild.

## Important
The project archive does not include the Gradle wrapper JAR, so compile verification must be done in Android Studio with the project's normal Gradle environment.
