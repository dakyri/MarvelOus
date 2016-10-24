# MarvelOus
## Trawl Marvel API for particular characters.

Configured via a properties file in the asset folder ("marvel.properties")

Looks for the first given character matching an input string (using the marvel api 'startsWith' parameter).

Caches up to 5 results in an SQLite db, and displays these on the openning page: these can be viewed with a click.

Built in Android Studio

Dependencies
 - rx
 - glide
 - okhttp3
 - gson
