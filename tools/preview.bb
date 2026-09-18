#!/usr/bin/env bb
;; What    : Renders a local static preview of the blog from _posts/, so the design
;;           can be checked in a browser without a Ruby/Jekyll toolchain.
;; Why     : GitHub Pages builds the real site, and installing the pinned
;;           github-pages gem locally is slow and version-fragile. Iterating on CSS
;;           is the common case and does not need real Jekyll.
;; Design  : The stylesheet is NOT duplicated - it is copied from assets/css/main.css
;;           on every run, so the repo remains the single source for styling.
;; Gotcha  : The HTML shell here MIRRORS _layouts/default.html rather than sharing it,
;;           because Liquid cannot run outside Jekyll. Structural changes to the real
;;           layout must be mirrored here or the preview drifts. Liquid-specific
;;           behaviour (relative_url, feed_meta, permalinks) is NOT exercised.
;; Usage   : bb tools/preview.bb [posts-dir ...]   ->  tools/_preview/index.html
;;           With no argument it previews _posts/ alone. Pass tools/demo-posts too
;;           to see how the index and archive look with several years of entries:
;;             bb tools/preview.bb _posts tools/demo-posts

(require '[babashka.fs :as fs]
         '[babashka.process :refer [shell]]
         '[clojure.string :as str])

(def repo (str (fs/parent (fs/parent (fs/absolutize *file*)))))
(def posts-dirs (if (seq *command-line-args*) *command-line-args* [(str repo "/_posts")]))
(def out (str repo "/tools/_preview"))
(def site-title "sodapop")
;; Mirrors site.lede in _config.yml. Duplicated because this script cannot read
;; Liquid; if the real lede changes, change it here too.
(def site-lede
  (str "This blog collects articles written by AI from my detailed prompts. Gathering "
       "sources and organising them into clusters of themes is a surprisingly good way "
       "to learn a new subject &mdash; it stays balanced, and holds several points of view "
       "at once instead of settling early on one. I don&rsquo;t plan to post regularly or "
       "stick to a topic. These are simply the ones I found interesting enough to publish."))

(defn front-matter
  "Splits a post file into [metadata-map body-markdown]. Handles only the flat
   key: value front matter our posts use."
  [raw]
  (let [[_ fm body] (str/split raw #"(?m)^---\s*$" 3)
        kv (into {} (for [line (str/split-lines (or fm ""))
                          :let [[_ k v] (re-matches #"^([a-zA-Z_]+):\s*(.*)$" line)]
                          :when k]
                      [(keyword k) (str/replace (str/trim v) #"^\"|\"$" "")]))]
    [kv (str/triml (or body ""))]))

(defn md->html [md]
  (:out (shell {:in md :out :string} "cmark-gfm" "--unsafe"
               "--extension" "table" "--extension" "strikethrough"
               "--extension" "footnotes")))

(def months ["Jan" "Feb" "Mar" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"])

(defn pretty-date [iso]
  (let [[y m d] (map parse-long (str/split (subs iso 0 10) #"-"))]
    (format "%s %d, %d" (months (dec m)) d y)))

(defn layout [{:keys [title here depth body]}]
  (let [root (str/join (repeat (or depth 0) "../"))]
    (str "<!doctype html>\n<html lang=\"en\">\n<head>\n"
         "<meta charset=\"utf-8\">\n"
         "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
         "<title>" title "</title>\n"
         "<link rel=\"stylesheet\" href=\"" root "assets/css/main.css\">\n"
         "<script>try{var t=localStorage.getItem('theme');if(t)document.documentElement.setAttribute('data-theme',t)}catch(e){}</script>\n"
         "</head>\n<body>\n<div class=\"wrap\">\n"
         "  <header class=\"masthead" (when (= here :home) " masthead--home") "\">\n"
         "    <h1 class=\"site-title\"><a href=\"" root "index.html\">" site-title "</a></h1>\n"
         "    <nav class=\"nav\">\n"
         "      <a href=\"" root "index.html\"" (when (= here :home) " aria-current=\"page\"") ">Home</a>\n"
         "      <a href=\"" root "archive.html\"" (when (= here :archive) " aria-current=\"page\"") ">Archive</a>\n"
         "      <a href=\"" root "feed.xml\">Feed</a>\n"
         "      <button class=\"theme-toggle\" type=\"button\" hidden title=\"Switch theme\">"
         "<svg viewBox=\"0 0 16 16\" aria-hidden=\"true\" focusable=\"false\">"
         "<circle cx=\"8\" cy=\"8\" r=\"6.4\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.4\"/>"
         "<path d=\"M8 1.6a6.4 6.4 0 0 1 0 12.8z\" fill=\"currentColor\"/></svg></button>\n"
         "    </nav>\n"
         ;; banner and lede are home-only, matching the {% if page.url == '/' %}
         ;; guard in _layouts/default.html
         (when (= here :home)
           (str "    <a class=\"banner\" href=\"" root "index.html\">\n"
                "      <picture>\n"
                "        <source type=\"image/webp\" srcset=\"" root "assets/img/banner-1440.webp 1440w, " root "assets/img/banner-2048.webp 2048w\" sizes=\"(max-width: 47rem) 100vw, 45rem\">\n"
                "        <img src=\"" root "assets/img/banner-1440.jpg\" width=\"2048\" height=\"768\" alt=\"" site-title "\" fetchpriority=\"high\">\n"
                "      </picture>\n    </a>\n"
                "    <p class=\"lede\">" site-lede "</p>\n"))
         "  </header>\n  <main>\n" body "\n  </main>\n"
         "  <footer class=\"foot\"><span>&copy; 2026 Mikhail Turilin</span></footer>\n"
         "</div>\n"
         "<script>\n"
         "(function(){var root=document.documentElement,btn=document.querySelector('.theme-toggle');\n"
         "if(!btn)return;btn.hidden=false;\n"
         "function cur(){var s=root.getAttribute('data-theme');return s?s:(matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light')}\n"
         "function label(){btn.setAttribute('aria-label','Switch to '+(cur()==='dark'?'light':'dark')+' theme')}\n"
         "label();\n"
         "btn.addEventListener('click',function(){var n=cur()==='dark'?'light':'dark';\n"
         "root.setAttribute('data-theme',n);try{localStorage.setItem('theme',n)}catch(e){}label()});\n"
         "})();\n</script>\n</body>\n</html>\n")))

;; Mirrors the contents script in _layouts/post.html. Duplicated for the same
;; reason as the page shell: Liquid cannot run here.
(def toc-script
  (str "<script>\n"
       "(function(){var body=document.querySelector('.post-body');if(!body)return;\n"
       "var hs=[].slice.call(body.querySelectorAll('h2'));if(hs.length<4)return;\n"
       "var nav=document.createElement('nav');nav.className='toc';nav.setAttribute('aria-label','On this page');\n"
       "var t=document.createElement('p');t.className='toc-title';t.textContent='On this page';nav.appendChild(t);\n"
       "var ol=document.createElement('ol'),links=[];\n"
       "hs.forEach(function(h,i){if(!h.id)h.id='section-'+i;\n"
       "var a=document.createElement('a');a.href='#'+h.id;a.textContent=h.textContent;\n"
       "var li=document.createElement('li');li.appendChild(a);ol.appendChild(li);links.push(a)});\n"
       "nav.appendChild(ol);body.parentNode.insertBefore(nav,body);\n"
       "var ticking=false;\n"
       "function sync(){ticking=false;var active=0;\n"
       "hs.forEach(function(h,i){if(h.getBoundingClientRect().top<=120)active=i});\n"
       "links.forEach(function(a,i){a.setAttribute('aria-current',i===active?'true':'false')})}\n"
       "addEventListener('scroll',function(){if(!ticking){ticking=true;requestAnimationFrame(sync)}},{passive:true});\n"
       "sync();})();\n</script>\n"))

(defn item [{:keys [slug title date lede]}]
  (str "    <li>\n      <time>" (pretty-date date) "</time>\n"
       "      <a href=\"posts/" slug ".html\">" title "</a>\n"
       (when lede (str "      <p>" lede "</p>\n"))
       "    </li>\n"))

(def posts
  (->> (mapcat #(fs/glob % "*.md") posts-dirs)
       (map (fn [p]
              (let [f (fs/file-name p)
                    [meta body] (front-matter (slurp (str p)))]
                {:slug (str/replace (subs f 11) #"\.md$" "")
                 :title (:title meta)
                 :date (or (:date meta) (subs f 0 10))
                 :lede (:lede meta)
                 :html (md->html body)})))
       (sort-by :date) reverse))

(fs/create-dirs (str out "/posts"))
(fs/create-dirs (str out "/assets/css"))
(fs/copy (str repo "/assets/css/main.css") (str out "/assets/css/main.css")
         {:replace-existing true})
(fs/create-dirs (str out "/assets/img"))
(doseq [n ["banner-1440.webp" "banner-2048.webp" "banner-1440.jpg"]]
  (fs/copy (str repo "/assets/img/" n) (str out "/assets/img/" n) {:replace-existing true}))

(spit (str out "/index.html")
      (layout {:title site-title :here :home :depth 0
               :body (str "  <ul class=\"post-list\">\n" (str/join (map item posts)) "  </ul>")}))

(spit (str out "/archive.html")
      (layout {:title (str "Archive &middot; " site-title) :here :archive :depth 0
               :body (str/join
                      (for [[y g] (->> posts (group-by #(subs (:date %) 0 4)) (sort-by key) reverse)]
                        (str "  <section class=\"year\">\n    <h2>" y "</h2>\n    <ul class=\"post-list\">\n"
                             (str/join (map item g)) "    </ul>\n  </section>\n")))}))

(doseq [{:keys [slug title date html lede]} posts]
  (spit (str out "/posts/" slug ".html")
        ;; depth 1 so ../ resolves back to the preview root
        (layout {:title (str title " &middot; " site-title) :depth 1
                 :body (str "<article>\n  <header class=\"post-header\">\n"
                            "    <h1 class=\"post-title\">" title "</h1>\n"
                            "    <div class=\"post-date\"><time>" (pretty-date date) "</time></div>\n"
                            "  </header>\n"
                            (when lede (str "  <p class=\"post-lede\">" lede "</p>\n"))
                            "  <div class=\"post-body\">\n" html "  </div>\n</article>\n"
                            toc-script)})))

(println (format "built %d post(s) -> %s/index.html" (count posts) out))
