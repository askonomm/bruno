(ns bruno.core-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [clojure.java.io :as io]
    [bruno.core :as core]
    [clojure.string :as string])
  (:import (java.io File)))

(alter-var-root #'core/*src-directory* (constantly (.getCanonicalPath (io/file "./resources/test"))))

(deftest triml-test
  (testing "Trimming one character from the left of a string"
    (is (= "ola" (core/triml "hola" "h"))))

  (testing "Trimming multiple characters from the left of a string"
    (is (= "ola" (core/triml "hhhhola" "h"))))

  (testing "Trimming a character from the left of a string that isn't there"
    (is (= "hola" (core/triml "hola" "b"))))

  (testing "Trimming a character from the left of a string that is of wrong type"
    (is (= "hola" (core/triml "hola" nil)))))


(deftest trimr-test
  (testing "Trimming one character from the right of a string"
    (is (= "hol" (core/trimr "hola" "a"))))

  (testing "Trimming multiple characters from the right of a string"
    (is (= "hol" (core/trimr "holaaaa" "a"))))

  (testing "Trimming a character from the right of a string that isn't there"
    (is (= "hola" (core/trimr "hola" "b"))))

  (testing "Trimming a character from the right of a string that is of wrong type"
    (is (= "hola" (core/trimr "hola" nil)))))


(deftest scan-test
  (testing "Getting sample file information"
    (let [data (->> (core/scan "./resources/test")
                    (filterv #(string/ends-with? (:path %) ".md")))]
      (is (= 2 (count data)))
      (is (map? (first data)))
      (is (string? (:path (first data))))
      (is (int? (:mtime (first data))))
      (is (map? (last data)))
      (is (string? (:path (last data))))
      (is (int? (:mtime (last data)))))))


(deftest get-content-items-test
  (testing "Getting content items"
    (is (= [{:title "This is a test file"
             :slug  "test-file"
             :entry "<p>And this is a test content.</p>"}
            {:title "This is a test file 2"
             :slug  "test-dir/test-file-2"
             :entry "<p>Just another test file. Again.</p>"}]
           (core/get-content-items)))))


(deftest get-pages-test
  (testing "Getting pages"
    (is (= [{:slug     "test-page.html"
             :contents "[:div \"This is a test page.\"]"}]
           (core/get-pages)))))


(deftest get-layouts-test
  (testing "Getting layouts"
    (is (= [{:name     "default"
             :contents "(declare post)\n(declare load-partial)\n\n[:div\n (load-partial \"header\" {'test \"this\"})\n [:div.post\n  [:h2 (:title post)]\n  [:div.entry (:entry post)]]]"}]
           (core/get-layouts)))))


(deftest parse-md-metadata-test
  (testing "One meta-data item"
    (is (= {:this "that"} (core/parse-md-metadata "---\nthis: that\n---"))))
  (testing "Multiple meta-data items"
    (is (= {:this    "that"
            :hip     "hop"
            :complex "More : characters \23 and things"}
           (core/parse-md-metadata "---\nthis: that\nhip: hop\ncomplex: More : characters \23 and things\n---"))))
  (testing "No meta-data"
    (is (= {} (core/parse-md-metadata "Hello.")))
    (is (= {} (core/parse-md-metadata "---\n---\nHello")))
    (is (= {} (core/parse-md-metadata "---\nHello")))))


(deftest parse-md-entry-test
  (testing "Rudimentary markdown compilation."
    (is (= "<p>hello</p>" (core/parse-md-entry "hello")))))


(deftest slug-from-path-test
  (testing "Getting a slug from path"
    (is (= "hello-world" (core/slug-from-path (str core/*src-directory* File/separatorChar "hello-world.md"))))
    (is (= "blog/hello-world" (core/slug-from-path (str core/*src-directory* File/separatorChar "blog" File/separatorChar "hello-world.md"))))))


(deftest load-partial-test
  (testing "Loading a partial"
    (is (= "<div class=\"header\">hello</div>" (core/load-partial "header" {'test "hello"})))))


(deftest format-date-test
  (testing "Formatting year-month-day"
    (is (= "28 Dec, 2021" (core/format-date "2021-12-28" "dd MMM, YYYY")))))