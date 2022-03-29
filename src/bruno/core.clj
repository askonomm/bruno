(ns bruno.core
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [clojure.java.shell :as sh]
    [hiccup.core :as h]
    [hiccup.page :as hpage]
    [sci.core :as sci]
    [markdown.core :as md])
  (:import (java.io File))
  (:gen-class))


(def ^:dynamic *directory* nil)
(declare bindings)


(defn triml
  "Trims the given `trim-char` from the left of `string`."
  [string trim-char]
  (try
    (loop [string string]
      (if (string/starts-with? string trim-char)
        (recur (subs string 1))
        string))
    (catch Exception _
      string)))


(defn trimr
  "Trims away the given `trim-char` from the right of `string`."
  [string trim-char]
  (try
    (loop [string string]
      (if (string/ends-with? string trim-char)
        (recur (subs string 0 (- (count string) 1)))
        string))
    (catch Exception _
      string)))


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
               {:path      (.getCanonicalPath (io/file %))
                :file-name (.getName (io/file %))
                :file-ext  (->> (-> (.getName (io/file %))
                                    (string/split #"\.")
                                    next)
                                (string/join "."))
                :mtime     (.lastModified (io/file %))}))
       flatten
       vec))


(defn- parse-md-metadata-value-by-key
  "Parses YAML metadata values depending on key."
  [value key]
  (cond (= "date" key)
        value
        :else value))


(defn- parse-md-metadata-line
  "Parses each YAML metadata line into a map."
  [line]
  (let [meta-key   (-> (string/split line #":")
                       first
                       string/trim)
        meta-value (->> (string/split line #":")
                        next
                        (string/join ":")
                        string/trim)]
    {(keyword meta-key)
     (parse-md-metadata-value-by-key meta-value meta-key)}))


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
  ""
  [path]
  (let [full-root-dir (.getCanonicalPath (io/file *directory*))]
    (-> (string/replace path full-root-dir "")
        (triml "/")
        (string/split #"\.")
        first)))


(defn get-content-items
  ""
  []
  (pmap (fn [item]
          (let [file-contents (slurp (:path item))]
            (merge (parse-md-metadata file-contents)
                   {:slug  (slug-from-path (:path item))
                    :entry (parse-md-entry file-contents)})))
        (->> (scan *directory*)
             (filter #(string/ends-with? (:path %) ".md")))))


(defn get-pages
  ""
  []
  (pmap (fn [item]
          (let [ext (-> (:file-ext item)
                        (string/replace ".clj" ""))]
            {:slug     (str (slug-from-path (:path item)) "." ext)
             :contents (slurp (:path item))}))
        (->> (scan *directory*)
             (filter #(or (= (:file-ext %) "html.clj")
                          (= (:file-ext %) "xml.clj"))))))


(defn get-layouts
  ""
  []
  (pmap (fn [item]
          {:name     (-> (string/split (:file-name item) #"\.")
                         first
                         (triml "/"))
           :contents (slurp (:path item))})
        (->> (scan (str *directory* File/separatorChar "_layouts"))
             (filter #(string/ends-with? (:path %) ".clj")))))


(defn- get-current-dir
  []
  (-> (:out (sh/sh "pwd"))
      (string/replace "\n" "")
      string/trim))


(defn load-partial
  ([name]
   (load-partial name {}))
  ([name local-bindings]
   (let [partial (slurp (str *directory*
                             File/separatorChar
                             "_partials"
                             File/separatorChar
                             name ".clj"))]
     (h/html (sci/eval-string partial {:bindings (merge bindings
                                                        local-bindings)})))))


(defn document
  [opts & contents]
  (if-not (map? opts)
    (hpage/html5 {} opts contents)
    (hpage/html5 opts contents)))


(def bindings
  {'document     document
   'include-js   hpage/include-js
   'include-css  hpage/include-css
   'load-partial load-partial})


(defn build-content-items!
  []
  (let [layouts (get-layouts)]
    (doseq [item (get-content-items)]
      (let [write-path (str *directory*
                            File/separatorChar
                            "public"
                            File/separatorChar
                            (:slug item)
                            File/separatorChar
                            "index.html")
            layout     (->> layouts
                            (filter #(= (:name %) (or (:layout item) "default")))
                            first)]
        (println "Writing " (:slug item))
        (io/make-parents write-path)
        (spit write-path (h/html (sci/eval-string
                                   (:contents layout)
                                   {:bindings (merge bindings
                                                     {'post    item
                                                      'is-post true})})))))))


(defn build-pages!
  []
  (doseq [page (get-pages)]
    (let [write-path (str *directory*
                          File/separatorChar
                          "public"
                          File/separatorChar
                          (:slug page))]
      (println "Writing " (:slug page))
      (io/make-parents write-path)
      (spit write-path (h/html (sci/eval-string
                                 (:contents page)
                                 {:bindings (merge bindings
                                                   {'is-page true
                                                    'page    page})}))))))


(defn -main [& args]
  (println "Thinking ...")
  (alter-var-root #'*directory* (constantly (get-current-dir)))
  (build-content-items!)
  (build-pages!)
  (System/exit 0))
