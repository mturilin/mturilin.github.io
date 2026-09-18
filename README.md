# sodapop

Source for **https://mturilin.github.io/** — strategy and product articles,
AI-written and prompted by Mikhail Turilin.

Built by GitHub Pages' built-in Jekyll. There is no CI and no theme dependency:
push to `main` and the site rebuilds in about a minute.

## Publishing an article

Add one file to `_posts/`:

    _posts/YYYY-MM-DD-slug.md

with front matter:

```yaml
---
title: "Article Title"
date: 2026-09-18 17:54:53 +0200
lede: "One or two sentences shown under the title on the index."
---
```

That is the whole contract. No build step, no index to update, no `layout:` line —
`_config.yml` applies `layout: post` to everything in `_posts/`.

Rules that matter:

- **The filename date sets the position.** Index and archive are both newest-first.
- **No `# Title` heading in the body.** `_layouts/post.html` already renders the
  front-matter `title` as the page `<h1>`; a heading in the body duplicates it.
- **Future-dated posts stay unpublished** until the date passes.
- **Write a `lede:` for every post.** It appears under the title on the index and
  becomes that page's `<meta name="description">`. Jekyll can derive an excerpt
  automatically, but it takes the first paragraph, which is usually an aside
  rather than a hook — the existing essay opens on a note about its sources.
- **`lede:` does not reach the RSS feed.** `jekyll-feed` looks for `description:`
  or falls back to the auto-excerpt, and it has no way to know about a custom
  field. Feed summaries are therefore first paragraphs. Add `description:` to a
  post as well if a particular one matters in feed readers.

## Local preview

`bb tools/preview.bb` renders a static preview to `tools/_preview/index.html`
without needing Ruby or Jekyll. It exists because iterating on CSS is the common
case and does not justify installing the pinned `github-pages` gem.

```bash
bb tools/preview.bb                      # _posts/ only
bb tools/preview.bb _posts tools/demo-posts   # plus filler, to see a fuller index
```

The stylesheet is copied from `assets/css/main.css` on every run, so the repo stays
the single source for styling. The HTML shell in `preview.bb` **mirrors**
`_layouts/default.html` rather than sharing it — Liquid cannot run outside Jekyll —
so structural layout changes must be made in both places. Liquid behaviour
(`relative_url`, permalinks, `feed_meta`) is not exercised by the preview.

## Layout

| Path | Purpose |
| --- | --- |
| `_posts/` | Articles, one file each |
| `_config.yml` | Site settings. `baseurl` is empty - this is a user site, served at the root |
| `_layouts/` | `default` (shell), `post`, `page` |
| `index.html` | Ten most recent posts, titles only |
| `archive.html` | Every post, grouped by year, at `/archive/` |
| `assets/css/main.css` | The entire design |
| `assets/img/` | Banner source plus its generated derivatives |
| `tools/` | Local preview script and filler posts. Excluded from the Jekyll build |

## Banner

`assets/img/banner.png` is the 2048x768 source, kept in the repo so the artwork can
be re-derived if the CSS sizes change. Nothing on the site loads it — it is 2 MB.
What the page serves is the derivative set, WebP with a JPEG fallback and a
`srcset` for 1x/2x:

```bash
for w in 1440 2048; do
  magick assets/img/banner.png -resize ${w}x -strip -quality 82 assets/img/banner-${w}.webp
  magick assets/img/banner.png -resize ${w}x -strip -quality 80 -interlace Plane assets/img/banner-${w}.jpg
done
```

That takes 1994 KB down to 92 KB at 1440px. Regenerate after replacing the source.

The banner and the site `lede` render on the home page only, guarded by
`{% if page.url == '/' %}` in `_layouts/default.html`. The wordmark is inside the
artwork, but the text title stays above it so every page carries the same
title-and-nav row. Article pages get neither — identity only needs establishing
once, and a 270px illustration before a 7,000-word essay is a toll.

## Design

Hand-written, ~300 lines of CSS, no framework and no upstream theme — chosen after
off-the-shelf themes turned out to need either Jekyll 4 (which Pages does not run)
or patched `baseurl` handling. Every internal link goes through `relative_url`, so
moving the site between a subpath and the root is a one-line `_config.yml` change.

Tunable at the top of `assets/css/main.css`:

| Variable | Controls |
| --- | --- |
| `--accent` | The blue: wordmark, all headings, links, list markers, archive year rules |
| `--measure` | Column width |
| `--serif` | Body and heading face (Source Serif 4) |

`_config.yml` carries two descriptions on purpose: `lede` is the full statement
rendered under the banner, and `description` is a ~130-character version for
`<meta>` and RSS, which both truncate near 155.

Light and dark are defined as three CSS states: bare `:root` is light, the OS
preference applies unless the reader explicitly chose light, and an explicit dark
choice beats a light OS setting. The switcher in the header persists the choice to
`localStorage`; an inline script in `<head>` applies it before first paint to avoid
a flash. With JavaScript off the button hides and the OS preference wins.

**Note:** the dark token block is written twice in the CSS — once per state — because
CSS cannot reuse a custom-property block. Retuning a dark colour means editing both.
