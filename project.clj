(defproject mrsudoku "0.2.0-SNAPSHOT"
  :description "Mini-projet Sudoku"
  :url ".."
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [midje "1.9.6"]
                 [seesaw "1.5.0"]]
  :plugins [[lein-midje "3.2.1"]]
  :main ^:skip-aot mrsudoku.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

