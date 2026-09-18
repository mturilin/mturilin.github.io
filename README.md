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
---
```

That is the whole contract. No build step, no index to update, no `layout:` line —
`_config.yml` applies `layout: post` to everything in `_posts/`.

Rules that matter:

- **The filename date sets the position.** Index and archive are both newest-first.
- **No `# Title` heading in the body.** `_layouts/post.html` already renders the
  front-matter `title` as the page `<h1>`; a heading in the body duplicates it.
- **Future-dated posts stay unpublished** until the date passes.
- **`excerpt:` is optional.** Nothing renders it — the index shows titles only —
  but `jekyll-feed` uses it for the RSS summary, and search engines for snippets.

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
| `tools/` | Local preview script and filler posts. Excluded from the Jekyll build |

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

Light and dark are defined as three CSS states: bare `:root` is light, the OS
preference applies unless the reader explicitly chose light, and an explicit dark
choice beats a light OS setting. The switcher in the header persists the choice to
`localStorage`; an inline script in `<head>` applies it before first paint to avoid
a flash. With JavaScript off the button hides and the OS preference wins.

**Note:** the dark token block is written twice in the CSS — once per state — because
CSS cannot reuse a custom-property block. Retuning a dark colour means editing both.
