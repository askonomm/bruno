(ns bruno.core-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [bruno.core :as core]
    [clojure.string :as string]))

(alter-var-root #'core/*directory* (constantly "./resources/test"))

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
             :contents "(declare ^:dynamic post)\n(declare ^:dynamic partial)\n\n[:div\n (partial \"header\" {'test \"this\"})\n [:div.post\n  [:h2 (:title post)]\n  [:div.entry (:entry post)]]]"}]
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
