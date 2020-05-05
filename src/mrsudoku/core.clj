(ns mrsudoku.core
  (:gen-class)
  (:require [seesaw.core :refer [invoke-later pack! show!]]
            [mrsudoku.grid :as g]
            [mrsudoku.view :as v]
            [mrsudoku.control :as control]))

(defn show-sudoku! [grid]
  (let [ctrl (control/mk-grid-controller grid)
        main-frame (v/mk-main-frame grid ctrl)]
    (invoke-later
     (-> main-frame
         pack!
         show!))
    ctrl))

(defn -main
  "Let the sudoku begin!"
  [& args]
  (println "Welcome to MrSudoku...")
  (let [grid (var-get #'g/sudoku-grid)]
    (show-sudoku! grid)))

