(ns bruno.core
  (:require
   [clojure.string :as string]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [hiccup.core :as h]
   [hiccup.page :as hpage]
   [sci.core :as sci]
   [markdown.core :as md])
  (:import
   (java.io File)
   (java.time LocalDate)
   (java.time.format DateTimeFormatter)
   (clojure.lang PersistentList)
   (java.util TimeZone))
  (:gen-class))

(def ^:dynamic *src-directory* nil)
(def ^:dynamic *target-directory* nil)
(declare sci-opts)

(defn triml
  "Trims the given `trim-char` from the left of `input`."
  [input trim-char]
  (if (string/starts-with? input trim-char)
    (recur (subs input 1)
           trim-char)
    input))

(defn trimr
  "Trims away the given `trim-char` from the right of `input`."
  [input trim-char]
  (if (string/ends-with? input trim-char)
    (recur (subs input 0 (- (count input) 1))
           trim-char)
    input))

(defn scan
  "Scans a given `directory` for any and all files (recursively)
  and returns a list of maps containing the path of a file and
  the modified time of said file."
  [directory]
  (->> (.list (io/file directory))
       (filter #(not (.isHidden (io/file %))))
       (map #(str directory File/separatorChar (.getPath (io/file %))))
       (map #(if (.isDirectory (io/file %))
               (scan %)
               {:path (.getCanonicalPath (io/file %))
                :file-name (.getName (io/file %))
                :file-ext (->> (-> (.getName (io/file %))
                                   (string/split #"\.")
                                   next)
                               (string/join "."))
                :mtime (.lastModified (io/file %))}))
       flatten
       vec))

(defn- parse-md-metadata-line
  "Parses each YAML metadata line into a map of k:v."
  [line]
  (let [meta-key (-> (string/split line #":")
                     first
                     string/trim)
        meta-value (->> (string/split line #":")
                        next
                        (string/join ":")
                        string/trim)]
    {(keyword meta-key)
     meta-value}))

(defn parse-md-metadata
  "Takes in a given `content` as the entirety of a Markdown
  content file, and then parses YAML metadata from it."
  [contents]
  (if-let [match (re-find #"(?s)^(---)(.*?)(---|\.\.\.)" contents)]
    (let [lines (remove #(= "---" %) (string/split-lines (first match)))]
      (into {} (map #(parse-md-metadata-line %) lines)))
    {}))

(defn parse-md-entry
  "Takes in a given `content` as the entirety of a Markdown
  content file, and then parses the Markdown into HTML from it."
  [contents]
  (-> contents
      (string/replace #"(?s)^---(.*?)---*" "")
      (string/trim)
      (md/md-to-html-string)))

(defn slug-from-path
  "Takes in a full `path` to a file and returns the relative URL slug
  from it."
  [path]
  (let [full-root-dir (.getCanonicalPath (io/file *src-directory*))]
    (-> (string/replace path full-root-dir "")
        (triml "/")
        (string/split #"\.")
        first)))

(defn get-content-items
  "Gets a collection of content items."
  ([]
   (get-content-items *src-directory*))
  ([directory]
   (pmap (fn [item]
           (let [file-contents (slurp (:path item))]
             (merge (parse-md-metadata file-contents)
                    {:slug (slug-from-path (:path item))
                     :entry (parse-md-entry file-contents)})))
         (->> (scan directory)
              (filter #(string/ends-with? (:path %) ".md"))))))

(defn get-pages
  "Gets a collection of pages."
  []
  (pmap (fn [item]
          (let [ext (-> (:file-ext item)
                        (string/replace ".clj" ""))]
            {:slug (str (slug-from-path (:path item)) "." ext)
             :contents (slurp (:path item))}))
        (->> (scan *src-directory*)
             (filter #(or (= (:file-ext %) "html.clj")
                          (= (:file-ext %) "xml.clj"))))))

(defn get-layouts
  "Gets a collection of layouts."
  []
  (pmap (fn [item]
          {:name (-> (string/split (:file-name item) #"\.")
                     first
                     (triml "/"))
           :contents (slurp (:path item))})
        (->> (scan (str *src-directory* File/separatorChar "_layouts"))
             (filter #(string/ends-with? (:path %) ".clj")))))

(defn- get-current-dir
  "Returns the current working directory."
  []
  (-> (:out (sh/sh "pwd"))
      (string/replace "\n" "")
      string/trim))

(defn- get-src-dir
  "Gets the source directory."
  []
  (let [current-dir (get-current-dir)]
    (if (.isDirectory (io/file (str current-dir File/separatorChar "src")))
      (str current-dir File/separatorChar "src")
      current-dir)))

(defn- get-target-dir
  "Gets teh target directory."
  []
  (let [current-dir (get-current-dir)]
    (str current-dir File/separatorChar "public")))

(defn load-partial
  "Renders a template partial."
  ([name]
   (load-partial name {}))
  ([name local-bindings]
   (let [partial (slurp (str *src-directory*
                             File/separatorChar
                             "_partials"
                             File/separatorChar
                             name ".clj"))]
     (h/html (sci/eval-string partial (merge-with into sci-opts
                                                  {:bindings local-bindings}))))))

(defn document
  "Wraps `contents` within a valid HTML document."
  [opts & contents]
  (if-not (map? opts)
    (hpage/html5 {} opts contents)
    (hpage/html5 opts contents)))

(defn content-composer
  "Composes data sets from available Markdown files."
  [opts]
  (cond->> (if (:from opts)
             (get-content-items (str *src-directory* File/separatorChar (:from opts)))
             (get-content-items))
    ; sort by
    (:sort-by opts) (sort-by #(get % (:sort-by opts)))
    ; order
    (= :desc (:order opts)) reverse
    ; group by
    (:group-by opts) (group-by (:group-by opts))))

(defn format-date
  "Format given `date` string according to `format`.

  Optionally takes in `timezone` as the third argument,
  which must adhere to the TimeZone ID. This defaults to UTC.

  Optionally takes in `parse-format` as the fourth argument,
  which must correspond to the format that the input `date` is in,
  in order for `SimpleDateFormat` to make sense of the string and
  successfully turn it into a date object. This defaults to `YYYY-mm-dd`."
  ([date format]
   (format-date date format "UTC"))
  ([date format timezone]
   (try
     (TimeZone/setDefault (TimeZone/getTimeZone ^String timezone))
     (let [parsed-dt (LocalDate/parse date)
           df (DateTimeFormatter/ofPattern format)]
       (.format df parsed-dt))
     (catch Exception e
       (println (.getMessage e))
       ""))))

(def sci-opts
  {:bindings {'document document
              'include-js hpage/include-js
              'include-css hpage/include-css
              'load-partial load-partial
              'content content-composer
              'format-date format-date}
   :namespaces {'clojure.string {'split string/split}}})

(defn build-content-items!
  "Builds all the content items with the layout specified
  in the individual content items, or with the \"default\"
  layout if none was found in the item."
  []
  (let [layouts (get-layouts)]
    (doseq [item (get-content-items)]
      (let [write-path (str *target-directory*
                            File/separatorChar
                            (:slug item)
                            File/separatorChar
                            "index.html")
            layout (->> layouts
                        (filter #(= (:name %) (or (:layout item) "default")))
                        first)
            bindings {'post item
                      'is-post true}
            html (h/html (sci/eval-string
                          (:contents layout)
                          (merge-with into sci-opts
                                      {:bindings bindings})))]
        (println "Writing " (:slug item))
        (io/make-parents write-path)
        (spit write-path html)))))

(defn build-pages!
  "Builds all the pages."
  []
  (doseq [page (get-pages)]
    (let [write-path (str *target-directory*
                          File/separatorChar
                          (:slug page))
          bindings {'is-page true
                    (symbol "is-" (:slug page)) true
                    'page page}
          html (h/html (sci/eval-string
                        (:contents page)
                        (merge-with into sci-opts
                                    {:bindings bindings})))]
      (println "Writing " (:slug page))
      (io/make-parents write-path)
      (spit write-path html))))

(defn empty-public-dir!
  "Deletes all files and folders from the `*target-directory*`."
  []
  (doseq [{:keys [path]} (scan *target-directory*)]
    (io/delete-file path)))

(defn copy-assets!
  "Copies all assets to the `*target-directory*`."
  []
  (doseq [{:keys [path file-name]}
          (->> (scan *src-directory*)
               (filter #(not (or (= "clj" (:file-ext %))
                                 (= "html.clj" (:file-ext %))
                                 (= "xml.clj" (:file-ext %))
                                 (= "md" (:file-ext %))))))]
    (let [from (io/file path)
          to-path (string/replace path *src-directory* *target-directory*)
          to (io/file to-path)]
      (println "Copying " file-name)
      (io/make-parents to-path)
      (io/copy from to))))

(defn build!
  "Builds the static site in `*src-directory*`."
  []
  (empty-public-dir!)
  (copy-assets!)
  (build-content-items!)
  (build-pages!))

(defn watch!
  "Runs an infinite loop that checks every 1s for any changes
  to files, upon which it will call `(build!)`."
  []
  (println "Watching ...")
  (loop [watch-list (scan *src-directory*)
         new-watch-list watch-list]
    (Thread/sleep 1000)
    (when-not (= watch-list new-watch-list)
      (build!))
    (recur new-watch-list
           (scan *src-directory*))))

(defn argument
  "Parses a given list of `args` for a `command` and returns
  `true` if the command was found. If the command has a
  subcommand provided, then it will return that instead."
  [command ^PersistentList args]
  (when (seq? args)
    (let [index (.indexOf args command)]
      (if-not (= -1 index)
        (if-let [subcommand (nth args (+ index 1) nil)]
          subcommand
          true)
        nil))))

(defn -main [& args]
  (println "Thinking ...")
  (alter-var-root #'*src-directory* (constantly (get-src-dir)))
  (alter-var-root #'*target-directory* (constantly (get-target-dir)))
  (if (argument "watch" args)
    (do (build!)
        (watch!))
    (do (build!)
        (System/exit 0))))
