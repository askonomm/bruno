(defproject bruno "0.1.0-SNAPSHOT"
  :description ""
  :url "https://github.com/askonomm/bruno"
  :license {:name "MIT"
            :url  "https://raw.githubusercontent.com/askonomm/bruno/master/LICENSE.txt"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.github.askonomm/clarktown "2.0.0"]
                 [com.github.clj-easy/graal-build-time "0.1.4"]
                 [nubank/matcher-combinators "3.8.3"]
                 [org.slf4j/slf4j-simple "2.0.6"]
                 [org.babashka/sci "0.7.38"]
                 [hiccup "1.0.5"]]
  :plugins [[lein-auto "0.1.3"]
            [lein-ancient "0.7.0"]
            [lein-shell "0.5.0"]]
  :aliases {"native"
            ["shell"
             "native-image" "--report-unsupported-elements-at-runtime" "--no-fallback"
             "-H:ReflectionConfigurationFiles=reflection.json" "-jar" "./target/uberjar/bruno.jar"]}
  :main bruno.core
  :min-lein-version "2.0.0"
  :target-path "target/%s"
  :uberjar-name "bruno.jar"
  :repl-options {:init-ns bruno.core}
  :aot [bruno.core])
